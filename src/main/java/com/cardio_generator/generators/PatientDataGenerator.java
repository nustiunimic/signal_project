package com.cardio_generator.generators;

import com.cardio_generator.outputs.OutputStrategy;
/**
 * Interface for generating simulated patient health data.
 * 
 * Implementing classes define how specific types of health data(e.g., ECG,
 * blood pressure)
 * are generated for a given patient and how the output is handled.
 * 
 * Usage:
 * Used by the simulator to periodically generate data per patient
 * and send it to a selected output strategy
 * 
 * @author Maria
 */

public interface PatientDataGenerator {
    /**
     * Generates simulated health data for a specific patient and sends it to 
     * the provided output strategy.
     * 
     * @param patientId The ID of the patient to generate data for; must be a positive integer
     * @param outputStrategy The output strategy to use for sending the generated data
     */
    void generate(int patientId, OutputStrategy outputStrategy);
}
