package data_management;

import com.data_management.Patient;
import com.data_management.PatientRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Patient that tests both core functionality and edge cases
 */
public class PatientTest {
    private Patient patient;
    private final int PATIENT_ID = 1234;
    
    @BeforeEach
    void setUp() {
        patient = new Patient(PATIENT_ID);
        
        // Adăugăm doar înregistrări pentru după-amiază (15:00 - 15:10)
        patient.addRecord(78.0, "HeartRate", 1715094000000L); // 7 mai 2024, 15:00
        patient.addRecord(82.0, "HeartRate", 1715094300000L); // 7 mai 2024, 15:05
        patient.addRecord(87.0, "HeartRate", 1715094600000L); // 7 mai 2024, 15:10
    }
    
    // Core functionality tests
    
    @Test
    void testGetPatientId() {
        assertEquals(PATIENT_ID, patient.getPatientId(), "Patient ID should match the one set in constructor");
    }
    
    @Test
    void testGetRecordsInTimeRange() {
        // Testăm interval de timp pentru după-amiază între 15:02 și 15:08 (ar trebui să returneze doar înregistrarea de la 15:05)
        List<PatientRecord> records = patient.getRecords(1715094120000L, 1715094480000L); // 15:02 - 15:08
        assertEquals(1, records.size(), "Should return only one record within time range");
        assertEquals(82.0, records.get(0).getMeasurementValue(), "Should return the correct record");
    }
    
    @Test
    void testGetRecordsAllMatching() {
        // Interval care include toate înregistrările
        List<PatientRecord> allRecords = patient.getRecords(1715094000000L, 1715094600000L); // 15:00 - 15:10
        assertEquals(3, allRecords.size(), "Should return all records");
    }
    
    @Test
    void testGetRecordsNoneMatching() {
        // Interval înainte de prima înregistrare
        List<PatientRecord> beforeAll = patient.getRecords(1715090400000L, 1715093900000L); // 14:00 - 14:58
        assertTrue(beforeAll.isEmpty(), "Should return empty list for time before any records");
        
        // Interval după ultima înregistrare
        List<PatientRecord> afterAll = patient.getRecords(1715094700000L, 1715098200000L); // 15:11 - 16:10
        assertTrue(afterAll.isEmpty(), "Should return empty list for time after all records");
    }
    
    // Edge case tests
    
    @Test
    void testExactBoundaryMatching() {
        // Testăm limita exactă pentru prima înregistrare
        List<PatientRecord> exactStart = patient.getRecords(1715094000000L, 1715094000000L); // Exact 15:00
        assertEquals(1, exactStart.size(), "Should return record exactly at boundary");
        assertEquals(78.0, exactStart.get(0).getMeasurementValue(), "Should return correct record");
        
        // Testăm limita exactă pentru ultima înregistrare
        List<PatientRecord> exactEnd = patient.getRecords(1715094600000L, 1715094600000L); // Exact 15:10
        assertEquals(1, exactEnd.size(), "Should return record exactly at boundary");
        assertEquals(87.0, exactEnd.get(0).getMeasurementValue(), "Should return correct record");
    }
    
    @Test
    void testOffByOneTimestamp() {
        // Cu o milisecundă înainte de prima înregistrare
        List<PatientRecord> oneMsBefore = patient.getRecords(1715093999999L, 1715093999999L); // 14:59:59.999
        assertTrue(oneMsBefore.isEmpty(), "Should return empty list when 1ms before record");
        
        // Cu o milisecundă după prima înregistrare
        List<PatientRecord> oneMsAfter = patient.getRecords(1715094000001L, 1715094000001L); // 15:00:00.001
        assertTrue(oneMsAfter.isEmpty(), "Should return empty list when 1ms after record");
    }
    
    @Test
    void testOverlappingTimestamps() {
        // Adăugăm o înregistrare cu același timestamp ca una existentă, dar de tip diferit
        patient.addRecord(120.0, "SystolicBP", 1715094000000L); // 15:00, același timestamp ca prima înregistrare HeartRate
        
        // Ar trebui să returneze două înregistrări la același timestamp
        List<PatientRecord> sameTimestamp = patient.getRecords(1715094000000L, 1715094000000L); // Exact 15:00
        assertEquals(2, sameTimestamp.size(), "Should return multiple records with same timestamp");
        
        // Verificăm că avem ambele tipuri de înregistrări
        boolean hasHeartRate = sameTimestamp.stream().anyMatch(r -> "HeartRate".equals(r.getRecordType()) && Math.abs(r.getMeasurementValue() - 78.0) < 0.001);
        boolean hasSystolic = sameTimestamp.stream().anyMatch(r -> "SystolicBP".equals(r.getRecordType()) && Math.abs(r.getMeasurementValue() - 120.0) < 0.001);
        
        assertTrue(hasHeartRate, "Should have HeartRate record at 15:00");
        assertTrue(hasSystolic, "Should have SystolicBP record at 15:00");
    }
    
    @Test
    void testInvalidTimeRange() {
        // Test cu end time înainte de start time (interval invalid)
        List<PatientRecord> invalidRange = patient.getRecords(1715094600000L, 1715094000000L); // 15:10 - 15:00 (inversat)
        assertTrue(invalidRange.isEmpty(), "Should return empty list when end time is before start time");
    }
    
    @Test
    void testLargeTimeRange() {
        // Test cu un interval foarte mare care include toate înregistrările
        List<PatientRecord> allRecords = patient.getRecords(0L, Long.MAX_VALUE);
        assertEquals(3, allRecords.size(), "Should return all records with very large time range");
    }
}