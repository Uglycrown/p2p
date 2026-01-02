const express = require("express");
const http = require("http");
const app = express();
const server = http.createServer(app);
const io = require("socket.io")(server, {
	cors: {
		origin: "*", // Allow connections from your Vue app
		methods: [ "GET", "POST" ]
	}
});

const PORT = process.env.PORT || 5000;

io.on("connection", (socket) => {
    // 1. User joins the room
	socket.on("joinRoom", (roomID) => {
        // Simple security: Check if room is full (Limit to 2 people for privacy)
        const room = io.sockets.adapter.rooms.get(roomID);
        if (room && room.size >= 2) {
            socket.emit("roomFull");
            return;
        }
        
		socket.join(roomID);
        socket.emit("me", socket.id); // Tell user their own ID
        
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
        socket.to(roomID).emit("callEnded");
    });
});

server.listen(PORT, () => console.log(`Server is running on port ${PORT}`));
