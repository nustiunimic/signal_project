package data_management;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.util.List;

class DataStorageTest {
private DataStorage storage;

    @BeforeEach
    void setup() {
        storage = DataStorage.getInstance();
        storage.clearAllData(); // asigurăm testele izolate
    }

    @Test
    void testAddAndRetrieveSingleRecord() {
        storage.addPatientData(42, 120.5, "HeartRate", 1714376789050L);

        List<PatientRecord> records = storage.getRecords(42, 1714376789040L, 1714376789060L);
        assertEquals(1, records.size());
        PatientRecord record = records.get(0);
        assertEquals(120.5, record.getMeasurementValue());
        assertEquals("HeartRate", record.getRecordType());
        assertEquals(42, record.getPatientId());
    }

    @Test
    void testMultiplePatients() {
        storage.addPatientData(1, 95.0, "Oxygen", 1714376789050L);
        storage.addPatientData(2, 80.0, "Oxygen", 1714376789050L);

        assertTrue(storage.hasPatient(1));
        assertTrue(storage.hasPatient(2));
        assertFalse(storage.hasPatient(3));
        assertEquals(2, storage.getPatientCount());
    }

    @Test
    void testGetPatientObject() {
        storage.addPatientData(100, 37.2, "Temperature", 1714376789050L);
        Patient patient = storage.getPatient(100);
        assertNotNull(patient);
        assertEquals(100, patient.getId());
    }

    @Test
    void testClearAllData() {
        storage.addPatientData(55, 140.0, "BloodPressure", 1714376789050L);
        assertTrue(storage.hasPatient(55));

        storage.clearAllData();
        assertFalse(storage.hasPatient(55));
        assertEquals(0, storage.getPatientCount());
    }

    @Test
    void testGetAllPatientsReturnsCorrectSize() {
        storage.addPatientData(1, 98.0, "Oxygen", 1714376789050L);
        storage.addPatientData(2, 120.0, "HeartRate", 1714376789050L);

        List<Patient> allPatients = storage.getAllPatients();
        assertEquals(2, allPatients.size());
    }
}
