package com.data_management;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A WebSocket client that connects to the health data WebSocket server,
 * parses incoming messages, and updates the DataStorage with the parsed information.
 */

public class WebSocketClientReader extends WebSocketClient implements DataReader  {

    // Singleton instance of DataStorage used to store parsed patient data
    private DataStorage datastorage;

    /**
     * Constructor that initializes the WebSocket client with the server URI.
     *
     * @param serverUri the URI of the WebSocket server
     * @throws URISyntaxException if the URI format is incorrect
     */
    public WebSocketClientReader(String serverUri) throws URISyntaxException{
        super(new URI(serverUri));
    }

    /**
     * Starts reading data by connecting to the WebSocket server.
     * Once connected, incoming messages will be handled asynchronously.
     *
     * @param dataStorage the shared instance of the storage where data will be added
     */
    @Override
    public void startReading(DataStorage dataStorage){
        this.datastorage = dataStorage;
        this.connect();
    }

     /**
     * Called automatically when the WebSocket connection is successfully opened.
     *
     * @param handshakedata details about the handshake (not used here)
     */
    @Override
    public void onOpen(ServerHandshake handshakedata){
        System.out.println("Connected to WebSocket server");
    }

     /**
     * Handles incoming messages from the WebSocket server.
     * Each message is expected to follow the format:
     * patientId,measurementValue,recordType,timestamp
     *
     * This method parses the message and updates the DataStorage accordingly.
     *
     * @param message the raw string message received from the server
     */
    @Override
    public void onMessage(String message){
        try {
            //Assuming format : patientId, measurementValue, recordType, timestamp
            String[] parts = message.split(",");
            if(parts.length != 4){
                System.err.println("Malformed message: " + message);
                return;
            }
            
            int patientId = Integer.parseInt(parts[0]);
            double measurementValue = Double.parseDouble(parts[1]);
            String recordType = parts[2];
            long timestamp = Long.parseLong(parts[3]);

            datastorage.addPatientData(patientId, measurementValue, recordType, timestamp);
    } catch (Exception e){
        System.err.println("Error parsing message: " + message);
            e.printStackTrace();
    }

}
/**
     * Called when the WebSocket connection is closed.
     *
     * @param code the closing code
     * @param reason the reason for closing
     * @param remote whether the closure was initiated remotely
     */
@Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket connection closed: " + reason);
    }

    /**
     * Called when an error occurs with the WebSocket connection.
     *
     * @param ex the exception that was thrown
     */
    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket error: ");
        ex.printStackTrace();
    }
}