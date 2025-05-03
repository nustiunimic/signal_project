package com.cardio_generator.outputs;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of OutputStrategy that writes patient data to text files.
 * Each label (e.g., "HeartRate") gets its own file in the specified directory.
 * Thread-safe using ConcurrentHashMap to avoid conflicts when writing to files.
 *
 * Files are created in the provided base directory, and data is appended line by line.
 * Format: Patient ID, Timestamp, Label, Data
 * 
 * @author Maria
 */

public class FileOutputStrategy implements OutputStrategy {

    /** Base directory path where the output files will be stored. */
    private String BaseDirectory;

    /** A thread-safe map that associates each label with its corresponding output file path. */
    public final ConcurrentHashMap<String, String> file_map = new ConcurrentHashMap<>();

    /**
     * Constructs a FileOutputStrategy with a specified output directory.
     * 
     * @param baseDirectory The directory where data files will be written; must be a valid file system path
     */
    public FileOutputStrategy(String baseDirectory) {

        this.BaseDirectory = baseDirectory;
    }

    /**
     * Outputs patient data to a file corresponding to the data label.
     * Creates the base directory if it does not exist.
     * Writes formatted patient data to a label-specific text file.
     *
     * @param patientId The ID of the patient whose data is being output; must be a positive integer
     * @param timestamp The time the data was generated, in milliseconds since Unix epoch
     * @param label The type of measurement (e.g., "HeartRate", "ECG"); used as filename
     * @param data The actual measured value or generated string; must not be null
     */

    @Override
    public void output(int patientId, long timestamp, String label, String data) {
        try {
            // Create the directory
            Files.createDirectories(Paths.get(BaseDirectory));
        } catch (IOException e) {
            System.err.println("Error creating base directory: " + e.getMessage());
            return;
        }
        // Set the FilePath variable
        String FilePath = file_map.computeIfAbsent(label, k -> Paths.get(BaseDirectory, label + ".txt").toString());

        // Write the data to the file
        try (PrintWriter out = new PrintWriter(
                Files.newBufferedWriter(Paths.get(FilePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
            out.printf("Patient ID: %d, Timestamp: %d, Label: %s, Data: %s%n", patientId, timestamp, label, data);
        } catch (Exception e) {
            System.err.println("Error writing to file " + FilePath + ": " + e.getMessage());
        }
    }
}