package com.cardio_generator.outputs;

/**
 * Defines the strategy interface for outputting simulated patient data.
 * 
 * Implementing classes determine how and where the generated data is sent or stored,
 * such as printing to the console, saving to a file, or sending over a network.
 * 
 * Usage:
 * Used by all data generators to abstract away the output mechanism,
 * allowing flexible output strategies.
 * 
 * @author Maria
 */

public interface OutputStrategy {

    /**
     * Outputs the generated patient data using the specific strategy.
     * 
     * @param patientId The ID of the patient; must be a positive integer
     * @param timestamp The time of data generation in milliseconds since the Unix epoch
     * @param label The type of measurement (e.g., "HeartRate", "BloodPressure")
     * @param data The actual value or message to be output; should be a valid, non-null string
     */
    void output(int patientId, long timestamp, String label, String data);
}
