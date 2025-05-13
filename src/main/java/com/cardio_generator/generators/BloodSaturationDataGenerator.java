package com.cardio_generator.generators;

import java.util.Random;

import com.cardio_generator.outputs.OutputStrategy;

/**
 * This is a data generator for simulating blood oxygen saturation levels of patients.
 * Each patient starts with a baseline saturation value between 95 and 100.
 * After each generation step, the data might fluctuate lightly.
 * 
 * The saturation values are still kept within realistic values (90%-100%).
 * Data is sent to the OutputStrategy with the format of: patientId,timestamp,label,value,
 * 
 * Example label:{@code "Saturation"}, value:{@code "97%"}.
 * 
 * Any exceptions during data generation are caught and printed out to the standard error stream.
 * 
 * @author Octavian
 */

public class BloodSaturationDataGenerator implements PatientDataGenerator {
    private static final Random random = new Random();
    private int[] lastSaturationValues;

    /**
     * Constructs a BloodSaturationDataGenerator for a given number of patients.
     * Initializes each patient with a baseline saturation value between 95 and 100.
     * 
     * @param patientCount The number of patients to simulate.
     */

    public BloodSaturationDataGenerator(int patientCount) {
        lastSaturationValues = new int[patientCount + 1];

        // Initialize with baseline saturation values for each patient
        for (int i = 1; i <= patientCount; i++) {
            lastSaturationValues[i] = 95 + random.nextInt(6); // Initializes with a value between 95 and 100
        }
    }

    /**
     * Generates a new blood saturation value for a specific patient and sends it using the output strategy
     * The value fluctuates lightly around the last value and is constrained between 90% and 100%.
     * 
     * @param patientId  The ID of the patient.
     * @param outputStrategy The output strategy used to send the data.
     */
    @Override
    public void generate(int patientId, OutputStrategy outputStrategy) {
        try {
            // Simulate blood saturation values
            int variation = random.nextInt(3) - 1; // -1, 0, or 1 to simulate small fluctuations
            int newSaturationValue = lastSaturationValues[patientId] + variation;

            // Ensure the saturation stays within a realistic and healthy range
            newSaturationValue = Math.min(Math.max(newSaturationValue, 90), 100);
            lastSaturationValues[patientId] = newSaturationValue;
            outputStrategy.output(patientId, System.currentTimeMillis(), "Saturation",
                    Double.toString(newSaturationValue) + "%");
        } catch (Exception e) {
            System.err.println("An error occurred while generating blood saturation data for patient " + patientId);
            e.printStackTrace(); // This will print the stack trace to help identify where the error occurred.
        }
    }
}
