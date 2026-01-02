const express = require("express");
const http = require("http");
const cors = require("cors");
const app = express();

// Enable CORS for all routes
app.use(cors({
	origin: "*",
	credentials: true
}));

const server = http.createServer(app);
const io = require("socket.io")(server, {
	cors: {
		origin: "*",
		methods: ["GET", "POST"],
		credentials: true
	},
	transports: ['websocket', 'polling']
});

const PORT = process.env.PORT || 5000;

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

io.on("connection", (socket) => {
    console.log("New client connected:", socket.id);
    
    // 1. User joins the room
	socket.on("joinRoom", (roomID) => {
        console.log(`User ${socket.id} joining room: ${roomID}`);
        
        // Simple security: Check if room is full (Limit to 2 people for privacy)
        const room = io.sockets.adapter.rooms.get(roomID);
        if (room && room.size >= 2) {
            console.log(`Room ${roomID} is full`);
            socket.emit("roomFull");
            return;
        }
        
		socket.join(roomID);
        socket.emit("me", socket.id); // Tell user their own ID
        
        console.log(`User ${socket.id} joined room ${roomID}`);
        
        // If someone else is already there, tell them a new user arrived
        socket.to(roomID).emit("userJoined", socket.id);
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
        console.log("Client disconnected:", socket.id);
    });
});

server.listen(PORT, () => {
    console.log(`═══════════════════════════════════════════════`);
    console.log(`  P2P Video Chat Server`);
    console.log(`  Running on port: ${PORT}`);
    console.log(`  Time: ${new Date().toLocaleString()}`);
    console.log(`═══════════════════════════════════════════════`);
});
