package alerts;

import com.alerts.Alert;
import com.alerts.AlertGenerator;
import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlertGeneratorTest {
    private InMemoryDataStorage dataStorage;
    private AlertGenerator alertGenerator;
    private long now;

    @BeforeEach
    void setUp() {
        dataStorage    = new InMemoryDataStorage();
        alertGenerator = new AlertGenerator(dataStorage);
        now            = System.currentTimeMillis();
    }

    /**
     * Tests the generation of a blood pressure trend alert
     * Simulates three consecutive systolic blood pressure measurements, each increasing by at least 10 mmHg
     */

    @Test
    void testEvaluateData_BloodPressureTrendAlert() {
        // Three systolic readings, each rising by at least 10 mmHg
        dataStorage.addPatientData(1, 110, "SystolicBloodPressure", now - 30_000);
        dataStorage.addPatientData(1, 125, "SystolicBloodPressure", now - 20_000);
        dataStorage.addPatientData(1, 140, "SystolicBloodPressure", now - 10_000);

        alertGenerator.evaluateData(new Patient(1));

        List<Alert> alerts = alertGenerator.getTriggeredAlerts();
        assertEquals(1, alerts.size(), "Expect one trend alert");
        assertEquals("Trend Alert: Systolic Blood Pressure",
                     alerts.get(0).getCondition());
    }

    /**
     * Tests the generation of a critical blood pressure threshold alert
     * Simulates a single systolic blood pressure measurement that exceeds the dangerous threshold
     */

    @Test
    void testEvaluateData_BloodPressureCriticalThresholdAlert() {
        // A single dangerously high systolic reading
        dataStorage.addPatientData(2, 185, "SystolicBloodPressure", now - 10_000);

        alertGenerator.evaluateData(new Patient(2));

        List<Alert> alerts = alertGenerator.getTriggeredAlerts();
        assertEquals(1, alerts.size(), "Expect one critical-threshold alert");
        assertEquals("Critical Systolic BP: 185.0mmHg",
                     alerts.get(0).getCondition());
    }

    /**
     * Tests the generation of a low blood oxygen level alert
     * Simulates an oxygen measurement below the 90% threshold
     */

    @Test
    void testEvaluateData_BloodSaturationLowAlert() {
        // Oxygen saturation has fallen below 90%
        dataStorage.addPatientData(3, 89, "OxygenLevel", now - 10_000);

        alertGenerator.evaluateData(new Patient(3));

        List<Alert> alerts = alertGenerator.getTriggeredAlerts();
        assertEquals(1, alerts.size(),
                     "Expected one alert for low blood saturation.");
        assertEquals("Low Oxygen Level: 89.0%",
                     alerts.get(0).getCondition());
    }

    /**
     * Tests the generation of a hypotensive hypoxemia alert
     * Simulates measurements of low blood pressure and low oxygen level within the same time interval
     */

    @Test
    void testEvaluateData_HypotensiveHypoxemiaAlert() {
        // Low blood pressure and low oxygen within the same hour
        dataStorage.addPatientData(4, 88, "SystolicBloodPressure", now - 20_000);
        dataStorage.addPatientData(4, 91, "OxygenLevel",now - 10_000);

        alertGenerator.evaluateData(new Patient(4));

        List<Alert> alerts = alertGenerator.getTriggeredAlerts();
        // We expect two alerts: one for critical BP and one combined Hypotensive Hypoxemia
        assertEquals(2, alerts.size(), "Expect two alerts: critical BP + combined alert");
        assertTrue(alerts.stream()
                         .anyMatch(a -> a.getCondition()
                                        .equals("Hypotensive Hypoxemia Alert")),
                   "Should include a Hypotensive Hypoxemia Alert");
    }

    /**
     * Tests the generation of an abnormal ECG activity alert
     * Simulates ECG measurements with a sudden spike exceeding 50% of the moving average
     */

    @Test
    void testEvaluateData_ECGAbnormalDataAlert() {
        // ECG readings with a sudden spike beyond 50% of the moving average
        dataStorage.addPatientData(5, 1.0, "ECG", now - 30_000);
        dataStorage.addPatientData(5, 1.5, "ECG", now - 20_000);
        dataStorage.addPatientData(5, 3.0, "ECG", now - 10_000);

        alertGenerator.evaluateData(new Patient(5));

        List<Alert> alerts = alertGenerator.getTriggeredAlerts();
        assertEquals(1, alerts.size(), "Expect one ECG anomaly alert");
        assertEquals("Abnormal ECG Activity Detected",
                     alerts.get(0).getCondition());
    }

    // In‑memory DataStorage for testing, holds raw PatientRecord entries
 
    static class InMemoryDataStorage extends DataStorage {
        private final List<PatientRecord> records = new ArrayList<>();

        public void addPatientData(int patientId,
                                   double measurementValue,
                                   String recordType,
                                   long timestamp) {
            records.add(new PatientRecord(
                patientId, measurementValue, recordType, timestamp));
        }

        @Override
        public List<PatientRecord> getRecords(int patientId,
                                              long startTime,
                                              long endTime) {
            List<PatientRecord> result = new ArrayList<>();
            for (PatientRecord r : records) {
                if (r.getPatientId() == patientId
                 && r.getTimestamp()  >= startTime
                 && r.getTimestamp()  <= endTime) {
                    result.add(r);
                }
            }
            return result;
        }
    }
}
