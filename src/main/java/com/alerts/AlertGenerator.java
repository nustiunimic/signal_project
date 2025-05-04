package com.alerts;

import java.util.List;

import com.data_management.DataStorage; 
import com.data_management.Patient;
import com.data_management.PatientRecord;

/* packets generally should not use _ but it gives error otherwise */

/**
 * The {@code AlertGenerator} class is responsible for monitoring patient data
 * and generating alerts when certain predefined conditions are met. This class
 * relies on a {@link DataStorage} instance to access patient data and evaluate
 * it against specific health criteria.
 * 
 * The class is designed to be extensible and allows adding new alert conditions,
 * based on different types of patient data(heart rate/blood pressure etc.).
 * 
 * When alert conditions are met (abnormal heart rate/ oxygen dangerous levels),
 * the alert system is triggered. This can notify the medical staff, logging or
 * any other action needed.
 * 
 * For example , the following conditions are checked:
 * Heart rate exceeding 120bpm and Oxygen level falling below 90% triggers an alert.
 * 
 * @author Octavian
 */
public class AlertGenerator {

    /** The data storage system used to retrieve patient data for evaluation. */
    
    private final DataStorage dataStorage;
    
    /* datastorage should be final */

    /**
     * Constructs an {@code AlertGenerator} with a specified {@code DataStorage}.
     * The {@code DataStorage} is used to retrieve patient data that this class
     * will monitor and evaluate.
     *
     * @param dataStorage the data storage system that provides access to patient data
     */
    
     /* improved spacing for better reading  */

    public AlertGenerator(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    /**
     * Evaluates the specified patient's data to determine if any alert conditions
     * are met. If a condition is met, an alert is triggered via the
     * {@link #triggerAlert}
     * method. This method should define the specific conditions under which an
     * alert will be triggered. //improved spacing
     *
     * @param patient the patient data to evaluate for alert conditions
     */

    public void evaluateData(Patient patient) {
        // Implementation goes here
         long now = System.currentTimeMillis();
        long lastHour = now - 60 * 60 * 1000;

        /** Get all patient records from the last hour */
        List<PatientRecord> recentRecords = patient.getRecords(lastHour, now);

        /** Iterated through the records to check conditions. */
        for (PatientRecord record : recentRecords) {
            String type = record.getRecordType();
            double value = record.getMeasurementValue();

            /** Checks for high heart rate condition */

            if (type.equals("HeartRate") && value > 120) {
                triggerAlert(new Alert(
                    String.valueOf(patient.getPatientId()), 
                    "High Heart Rate: " + value + " bpm", 
                    record.getTimestamp()
                ));
            }

            /** Checks for low oxygen level */
            if (type.equals("OxygenLevel") && value < 90) {
                triggerAlert(new Alert(
                    String.valueOf(patient.getPatientId()), 
                    "Low Oxygen Level: " + value + "%", 
                    record.getTimestamp()
                ));
            }

            // More conditions can be added here (e.g., blood pressure, temperature)
        }
        
    }

    /**
     * Triggers an alert for the monitoring system. This method can be extended to
     * notify medical staff, log the alert, or perform other actions. The method
     * currently assumes that the alert information is fully formed when passed as
     * an argument.
     *
     * @param alert the alert object containing details about the alert condition
     */
    private void triggerAlert(Alert alert) {
        // Implementation might involve logging the alert or notifying staff
    }
}
