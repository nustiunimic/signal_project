package com.data_management;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A WebSocket client that connects to the health data WebSocket server,
 * parses incoming messages, and updates the DataStorage with the parsed information.
 */

public class WebSocketClientReader extends WebSocketClient implements DataReader  {

    // Singleton instance of DataStorage used to store parsed patient data
    private DataStorage dataStorage;

    // Reconnection management - prevents infinite reconnection attempts
    private final ScheduledExecutorService reconnectExecutor;
    private final AtomicInteger reconnectAttempts;
    private final AtomicBoolean shouldReconnect;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int BASE_RECONNECT_DELAY_SECONDS = 3;
    
    // Data quality tracking
    private long messagesReceived = 0;
    private long messagesProcessed = 0;
    private long malformedMessages = 0;


    /**
     * Constructor that initializes the WebSocket client with the server URI.
     *
     * @param serverUri the URI of the WebSocket server
     * @throws URISyntaxException if the URI format is incorrect
     */
    public WebSocketClientReader(URI serverUri){
        super(serverUri);
        this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WebSocketReconnect");
            t.setDaemon(true); // Don't prevent JVM shutdown
            return t;
        });
        this.reconnectAttempts = new AtomicInteger(0);
        this.shouldReconnect = new AtomicBoolean(true);
    }

    /**
     * Starts reading data by connecting to the WebSocket server.
     * Once connected, incoming messages will be handled asynchronously.
     *
     * @param dataStorage the shared instance of the storage where data will be added
     */
    @Override
public void startReading(DataStorage dataStorage){
    this.dataStorage = dataStorage;
    this.shouldReconnect.set(true);
    
    try {
        System.out.println("Connecting to WebSocket server: " + getURI());
        boolean connected = this.connectBlocking(10, TimeUnit.SECONDS); // 10 second timeout
        
        if (!connected) {
            System.err.println("Failed to connect to WebSocket server within timeout period");
            return; // ← SCHIMBAT din throw
        }
        
        System.out.println("Successfully connected to WebSocket server");
        
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt(); // Restore interrupt status
        System.err.println("Interrupted while connecting to WebSocket server: " + e.getMessage()); // ← SCHIMBAT din throw
    }
}

     /**
     * Gracefully stops the WebSocket client and cleans up resources
     */
    public void stopReading() {
        this.shouldReconnect.set(false);
        this.close();
        this.reconnectExecutor.shutdown();
        
        try {
            if (!reconnectExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                reconnectExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            reconnectExecutor.shutdownNow();
        }
        
        System.out.println("WebSocket client stopped. " + getStatistics());
    }

     /**
     * Triggered when the WebSocket connection is opened.
     * Resets reconnection counter on successful connection.
     */
    @Override
    public void onOpen(ServerHandshake handshakedata){
        System.out.println("Connected to WebSocket server");
        reconnectAttempts.set(0);
    }

      /**
     * Triggered upon receiving a message from the WebSocket server.
     * 
     * Handles potential corrupted data by:
     * - Validating message format and structure
     * - Parsing each field with specific error handling
     * - Logging detailed error information
     * 
     * Expected format: patientId,timestamp,label,data
     * Example: "123,1640995200000,HeartRate,72.5"
     *
     * @param message The incoming data string
     */
    @Override
    public void onMessage(String message){
        messagesReceived++;
        try {
            // Handle null or empty messages (corrupted data)
            if (message == null || message.trim().isEmpty()) {
                logDataCorruption("Received null or empty message", message);
                return;
            }

            // Parse and validate message format
            String[] parts = message.trim().split(",");
            if (parts.length != 4) {
                logDataCorruption("Invalid message format - expected 4 parts, got " + parts.length, message);
                return;
            }

            // Parse each field with specific validation
            int patientId = parsePatientId(parts[0].trim());
            long timestamp = parseTimestamp(parts[1].trim());
            String label = validateLabel(parts[2].trim());
            double measurementValue = parseMeasurementValue(parts[3].trim());

            // Final validation before storing
            if (isValidHealthData(patientId, timestamp, label, measurementValue)) {
                dataStorage.addPatientData(patientId, measurementValue, label, timestamp);
                messagesProcessed++;
                
                // Periodic progress reporting
                if (messagesProcessed % 100 == 0) {
                    System.out.println("Processed " + messagesProcessed + " messages. " + getStatistics());
                }
            } else {
                logDataCorruption("Data validation failed", message);
            }

        } catch (NumberFormatException e) {
            logDataCorruption("Number parsing error: " + e.getMessage(), message);
        } catch (Exception e) {
            logDataCorruption("Unexpected error: " + e.getMessage(), message);
            e.printStackTrace();
        }
}
 /**
     * Parses and validates patient ID
     */
    private int parsePatientId(String patientIdStr) throws NumberFormatException {
        int patientId = Integer.parseInt(patientIdStr);
        if (patientId <= 0) {
            throw new NumberFormatException("Patient ID must be positive: " + patientId);
        }
        return patientId;
    }
    /**
     * Parses and validates timestamp
     */
    private long parseTimestamp(String timestampStr) throws NumberFormatException {
        long timestamp = Long.parseLong(timestampStr);
        if (timestamp <= 0) {
            throw new NumberFormatException("Timestamp must be positive: " + timestamp);
        }
        
        // Warning for suspicious timestamps (more than 1 day in the future)
        long currentTime = System.currentTimeMillis();
        if (timestamp > currentTime + 86400000) {
            System.out.println("Warning: Future timestamp detected: " + timestamp);
        }
        
        return timestamp;
    }

    /**
     * Validates measurement label/type
     */
    private String validateLabel(String label) {
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException("Label cannot be null or empty");
        }
        
        // Log warning for unexpected labels but don't reject
        if (!isKnownHealthMetric(label)) {
            System.out.println("Warning: Unknown health metric: " + label);
        }
        
        return label;
    }

    /**
     * Parses and validates measurement value
     */
    private double parseMeasurementValue(String valueStr) throws NumberFormatException {
        double value = Double.parseDouble(valueStr);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new NumberFormatException("Invalid measurement value: " + value);
        }
        return value;
    }

    /**
     * Checks if the label represents a known health metric
     */
    private boolean isKnownHealthMetric(String label) {
        return label.equals("HeartRate") || label.equals("BloodPressure") || 
               label.equals("BloodSaturation") || label.equals("ECG") || 
               label.equals("Alert") || label.equals("BloodLevels") ||
               label.equals("Systolic") || label.equals("Diastolic");
    }

    /**
     * Final validation of health data before storage
     */
    private boolean isValidHealthData(int patientId, long timestamp, String label, double value) {
        // Business logic validation
        if (patientId <= 0 || timestamp <= 0 || label == null || label.isEmpty()) {
            return false;
        }
        
        // Value range validation (can be extended based on metric type)
        if (label.equals("HeartRate") && (value < 0 || value > 300)) {
            System.out.println("Warning: Suspicious heart rate value: " + value);
        }
        
        return true;
    }

    /**
     * Logs data corruption incidents with detailed information
     */
    private void logDataCorruption(String errorType, String message) {
        malformedMessages++;
        System.err.println("[DATA CORRUPTION] " + errorType + " - Raw message: '" + message + "'");
        System.err.println("[DATA CORRUPTION] Total corrupted messages: " + malformedMessages);
    }

    /**
     * Triggered when the WebSocket connection closes.
     * Implements intelligent reconnection with exponential backoff.
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket connection closed - Code: " + code + 
                          ", Reason: " + reason + ", Remote: " + remote);
        
        // Only attempt reconnection if we should and haven't exceeded max attempts
        if (shouldReconnect.get() && reconnectAttempts.get() < MAX_RECONNECT_ATTEMPTS) {
            int currentAttempt = reconnectAttempts.incrementAndGet();
            
            // Exponential backoff: 3s, 6s, 12s, 24s, 48s
            long delaySeconds = BASE_RECONNECT_DELAY_SECONDS * (1L << (currentAttempt - 1));
            
            System.out.println("Scheduling reconnection attempt " + currentAttempt + 
                             "/" + MAX_RECONNECT_ATTEMPTS + " in " + delaySeconds + " seconds");
            
            reconnectExecutor.schedule(this::performReconnect, delaySeconds, TimeUnit.SECONDS);
        } else if (reconnectAttempts.get() >= MAX_RECONNECT_ATTEMPTS) {
            System.err.println("Maximum reconnection attempts reached. Giving up.");
            System.err.println("Final statistics: " + getStatistics());
        }
    }

    /**
     * Performs the actual reconnection attempt
     */
    private void performReconnect() {
        if (shouldReconnect.get()) {
            try {
                System.out.println("Attempting to reconnect to WebSocket server...");
                this.reconnect();
            } catch (Exception e) {
                System.err.println("Reconnection failed: " + e.getMessage());
            }
        }
    }

    /**
     * Triggered when a WebSocket error occurs.
     * Provides detailed error classification for better debugging.
     *
     * @param ex The thrown exception
     */
    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket error occurred: " + ex.getMessage());
        
        // Classify error types for better handling
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("Connection refused")) {
                System.err.println("Server appears to be down or unreachable");
            } else if (ex.getMessage().contains("timeout")) {
                System.err.println("Connection timeout - check network connectivity");
            } else if (ex.getMessage().contains("Connection reset")) {
                System.err.println("Connection was reset - possible server restart");
            }
        }
        
        ex.printStackTrace();
    }

    /**
     * Returns statistics about data processing quality
     */
    public String getStatistics() {
        double successRate = messagesReceived > 0 ? 
            (double) messagesProcessed / messagesReceived * 100 : 0;
        
        return String.format("Messages - Received: %d, Processed: %d, Corrupted: %d, Success Rate: %.2f%%",
                messagesReceived, messagesProcessed, malformedMessages, successRate);
    }
}
