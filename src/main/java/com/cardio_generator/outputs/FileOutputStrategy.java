package com.cardio_generator.outputs;
 
/*
 * _ normally needs to be changed but the package is across more
 * than the 2 files needed to correct. 
 * */

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

    /* Base directory path where the output files will be stored. */

    /* changed the basedDirectory , as variables should be lowerCamelCase. */

    private String baseDirectory; 
    /** A thread-safe map that associates each label with its corresponding output file path. */
    private final ConcurrentHashMap<String, String> fileMap = new ConcurrentHashMap<>();
   
    /*changed public to private final and due to the encapsulation principle
     * and file_map to fileMap for the lowerCamelCase */

    /**
     * Constructs a FileOutputStrategy with a specified output directory.
     * 
     * @param baseDirectory The directory where data files will be written; must be a valid file system path
     */
    
    public FileOutputStrategy(String baseDirectory) { //methods should be lowerCamelCase

        this.baseDirectory = baseDirectory; }

        /* Changed again the variable to lowerCamelCase. */
    
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
    public void output(int patientId, long timestamp, String label, String data) { //timestamp should be lowerCamelCase
        try {
            // Create the directory
            Files.createDirectories(Paths.get(baseDirectory));
        } catch (IOException e) {
            System.err.println("Error creating base directory: " + e.getMessage());
            return;
        }


        // Set the FilePath variable
        String filePath = fileMap.computeIfAbsent(label, k -> Paths.get(baseDirectory, label + ".txt").toString());
        /*changed FilePath and file_map by the lowerCamelCase declaration problem 
         * added spacing for better reading. */

        // Write the data to the file

        //changed the Path file into lowerCamelCase
        
        try (PrintWriter out = new PrintWriter(
                Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
            out.printf("Patient ID: %d, Timestamp: %d, Label: %s, Data: %s%n", patientId, timestamp, label, data);
        } catch (IOException e) {
            System.err.println("Error writing to file " + filePath + ": " + e.getMessage());
        }

        /* catch IO type of exception for more precision
         *changed the FilePath into lowerCamelCase. */
    }
}