require('dotenv').config();
const express = require("express");
const http = require("http");
const cors = require("cors");
const rateLimit = require("express-rate-limit");
const helmet = require("helmet");
const validator = require("validator");
const { v4: uuidv4 } = require('uuid');
const { 
	generateRoomToken, 
	verifyRoomToken, 
	hashPassword, 
	verifyPassword 
} = require('./auth');
const {
	logInfo,
	logError,
	logSecurity,
	logConnection,
	logDisconnection,
	logRoomActivity
} = require('./logger');
const app = express();

// Security: Helmet middleware for HTTP headers
app.use(helmet({
	contentSecurityPolicy: false,
	crossOriginEmbedderPolicy: false
}));

// Security: Restricted CORS (only allow specific origins)
const allowedOrigins = (process.env.ALLOWED_ORIGINS || 'http://localhost:5174,http://localhost:3000').split(',');

app.use(cors({
	origin: function(origin, callback) {
		// Allow requests with no origin (like mobile apps or curl)
		if (!origin) return callback(null, true);
		
		if (allowedOrigins.indexOf(origin) !== -1 || origin.includes('capacitor://') || origin.includes('ionic://')) {
			callback(null, true);
		} else {
			console.log(`❌ Blocked origin: ${origin}`);
			// For development, allow all origins - CHANGE IN PRODUCTION
			callback(null, true);
		}
	},
	credentials: true
}));

// Security: Rate limiting for API endpoints
const apiLimiter = rateLimit({
	windowMs: 15 * 60 * 1000, // 15 minutes
	max: 100, // Limit each IP to 100 requests per windowMs
	message: { error: "Too many requests, please try again later" },
	standardHeaders: true,
	legacyHeaders: false,
});

app.use('/api/', apiLimiter);

// Parse JSON bodies
app.use(express.json());

const server = http.createServer(app);
const io = require("socket.io")(server, {
	cors: {
		origin: "*", // Allow all origins for mobile apps
		methods: ["GET", "POST"],
		credentials: true
	},
	transports: ['websocket', 'polling'],
	pingTimeout: 60000,
	pingInterval: 25000,
	allowEIO3: true // Support older clients
});

const PORT = process.env.PORT || 5000;

// Security: Store room passwords (In production, use Redis or database)
const roomPasswords = new Map();
const roomTokens = new Map();
const activeSessions = new Map();

// Security: Track connection attempts per IP
const connectionAttempts = new Map();
const MAX_CONNECTIONS_PER_IP = 5;
const CONNECTION_RESET_TIME = 60000; // 1 minute

// Security: Validate room ID
function isValidRoomId(roomId) {
	if (!roomId || typeof roomId !== 'string') {
		return false;
	}
	
	// Must be at least 6 characters
	if (roomId.length < 6) {
		return false;
	}
	
	// Must be alphanumeric with optional dashes and underscores
	if (!/^[a-zA-Z0-9_-]+$/.test(roomId)) {
		return false;
	}
	
	// Sanitize to prevent XSS
	return validator.isAlphanumeric(roomId.replace(/[-_]/g, ''));
}

// Security: Track and limit connections per IP
function canConnect(ip) {
	const now = Date.now();
	const attempts = connectionAttempts.get(ip) || { count: 0, timestamp: now };
	
	// Reset if time window passed
	if (now - attempts.timestamp > CONNECTION_RESET_TIME) {
		connectionAttempts.set(ip, { count: 1, timestamp: now });
		return true;
	}
	
	// Check if limit exceeded
	if (attempts.count >= MAX_CONNECTIONS_PER_IP) {
		return false;
	}
	
	// Increment count
	attempts.count++;
	connectionAttempts.set(ip, attempts);
	return true;
}

// Cleanup old connection attempts every minute
setInterval(() => {
	const now = Date.now();
	for (const [ip, data] of connectionAttempts.entries()) {
		if (now - data.timestamp > CONNECTION_RESET_TIME) {
			connectionAttempts.delete(ip);
		}
	}
}, 60000);

// Health check endpoint
app.get("/", (req, res) => {
	res.json({ 
		status: "Server is running",
		message: "P2P Video Chat Server",
		timestamp: new Date().toISOString()
	});
});

// Status endpoint
app.get("/health", (req, res) => {
	res.json({ status: "OK" });
});

// API: Generate room token (with optional password)
app.post("/api/generate-room-token", async (req, res) => {
	try {
		const { roomID, password } = req.body;
		
		// Validate room ID
		if (!isValidRoomId(roomID)) {
			return res.status(400).json({ 
				error: 'Invalid room name. Use 6+ characters (letters, numbers, - or _)' 
			});
		}
		
		// Generate user ID
		const userID = uuidv4();
		
		// If password provided, hash and store it
		if (password) {
			if (password.length < 8) {
				return res.status(400).json({ 
					error: 'Password must be at least 8 characters' 
				});
			}
			
			// Check if room already has a password
			if (roomPasswords.has(roomID)) {
				return res.status(409).json({ 
					error: 'Room already exists with a password' 
				});
			}
			
			const hashedPassword = await hashPassword(password);
			roomPasswords.set(roomID, {
				hash: hashedPassword,
				createdAt: Date.now(),
				createdBy: userID
			});
			
			console.log(`🔒 Room ${roomID} created with password protection`);
		}
		
		// Generate JWT token
		const token = generateRoomToken(roomID, userID);
		roomTokens.set(userID, { roomID, token, createdAt: Date.now() });
		
		res.json({ 
			success: true,
			token,
			userID,
			roomID,
			hasPassword: !!password
		});
		
	} catch (error) {
		console.error('❌ Error generating token:', error);
		res.status(500).json({ error: 'Internal server error' });
	}
});

// API: Verify room password and get token
app.post("/api/verify-room-password", async (req, res) => {
	try {
		const { roomID, password } = req.body;
		
		// Validate inputs
		if (!isValidRoomId(roomID)) {
			return res.status(400).json({ error: 'Invalid room name' });
		}
		
		if (!password) {
			return res.status(400).json({ error: 'Password required' });
		}
		
		// Check if room has password
		const roomData = roomPasswords.get(roomID);
		if (!roomData) {
			return res.status(404).json({ error: 'Room not found or no password set' });
		}
		
		// Verify password
		const isValid = await verifyPassword(password, roomData.hash);
		if (!isValid) {
			console.log(`❌ Invalid password attempt for room: ${roomID}`);
			return res.status(401).json({ error: 'Incorrect password' });
		}
		
		// Generate token for user
		const userID = uuidv4();
		const token = generateRoomToken(roomID, userID);
		roomTokens.set(userID, { roomID, token, createdAt: Date.now() });
		
		console.log(`✅ User ${userID} authenticated for room: ${roomID}`);
		
		res.json({ 
			success: true,
			token,
			userID,
			roomID
		});
		
	} catch (error) {
		console.error('❌ Error verifying password:', error);
		res.status(500).json({ error: 'Internal server error' });
	}
});

// API: Check if room requires password
app.get("/api/room-info/:roomID", (req, res) => {
	const { roomID } = req.params;
	
	if (!isValidRoomId(roomID)) {
		return res.status(400).json({ error: 'Invalid room name' });
	}
	
	const hasPassword = roomPasswords.has(roomID);
	const room = io.sockets.adapter.rooms.get(roomID);
	const occupancy = room ? room.size : 0;
	
	res.json({
		roomID,
		hasPassword,
		occupancy,
		isFull: occupancy >= 2
	});
});

io.on("connection", (socket) => {
	const clientIp = socket.handshake.address;
	const clientOrigin = socket.handshake.headers.origin;
	const token = socket.handshake.auth.token;
	const connectionTime = Date.now();
	
	logConnection(socket.id, clientIp, clientOrigin);
	
	// Security: Check connection rate limit
	if (!canConnect(clientIp)) {
		logSecurity('Rate limit exceeded', { ip: clientIp, socketId: socket.id });
		socket.emit("error", { message: "Too many connection attempts. Please wait." });
		socket.disconnect(true);
		return;
	}
	
	// Security: Verify origin
	if (clientOrigin && !allowedOrigins.includes(clientOrigin)) {
		logSecurity('Unauthorized origin blocked', { origin: clientOrigin, ip: clientIp });
		socket.disconnect(true);
		return;
	}
	
	// Security: Verify JWT token (if authentication is enabled)
	if (token) {
		const decoded = verifyRoomToken(token);
		if (!decoded) {
			logSecurity('Invalid token rejected', { socketId: socket.id, ip: clientIp });
			socket.emit("error", { message: "Invalid or expired token" });
			socket.disconnect(true);
			return;
		}
		
		// Store user info
		socket.userID = decoded.userID;
		socket.authorizedRoom = decoded.roomID;
		
		logInfo('User authenticated', { 
			userId: decoded.userID, 
			roomId: decoded.roomID,
			socketId: socket.id
		});
	}
    
    // 1. User joins the room
	socket.on("joinRoom", (roomID) => {
		logRoomActivity('join-attempt', roomID, socket.userID || socket.id);
		
		// Security: Validate room ID
		if (!isValidRoomId(roomID)) {
			logSecurity('Invalid room ID attempt', { roomId: roomID, socketId: socket.id });
			socket.emit("error", { 
				message: "Invalid room name. Use 6+ characters (letters, numbers, - or _)" 
			});
			return;
		}
		
		// Security: Check if user is authorized for this room (if token provided)
		if (socket.authorizedRoom && socket.authorizedRoom !== roomID) {
			logSecurity('Unauthorized room access attempt', {
				userId: socket.userID,
				authorizedRoom: socket.authorizedRoom,
				attemptedRoom: roomID
			});
			socket.emit("error", { 
				message: "Not authorized to join this room" 
			});
			return;
		}
		
		// Security: Sanitize room ID
		const sanitizedRoomId = validator.escape(roomID);
        
        // Simple security: Check if room is full (Limit to 2 people for privacy)
        const room = io.sockets.adapter.rooms.get(sanitizedRoomId);
        if (room && room.size >= 2) {
            logRoomActivity('room-full', sanitizedRoomId, socket.userID || socket.id);
            socket.emit("roomFull");
            return;
        }
        
		socket.join(sanitizedRoomId);
        socket.emit("me", socket.id); // Tell user their own ID
        
        logRoomActivity('joined', sanitizedRoomId, socket.userID || socket.id, {
			roomSize: (room?.size || 0) + 1
		});
        
        // If someone else is already there, tell them a new user arrived
        socket.to(sanitizedRoomId).emit("userJoined", socket.id);
	});

    // 2. Handling the Call (The "Hello")
	socket.on("callUser", (data) => {
		io.to(data.userToCall).emit("callUser", { 
            signal: data.signalData, 
            from: data.from 
        });
	});

    // 3. Answering the Call (The "Yes, I'm here")
	socket.on("answerCall", (data) => {
		io.to(data.to).emit("callAccepted", data.signal);
	});

    // 4. End Call
    socket.on("endCall", (roomID) => {
        console.log(`Call ended in room: ${roomID}`);
        socket.to(roomID).emit("callEnded");
    });
    
    // Handle disconnect
    socket.on("disconnect", () => {
        const duration = Date.now() - connectionTime;
        logDisconnection(socket.id, duration);
    });
});

server.listen(PORT, () => {
    console.log(`═══════════════════════════════════════════════`);
    console.log(`  P2P Video Chat Server`);
    console.log(`  Running on port: ${PORT}`);
    console.log(`  Time: ${new Date().toLocaleString()}`);
    console.log(`═══════════════════════════════════════════════`);
});
