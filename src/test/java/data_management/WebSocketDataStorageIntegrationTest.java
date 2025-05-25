package data_management;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.data_management.DataStorage;
import com.data_management.WebSocketClientReader;
import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests specifically for WebSocketClientReader with DataStorage.
 * Tests real-time data processing, listener notifications, and data integrity.
 */
class WebSocketDataStorageIntegrationTest {

    private WebSocketClientReader webSocketClient;
    private DataStorage dataStorage;
    private TestDataListener testListener;

    @BeforeEach
    void setUp() throws URISyntaxException {
        // Use singleton DataStorage instance
        dataStorage = DataStorage.getInstance();
        dataStorage.clearAllData();
        
        // Set up test data listener
        testListener = new TestDataListener();
        dataStorage.addDataUpdateListener(testListener);
        
        // Create WebSocket client that doesn't actually connect
        webSocketClient = new WebSocketClientReader(new URI("ws://localhost:8080")) {
            @Override
            public boolean connectBlocking(long timeout, TimeUnit timeUnit) {
                return true; // Mock successful connection
            }
        };
        
        webSocketClient.startReading(dataStorage);
    }

    @AfterEach
    void tearDown() {
        if (webSocketClient != null) {
            webSocketClient.stopReading();
        }
        if (dataStorage != null && testListener != null) {
            dataStorage.removeDataUpdateListener(testListener);
            dataStorage.clearAllData();
        }
    }

    /**
     * Test that data is stored correctly in DataStorage
     */
    @Test
    void testDataStorageIntegration() {
        // When: Processing a valid health data message
        webSocketClient.onMessage("150,1714376789050,HeartRate,78.5");
        
        // Then: Data should be stored in DataStorage
        assertEquals(1, dataStorage.getPatientCount(), "One patient should be created");
        Patient patient = dataStorage.getPatient(150);
        assertNotNull(patient, "Patient 150 should exist in DataStorage");
        
        // And: Patient should have the correct record
        List<PatientRecord> records = dataStorage.getRecords(150, 0, Long.MAX_VALUE);
        assertEquals(1, records.size(), "Patient should have one record");
        PatientRecord record = records.get(0);
        assertEquals("HeartRate", record.getRecordType(), "Record type should match");
        assertEquals(78.5, record.getMeasurementValue(), 0.001, "Measurement value should match");
    }

    /**
     * Test real-time listener notifications during WebSocket data processing
     */
    @Test
    void testRealTimeListenerNotifications() throws InterruptedException {
        // Given: Expecting notifications for new data
        CountDownLatch dataLatch = new CountDownLatch(2);
        CountDownLatch patientLatch = new CountDownLatch(1);
        testListener.setLatch(dataLatch, patientLatch);
        
        // When: Processing messages for new patient
        webSocketClient.onMessage("250,1714376789050,HeartRate,82.0");
        webSocketClient.onMessage("250,1714376789051,BloodPressure,125.0");
        
        // Then: All notifications should be received
        assertTrue(dataLatch.await(3, TimeUnit.SECONDS), "Should receive data update notifications");
        assertTrue(patientLatch.await(3, TimeUnit.SECONDS), "Should receive new patient notification");
        
        // And: Listener should have captured the events
        assertEquals(2, testListener.getDataUpdateCount(), "Should receive 2 data update events");
        assertEquals(1, testListener.getNewPatientCount(), "Should receive 1 new patient event");
    }

    /**
     * Test duplicate detection integration with WebSocket data flow
     */
    @Test
    void testDuplicateDetectionIntegration() {
        // When: Sending the same message twice
        webSocketClient.onMessage("300,1714376789050,HeartRate,75.0");
        webSocketClient.onMessage("300,1714376789050,HeartRate,75.0"); // Exact duplicate
        
        // Then: Only one record should be stored
        List<PatientRecord> records = dataStorage.getRecords(300, 0, Long.MAX_VALUE);
        assertEquals(1, records.size(), "Duplicate should be rejected, only one record stored");
        
        // And: DataStorage statistics should show duplicate rejection
        String stats = dataStorage.getSystemStatistics();
        assertTrue(stats.contains("Duplicates Rejected: 1"), "Should show 1 duplicate rejected");
    }

    /**
     * Test multiple patients with concurrent data processing
     */
    @Test
    void testMultiplePatientsConcurrentProcessing() throws InterruptedException {
        // Given: Expecting multiple patients and records
        CountDownLatch dataLatch = new CountDownLatch(6);
        CountDownLatch patientLatch = new CountDownLatch(3);
        testListener.setLatch(dataLatch, patientLatch);
        
        // When: Processing data for multiple patients concurrently
        webSocketClient.onMessage("401,1714376789050,HeartRate,70.0");
        webSocketClient.onMessage("402,1714376789051,BloodPressure,115.0");
        webSocketClient.onMessage("403,1714376789052,ECG,0.9");
        webSocketClient.onMessage("401,1714376789053,BloodSaturation,98.0");
        webSocketClient.onMessage("402,1714376789054,Alert,0.0");
        webSocketClient.onMessage("403,1714376789055,HeartRate,85.0");
        
        // Then: All data should be processed correctly
        assertTrue(dataLatch.await(5, TimeUnit.SECONDS), "All data updates should be received");
        assertTrue(patientLatch.await(5, TimeUnit.SECONDS), "All new patients should be created");
        
        // And: All patients should exist with correct data
        assertEquals(3, dataStorage.getPatientCount(), "3 patients should be created");
        assertNotNull(dataStorage.getPatient(401), "Patient 401 should exist");
        assertNotNull(dataStorage.getPatient(402), "Patient 402 should exist");
        assertNotNull(dataStorage.getPatient(403), "Patient 403 should exist");
        
        // And: Each patient should have correct number of records
        assertEquals(2, dataStorage.getRecords(401, 0, Long.MAX_VALUE).size(), "Patient 401 should have 2 records");
        assertEquals(2, dataStorage.getRecords(402, 0, Long.MAX_VALUE).size(), "Patient 402 should have 2 records");
        assertEquals(2, dataStorage.getRecords(403, 0, Long.MAX_VALUE).size(), "Patient 403 should have 2 records");
    }

    /**
     * Test data freshness tracking with WebSocket data flow
     */
    @Test
    void testDataFreshnessTracking() {
        // When: Processing recent data
        webSocketClient.onMessage("500,1714376789050,HeartRate,76.0");
        
        // Then: Data should be considered fresh
        assertTrue(dataStorage.isDataFresh(5000), "Data should be fresh within 5 seconds");
        
        // And: System statistics should reflect recent activity
        String stats = dataStorage.getSystemStatistics();
        assertTrue(stats.contains("Total Records: 1"), "Should show 1 total record");
        assertFalse(stats.contains("Last Update: 0"), "Should show non-zero last update timestamp");
    }

    /**
     * Test retrieval of recent records after WebSocket processing
     */
    @Test
    void testRecentRecordsRetrieval() {
        // When: Processing multiple messages over time
        webSocketClient.onMessage("600,1714376789050,HeartRate,72.0");
        webSocketClient.onMessage("601,1714376789051,BloodPressure,118.0");
        webSocketClient.onMessage("602,1714376789052,ECG,0.7");
        
        // Then: Recent records should be retrievable
        List<PatientRecord> recentRecords = dataStorage.getRecentRecords(5);
        assertEquals(3, recentRecords.size(), "Should retrieve 3 recent records");
        
        // And: Records should be sorted by timestamp (most recent first)
        assertTrue(recentRecords.get(0).getTimestamp() >= recentRecords.get(1).getTimestamp(),
                "Records should be sorted by timestamp, newest first");
    }

    /**
     * Test system statistics accuracy during WebSocket data processing
     */
    @Test
    void testSystemStatisticsAccuracy() {
        // When: Processing various messages
        webSocketClient.onMessage("700,1714376789050,HeartRate,74.0");  // Valid
        webSocketClient.onMessage("701,1714376789051,ECG,0.8");         // Valid
        webSocketClient.onMessage("invalid-format");                     // Invalid (won't reach DataStorage)
        webSocketClient.onMessage("700,1714376789050,HeartRate,74.0");  // Duplicate
        
        // Then: DataStorage statistics should be accurate
        String stats = dataStorage.getSystemStatistics();
        assertTrue(stats.contains("Patients: 2"), "Should show 2 unique patients");
        assertTrue(stats.contains("Total Records: 2"), "Should show 2 total records stored");
        assertTrue(stats.contains("Duplicates Rejected: 1"), "Should show 1 duplicate rejected");
        assertTrue(stats.contains("Active Listeners: 1"), "Should show 1 active listener");
    }

    /**
     * Test WebSocket integration with DataStorage error scenarios
     */
    @Test
    void testIntegrationWithDataStorageErrors() {
        // When: Processing valid data that should work
        webSocketClient.onMessage("800,1714376789050,HeartRate,79.0");
        webSocketClient.onMessage("801,1714376789051,BloodPressure,130.0");
        
        // Then: Data should be stored successfully
        assertEquals(2, dataStorage.getPatientCount(), "Should create 2 patients");
        
        // When: Processing invalid data mixed with valid
        webSocketClient.onMessage("invalid-data");
        webSocketClient.onMessage("802,1714376789052,ECG,0.6");
        
        // Then: System should continue working normally
        assertEquals(3, dataStorage.getPatientCount(), "Should create 3rd patient despite invalid data");
    }

    /**
     * Test long-running integration scenario
     */
    @Test
    void testLongRunningIntegration() {
        // When: Processing many messages over time
        for (int i = 0; i < 50; i++) {
            int patientId = 900 + (i % 10); // 10 different patients
            String recordType = (i % 2 == 0) ? "HeartRate" : "BloodPressure";
            double value = 70.0 + (i % 30);
            long timestamp = 1714376789050L + i;
            
            String message = String.format("%d,%d,%s,%.1f", patientId, timestamp, recordType, value);
            webSocketClient.onMessage(message);
        }
        
        // Then: All data should be processed correctly
        assertEquals(10, dataStorage.getPatientCount(), "Should create 10 unique patients");
        
        // Each patient should have multiple records
        for (int i = 0; i < 10; i++) {
            int patientId = 900 + i;
            List<PatientRecord> records = dataStorage.getRecords(patientId, 0, Long.MAX_VALUE);
            assertEquals(5, records.size(), "Each patient should have 5 records");
        }
        
        // System statistics should be accurate
        String stats = dataStorage.getSystemStatistics();
        assertTrue(stats.contains("Total Records: 50"), "Should show 50 total records");
    }

    /**
     * Helper class to capture DataStorage events during testing
     */
    private static class TestDataListener implements DataStorage.DataUpdateListener {
        private volatile int dataUpdateCount = 0;
        private volatile int newPatientCount = 0;
        private CountDownLatch dataLatch;
        private CountDownLatch patientLatch;
        
        public void setLatch(CountDownLatch dataLatch, CountDownLatch patientLatch) {
            this.dataLatch = dataLatch;
            this.patientLatch = patientLatch;
        }
        
        @Override
        public void onDataUpdated(int patientId, PatientRecord record) {
            dataUpdateCount++;
            if (dataLatch != null) {
                dataLatch.countDown();
            }
        }
        
        @Override
        public void onNewPatientAdded(Patient patient) {
            newPatientCount++;
            if (patientLatch != null) {
                patientLatch.countDown();
            }
        }
        
        public int getDataUpdateCount() { return dataUpdateCount; }
        public int getNewPatientCount() { return newPatientCount; }
    }
}