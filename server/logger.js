const winston = require('winston');
require('winston-daily-rotate-file');

// Define log format
const logFormat = winston.format.combine(
    winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
    winston.format.errors({ stack: true }),
    winston.format.splat(),
    winston.format.json()
);

// Console format (for development)
const consoleFormat = winston.format.combine(
    winston.format.colorize(),
    winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
    winston.format.printf(({ timestamp, level, message, ...metadata }) => {
        let msg = `${timestamp} [${level}] : ${message}`;
        if (Object.keys(metadata).length > 0) {
            msg += ` ${JSON.stringify(metadata)}`;
        }
        return msg;
    })
);

// Rotate file transport for general logs
const fileRotateTransport = new winston.transports.DailyRotateFile({
    filename: 'logs/app-%DATE%.log',
    datePattern: 'YYYY-MM-DD',
    maxSize: '20m',
    maxFiles: '14d',
    format: logFormat
});

// Rotate file transport for errors
const errorFileRotateTransport = new winston.transports.DailyRotateFile({
    filename: 'logs/error-%DATE%.log',
    datePattern: 'YYYY-MM-DD',
    level: 'error',
    maxSize: '20m',
    maxFiles: '30d',
    format: logFormat
});

// Security events log
const securityFileRotateTransport = new winston.transports.DailyRotateFile({
    filename: 'logs/security-%DATE%.log',
    datePattern: 'YYYY-MM-DD',
    maxSize: '20m',
    maxFiles: '90d',
    format: logFormat
});

// Create logger
const logger = winston.createLogger({
    level: process.env.LOG_LEVEL || 'info',
    format: logFormat,
    transports: [
        fileRotateTransport,
        errorFileRotateTransport,
        new winston.transports.Console({
            format: consoleFormat
        })
    ],
    exceptionHandlers: [
        new winston.transports.File({ filename: 'logs/exceptions.log' })
    ],
    rejectionHandlers: [
        new winston.transports.File({ filename: 'logs/rejections.log' })
    ]
});

// Security logger
const securityLogger = winston.createLogger({
    level: 'info',
    format: logFormat,
    transports: [
        securityFileRotateTransport,
        new winston.transports.Console({
            format: consoleFormat
        })
    ]
});

// Helper functions
const logInfo = (message, metadata = {}) => {
    logger.info(message, metadata);
};

const logError = (message, error = null, metadata = {}) => {
    if (error) {
        logger.error(message, { error: error.message, stack: error.stack, ...metadata });
    } else {
        logger.error(message, metadata);
    }
};

const logWarning = (message, metadata = {}) => {
    logger.warn(message, metadata);
};

const logDebug = (message, metadata = {}) => {
    logger.debug(message, metadata);
};

const logSecurity = (event, metadata = {}) => {
    securityLogger.info(event, {
        timestamp: new Date().toISOString(),
        type: 'security',
        ...metadata
    });
};

// Connection tracking
const logConnection = (socketId, ip, origin) => {
    logInfo('New connection', {
        socketId,
        ip,
        origin,
        timestamp: new Date().toISOString()
    });
};

const logDisconnection = (socketId, duration) => {
    logInfo('Client disconnected', {
        socketId,
        duration: `${duration}ms`,
        timestamp: new Date().toISOString()
    });
};

// Room activity tracking
const logRoomActivity = (action, roomId, userId, metadata = {}) => {
    logInfo(`Room activity: ${action}`, {
        action,
        roomId,
        userId,
        timestamp: new Date().toISOString(),
        ...metadata
    });
};

// Call quality metrics
const logCallQuality = (roomId, metrics) => {
    logInfo('Call quality metrics', {
        roomId,
        metrics,
        timestamp: new Date().toISOString()
    });
};

module.exports = {
    logger,
    securityLogger,
    logInfo,
    logError,
    logWarning,
    logDebug,
    logSecurity,
    logConnection,
    logDisconnection,
    logRoomActivity,
    logCallQuality
};
