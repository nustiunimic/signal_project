package com.cardio_generator.outputs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

/**
 * An implementation of the OutputStrategy that transmits the patient data over TCP.
 * This class establishes a TCP server socket on a specified port and waits until a client connects.
 * After the connection, the data is formated and sent to him (the patient) via a PrintWriter stream.
 * 
 * The server runs in a separated thread from the main one to avoid blocking it.
 * When the data is received through the output method, it is formatted as comma-separated values,
 * and is transmitted to the connected client.
 * 
 * Format:patientId,timestamp,label,data .
 * 
 * Example output: {@code 112,17155398500000,HeartRate,75}
 * 
 * @author Octavian
 */

public class TcpOutputStrategy implements OutputStrategy {

    /** The server socket that listens for client connections. */
    private ServerSocket serverSocket;

    /** Socket connection to the client. */
    private Socket clientSocket;

    /** PrintWriter used to send the data to the connected client. */
    private PrintWriter out;

    /**
     * Constructs a TcpOutputStrategy that listens on the specified port.
     * Initializes a server socket and starts a separate thread to accept client connections.
     * When a client connects, a PrintWriter is established for sending the data to the client.
     *If {@link IOException} occurs while opening the server socket or accepting the connection,
     it will be caught and printed to the standard error stream.
     * @param port The TCP port number on which is listened for connections.
     */

    public TcpOutputStrategy(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("TCP Server started on port " + port);

            // Accept clients in a new thread to not block the main thread
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    clientSocket = serverSocket.accept();
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    System.out.println("Client connected: " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Outputs patient data to the connected TCP client.
     * The data is formatted as a comma-separated string in the format: patientId,timestamp,label,data.
     * If no client is connected, the method silently returns without sending any data.
     * 
     * @param patientId The ID of the patient whose data is being output
     * @param timestamp The time the data was generated
     * @param label The type of measurment(HeartRate/BloodPressure)
     * @param data The actual measured value as a string representation
     * 
     */

    @Override
    public void output(int patientId, long timestamp, String label, String data) {
        if (out != null) {
            String message = String.format("%d,%d,%s,%s", patientId, timestamp, label, data);
            out.println(message);
        }
    }
}
