const jwt = require('jsonwebtoken');
const crypto = require('crypto');

// Secret key for JWT (In production, use environment variable!)
const JWT_SECRET = process.env.JWT_SECRET || crypto.randomBytes(64).toString('hex');


/**
 * Generate a room access token
 * @param {string} roomID - The room identifier
 * @param {string} userID - The user identifier
 * @returns {string} JWT token
 */
const generateRoomToken = (roomID, userID) => {
    const payload = {
        roomID: roomID,
        userID: userID,
        type: 'room-access',
        timestamp: Date.now()
    };
    
    return jwt.sign(payload, JWT_SECRET, {
        expiresIn: '2h', // Token valid for 2 hours
        issuer: 'p2p-video-chat',
        audience: 'video-call-users'
    });
};

/**
 * Verify a room access token
 * @param {string} token - JWT token to verify
 * @returns {object|null} Decoded token or null if invalid
 */
const verifyRoomToken = (token) => {
    try {
        const decoded = jwt.verify(token, JWT_SECRET, {
            issuer: 'p2p-video-chat',
            audience: 'video-call-users'
        });
        
        return decoded;
    } catch (err) {
        return null;
    }
};

/**
 * Generate a unique session ID
 * @returns {string} Session ID
 */
const generateSessionId = () => {
    return crypto.randomBytes(16).toString('hex');
};

/**
 * Hash a password using bcrypt
 * @param {string} password - Plain text password
 * @returns {Promise<string>} Hashed password
 */
const hashPassword = async (password) => {
    const bcrypt = require('bcryptjs');
    const saltRounds = 10;
    return await bcrypt.hash(password, saltRounds);
};

/**
 * Verify a password against its hash
 * @param {string} password - Plain text password
 * @param {string} hash - Hashed password
 * @returns {Promise<boolean>} True if password matches
 */
const verifyPassword = async (password, hash) => {
    const bcrypt = require('bcryptjs');
    return await bcrypt.compare(password, hash);
};

module.exports = {
    generateRoomToken,
    verifyRoomToken,
    generateSessionId,
    hashPassword,
    verifyPassword,
    JWT_SECRET
};
