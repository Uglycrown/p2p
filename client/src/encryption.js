import CryptoJS from 'crypto-js';

/**
 * End-to-End Encryption Layer
 * Adds an additional encryption layer on top of WebRTC DTLS
 */
export class E2EEncryption {
    constructor(roomID, password = null) {
        // Derive encryption key from room ID and optional password
        const baseKey = roomID + (password || '');
        const salt = 'p2p-video-chat-secure-salt-2024';
        
        // Use PBKDF2 to derive a strong key
        this.key = CryptoJS.PBKDF2(baseKey, salt, {
            keySize: 256/32,
            iterations: 10000
        }).toString();
    }
    
    /**
     * Encrypt WebRTC signaling data
     * @param {object} signal - The signaling data to encrypt
     * @returns {string} Encrypted data
     */
    encryptSignal(signal) {
        try {
            const jsonStr = JSON.stringify(signal);
            const encrypted = CryptoJS.AES.encrypt(jsonStr, this.key).toString();
            return encrypted;
        } catch (error) {
            throw error;
        }
    }
    
    /**
     * Decrypt WebRTC signaling data
     * @param {string} encrypted - The encrypted data
     * @returns {object} Decrypted signaling data
     */
    decryptSignal(encrypted) {
        try {
            const decrypted = CryptoJS.AES.decrypt(encrypted, this.key);
            const jsonStr = decrypted.toString(CryptoJS.enc.Utf8);
            
            if (!jsonStr) {
                throw new Error('Decryption failed - wrong key or corrupted data');
            }
            
            return JSON.parse(jsonStr);
        } catch (error) {
            throw error;
        }
    }
    
    /**
     * Encrypt a text message
     * @param {string} message - Plain text message
     * @returns {string} Encrypted message
     */
    encryptMessage(message) {
        return CryptoJS.AES.encrypt(message, this.key).toString();
    }
    
    /**
     * Decrypt a text message
     * @param {string} encrypted - Encrypted message
     * @returns {string} Decrypted message
     */
    decryptMessage(encrypted) {
        const decrypted = CryptoJS.AES.decrypt(encrypted, this.key);
        return decrypted.toString(CryptoJS.enc.Utf8);
    }
}

/**
 * Generate a secure random room password
 * @returns {string} 16-character password
 */
export const generateSecurePassword = () => {
    const array = new Uint8Array(12);
    crypto.getRandomValues(array);
    return Array.from(array, byte => 
        byte.toString(36).padStart(2, '0')
    ).join('').substring(0, 16);
};
