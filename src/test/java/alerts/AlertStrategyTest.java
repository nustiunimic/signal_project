package alerts;

import com.alerts.AlertStrategy;
import com.alerts.BloodPressureStrategy;
import com.alerts.HeartRateStrategy;
import com.alerts.OxygenSaturationStrategy;
import com.alerts.AlertGenerator;


import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;



public class AlertStrategyTest {
    
    @Test
    void testBloodPressureStrategy() {
        // create strategy
        AlertStrategy systolicStrategy = new BloodPressureStrategy(90, 180, "Systolic");
        AlertStrategy diastolicStrategy = new BloodPressureStrategy(60, 120, "Diastolic");
        
        String patientId = "123";
        long timestamp = System.currentTimeMillis();
        
        // we test the values that should activate the mechansim
        assertTrue(systolicStrategy.checkAlert(patientId, 190, timestamp), "Should detect high systolic BP");
        assertTrue(systolicStrategy.checkAlert(patientId, 85, timestamp), "Should detect low systolic BP");
        assertTrue(diastolicStrategy.checkAlert(patientId, 130, timestamp), "Should detect high diastolic BP");
        assertTrue(diastolicStrategy.checkAlert(patientId, 55, timestamp), "Should detect low diastolic BP");
        
        // we test the values that shouldn't do anything
        assertFalse(systolicStrategy.checkAlert(patientId, 120, timestamp), "Should not trigger for normal systolic BP");
        assertFalse(diastolicStrategy.checkAlert(patientId, 80, timestamp), "Should not trigger for normal diastolic BP");
        
        // we check the generated descriptions to match 
        assertEquals("Critical Systolic BP: 190.0mmHg", systolicStrategy.getConditionDescription(190.0));
        assertEquals("Critical Low Systolic BP: 85.0 mmHg", systolicStrategy.getConditionDescription(85.0));
        assertEquals("Critical Diastolic BP: 130.0mmHg", diastolicStrategy.getConditionDescription(130.0));
        assertEquals("Critical Low Diastolic BP: 55.0 mmHg", diastolicStrategy.getConditionDescription(55.0));
    }
    
    @Test
    void testHeartRateStrategy() {
        AlertStrategy heartRateStrategy = new HeartRateStrategy(120);
        
        String patientId = "123";
        long timestamp = System.currentTimeMillis();
        
        // Verifies the high heart rate
        assertTrue(heartRateStrategy.checkAlert(patientId, 130, timestamp), "Should detect high heart rate");
        assertFalse(heartRateStrategy.checkAlert(patientId, 80, timestamp), "Should not trigger for normal heart rate");
        
        // Verifies the outputs
        assertEquals("High Heart Rate: 130.0 bpm", heartRateStrategy.getConditionDescription(130.0));
        assertEquals("Normal Heart Rate: 80.0 bpm", heartRateStrategy.getConditionDescription(80.0));
    }
    
    @Test
    void testOxygenSaturationStrategy() {
        AlertStrategy oxygenStrategy = new OxygenSaturationStrategy(90);
        
        String patientId = "123";
        long timestamp = System.currentTimeMillis();
        
        // Verifies the low oxygen 
        assertTrue(oxygenStrategy.checkAlert(patientId, 85, timestamp), "Should detect low oxygen");
        assertFalse(oxygenStrategy.checkAlert(patientId, 95, timestamp), "Should not trigger for normal oxygen");
        
        // Verifies the description
        assertEquals("Low Oxygen Level: 85.0%", oxygenStrategy.getConditionDescription(85.0));
        assertEquals("Normal Oxygen Level: 95.0%", oxygenStrategy.getConditionDescription(95.0));
    }
    
    @Test
    void testChangingStrategy() {
        // we test the change of the strategy
        AlertGenerator alertGenerator = new AlertGenerator(new TestDataStorage());
        
        // check that the initial strategy has the 90 threshold
        AlertStrategy initialStrategy = new BloodPressureStrategy(90, 180, "Systolic");
        assertTrue(initialStrategy.checkAlert("123", 85, 0), "Initial strategy should detect BP of 85");
        
        // change the strategy for an 80 threshold
        AlertStrategy newStrategy = new BloodPressureStrategy(80, 180, "Systolic");
        alertGenerator.setStrategy("SystolicBloodPressure", newStrategy);
        
        // verify the new strategy against others
        assertFalse(newStrategy.checkAlert("123", 85, 0), "New strategy should not detect BP of 85");
    }
    
    // helper class
    private static class TestDataStorage extends com.data_management.DataStorage {
        @Override
        public java.util.List<com.data_management.PatientRecord> getRecords(int patientId, long startTime, long endTime) {
            return new java.util.ArrayList<>();
        }
    }
}
