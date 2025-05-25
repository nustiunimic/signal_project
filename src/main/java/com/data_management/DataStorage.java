package com.data_management;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.alerts.AlertGenerator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Set;

/**
 * Manages storage and retrieval of patient data within a healthcare monitoring
 * system.
 * This class serves as a repository for all patient records, organized by
 * patient IDs.
 */
public class DataStorage {
    private static DataStorage instance;
    private static final Object lock = new Object();
    
    //Thread-safe storage for concurrent access
    private final Map<Integer, Patient> patientMap;

    //ReadWriteLock for efficient conccurrent acess
    //Multiple readers can access simultaneouslt, but writes are exclusive
    private final ReadWriteLock dataLock = new ReentrantReadWriteLock();

    //Listeners for real-time data updates
    private final List<DataUpdateListener> updateListeners = new CopyOnWriteArrayList<>();

    //Duplicate detection - stores unique record identifiers 
    private final Set<String> recordIdentifiers = ConcurrentHashMap.newKeySet();

    //Statistics for monitoring system performance
    private volatile long totalRecordsAdded = 0;
    private volatile long duplicatesRejected = 0;
    private volatile long lastUpdateTimestamp = 0;

    /**
     * Interface for listening to real-time data updates
     */
    public interface DataUpdateListener {
        /**
         * Called when new patient data is added to the system
         * @param patientId The ID of the patient
         * @param record The new patient record that was added
         */
        void onDataUpdated(int patientId, PatientRecord record);
        
        /**
         * Called when a new patient is created in the system
         * @param patient The newly created patient
         */
        void onNewPatientAdded(Patient patient);
    }


    /**
     * Constructs a new instance of DataStorage, initializing the underlying storage
     * structure with thread-safe components.
     */
    private DataStorage() {
        //Use ConcurrentHashMap for thread-safe operations
        this.patientMap = new ConcurrentHashMap<>();
        System.out.println("DataStorage singleton instance created with concurrent data integration support.");
    }

    /**
     * Thread-safe singleton pattern implementation
     * @return
     */
    public static DataStorage getInstance(){
        if(instance == null){
            synchronized(lock){
                if(instance == null){
                    instance = new DataStorage();
                }
            }
        }
        return instance;
    }
    /**
 * Adds or updates patient data in the storage with real-time integration support.
 * 
 * Features:
 * - Thread-safe concurrent updates
 * - Duplicate detection and prevention
 * - Real-time listener notifications
 * - Data freshness tracking
 *
 * @param patientId        the unique identifier of the patient
 * @param measurementValue the value of the health metric being recorded
 * @param recordType       the type of record, e.g., "HeartRate", "BloodPressure"
 * @param timestamp        the time at which the measurement was taken, in milliseconds since Unix epoch
 */
public void addPatientData(int patientId, double measurementValue, String recordType, long timestamp) {
    // Input validation
    if (patientId <= 0) {
        System.err.println("Invalid patient ID: " + patientId + ". Skipping data addition.");
        return;
    }
    if (recordType == null || recordType.trim().isEmpty()) {
        System.err.println("Invalid record type for patient " + patientId + ". Skipping data addition.");
        return;
    }
    if (timestamp <= 0) {
        System.err.println("Invalid timestamp for patient " + patientId + ". Skipping data addition.");
        return;
    }

    // Create unique identifier for duplicate detection
    String recordId = createRecordIdentifier(patientId, recordType, timestamp, measurementValue);
    
    // Check for duplicates - this prevents data corruption from multiple sources
    if (recordIdentifiers.contains(recordId)) {
        duplicatesRejected++;
        System.out.println("Duplicate record detected and rejected: " + recordId);
        return;
    }

    // Variables to hold data for listener notification
    PatientRecord newRecord = null;
    Patient newPatient = null;

    // Use write lock for data modification
    dataLock.writeLock().lock();
    try {
        // Get or create patient
        Patient patient = patientMap.get(patientId);
        boolean isNewPatient = false;
        
        if (patient == null) {
            patient = new Patient(patientId);
            patientMap.put(patientId, patient);
            isNewPatient = true;
            newPatient = patient; // Store for listener notification
            System.out.println("New patient created with ID: " + patientId);
        }

        // Add the record to patient
        patient.addRecord(measurementValue, recordType, timestamp);
        
        // Mark record as processed to prevent duplicates
        recordIdentifiers.add(recordId);
        
        // Update statistics
        totalRecordsAdded++;
        lastUpdateTimestamp = System.currentTimeMillis();
        
        // Create PatientRecord for listeners
        newRecord = new PatientRecord(patientId, measurementValue, recordType, timestamp);
        
    } catch (Exception e) {
        System.err.println("Error adding patient data: " + e.getMessage());
        e.printStackTrace();
    } finally {
        // Always release the lock - only once!
        dataLock.writeLock().unlock();
    }

    // Notify listeners OUTSIDE the lock to prevent deadlocks
    if (newRecord != null) {
        notifyListeners(patientId, newRecord, newPatient);
    }
}

    /**
     * Retrieves a list of PatientRecord objects for a specific patient, filtered by
     * a time range.
     *
     * @param patientId the unique identifier of the patient whose records are to be
     *                  retrieved
     * @param startTime the start of the time range, in milliseconds since the Unix
     *                  epoch
     * @param endTime   the end of the time range, in milliseconds since the Unix
     *                  epoch
     * @return a list of PatientRecord objects that fall within the specified time
     *         range
     */
    public List<PatientRecord> getRecords(int patientId, long startTime, long endTime) {
        dataLock.readLock().lock();
        try{
            Patient patient = patientMap.get(patientId);
            if(patient != null){
                return patient.getRecords(startTime, endTime);
            }
            return new ArrayList<>(); //return empty list if no patient found
        }finally{
            dataLock.readLock().unlock();
        }
    }

    /**
     * Retrieves a collection of all patients stored in the data storage.
     *
     * @return a list of all patients
     */
    public List<Patient> getAllPatients() {
        dataLock.readLock().lock();
        try{
            return new ArrayList<>(patientMap.values());
        } finally{
            dataLock.readLock().unlock();
        }

    }

    /**
     * Thread-safe patient count retrieval
     * @return
     */
    public int getPatientCount(){
        dataLock.readLock().lock();
        try{
            return patientMap.size();
        } finally{
            dataLock.readLock().unlock();
        }
    }

    /**
     * Thread-safe patient existence check
     * @param patientId
     * @return
     */
    public boolean hasPatient(int patientId){
        dataLock.readLock().lock();
        try{
            return patientMap.containsKey(patientId);
        } finally{
            dataLock.readLock().unlock();
        }
    }

    /**
     * Thread-safe patient retrieval
     * @param patientId
     * @return
     */
    public Patient getPatient(int patientId){
        dataLock.readLock().lock();
        try{
            return patientMap.get(patientId);
        } finally{
            dataLock.readLock().unlock();
        }
    }

    /**
     * Thread-safe data clearing with proper cleanup
     */
    public void clearAllData(){
        dataLock.writeLock().lock();
        try{
            patientMap.clear();
            recordIdentifiers.clear();
            totalRecordsAdded = 0;
            duplicatesRejected = 0;
            lastUpdateTimestamp = 0;
            System.out.println("All patient data cleared from DataStorage singleton.");
        } finally{
            dataLock.writeLock().unlock();
        }
    }

    /**
     * Registers a listener for real-time data updates
     * @param listener The listener to register
     */
    public void addDataUpdateListener(DataUpdateListener listener){
        if(listener != null){
            updateListeners.add(listener);
            System.out.println("Data update listener registered.Total listeners " + updateListeners.size());
        }
    }


    /**
     * Removes a data update listener
     * @param listener The Listener to remove
     */
    public void removeDataUpdateListener(DataUpdateListener listener){
        if(listener != null){
            updateListeners.remove(listener);
            System.out.println("Data update listener removed. Remaining listeners:" + updateListeners.size());
        }
    }

    /**
     * Creates a unique identifier for a record to prevent duplicates
     */
    private String createRecordIdentifier(int patientId, String recordType, long timestamp, double value){
        return String.format("%d_%s_%d_%.6f", patientId, recordType, timestamp, value);
    }

    /**
     * Notifies all registered listeners about data updates
     */
    private void notifyListeners(int patientId, PatientRecord record, Patient newPatient) {
        for (DataUpdateListener listener : updateListeners) {
            try {
                // Notify about new data
                listener.onDataUpdated(patientId, record);
                
                // Notify about new patient if applicable
                if (newPatient != null) {
                    listener.onNewPatientAdded(newPatient);
                }
            } catch (Exception e) {
                System.err.println("Error notifying listener: " + e.getMessage());
                // Continue with other listeners even if one fails
            }
        }
    }

    /**
     * Returns system statistics for monitoring performance
     */
    public String getSystemStatistics() {
        dataLock.readLock().lock();
        try {
            return String.format(
                "DataStorage Statistics - Patients: %d, Total Records: %d, Duplicates Rejected: %d, " +
                "Last Update: %d, Active Listeners: %d",
                patientMap.size(), totalRecordsAdded, duplicatesRejected, 
                lastUpdateTimestamp, updateListeners.size()
            );
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * Checks if the system has received recent data updates
     * @param maxAgeMs Maximum age in milliseconds to consider data "fresh"
     * @return true if data is fresh, false otherwise
     */
    public boolean isDataFresh(long maxAgeMs) {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastUpdateTimestamp) <= maxAgeMs;
    }

    /**
     * Gets the most recent records across all patients for real-time monitoring
     * @param maxRecords Maximum number of recent records to return
     * @return List of most recent patient records
     */
    public List<PatientRecord> getRecentRecords(int maxRecords) {
        dataLock.readLock().lock();
        try {
            List<PatientRecord> allRecords = new ArrayList<>();
            
            // Collect all recent records from all patients
            for (Patient patient : patientMap.values()) {
                List<PatientRecord> patientRecords = patient.getRecords(0, Long.MAX_VALUE);
                allRecords.addAll(patientRecords);
            }
            
            // Sort by timestamp (most recent first) and limit results
            allRecords.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
            
            return allRecords.subList(0, Math.min(maxRecords, allRecords.size()));
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * The main method for the DataStorage class.
     * Initializes the system, reads data into storage, and continuously monitors
     * and evaluates patient data.
     * 
     * @param args command line arguments
     */
    
     /**
     * The main method for the DataStorage class with continuous integration example.
     */
    public static void main(String[] args) {
        DataStorage storage = DataStorage.getInstance();
        DataStorage storage2 = DataStorage.getInstance();
        System.out.println("Same instance? " + (storage == storage2));

        // Example of setting up real-time data listener
        storage.addDataUpdateListener(new DataUpdateListener() {
            @Override
            public void onDataUpdated(int patientId, PatientRecord record) {
                System.out.println("REAL-TIME UPDATE: Patient " + patientId + 
                                 " - " + record.getRecordType() + ": " + record.getMeasurementValue());
                
                // Here you could trigger alerts, update dashboards, etc.
                // For example, check for critical values:
                if ("HeartRate".equals(record.getRecordType()) && record.getMeasurementValue() > 150) {
                    System.out.println("ALERT: High heart rate detected for patient " + patientId);
                }
            }

            @Override
            public void onNewPatientAdded(Patient patient) {
                System.out.println("NEW PATIENT: " + patient.getPatientId() + " added to monitoring system");
            }
        });

        // Simulate real-time data arrival
        System.out.println("\n=== Simulating Real-time Data Integration ===");
        
        // Add some test data to demonstrate real-time integration
        storage.addPatientData(1001, 72.5, "HeartRate", System.currentTimeMillis());
        storage.addPatientData(1001, 120.0, "SystolicBP", System.currentTimeMillis() + 1000);
        storage.addPatientData(1002, 180.0, "HeartRate", System.currentTimeMillis() + 2000); // This will trigger alert
        
        // Try to add duplicate data
        storage.addPatientData(1001, 72.5, "HeartRate", System.currentTimeMillis());
        
        // Print system statistics
        System.out.println("\n" + storage.getSystemStatistics());
        
        // Example of using real-time features
        System.out.println("Data is fresh (last 10 seconds): " + storage.isDataFresh(10000));
        
        List<PatientRecord> recent = storage.getRecentRecords(5);
        System.out.println("Most recent records: " + recent.size());

        // Initialize AlertGenerator for continuous monitoring
        AlertGenerator alertGenerator = new AlertGenerator(storage);

        // Evaluate all patients' data to check for conditions that may trigger alerts
        for (Patient patient : storage.getAllPatients()) {
            alertGenerator.evaluateData(patient);
        }
    }
}
