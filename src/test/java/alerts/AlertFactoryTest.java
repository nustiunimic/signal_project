package alerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.alerts.Alert;
import com.alerts.AlertFactory;
import com.alerts.BloodOxygenAlertFactory;
import com.alerts.BloodPressureAlertFactory;
import com.alerts.ECGAlertFactory;

public class AlertFactoryTest {

    @Test
    void testBloodPressureAlertFactory() {
        //alert factory for blood pressure
        AlertFactory factory = new BloodPressureAlertFactory();
        
        // Alert parameters
        String patientId = "123";
        String condition = "High: 190 mmHg";
        long timestamp = System.currentTimeMillis();
        
        // Create the alert
        Alert alert = factory.createAlert(patientId, condition, timestamp);
        
        // check if it was correctly made
        assertEquals(patientId, alert.getPatientId());
        assertEquals(timestamp, alert.getTimestamp());
        assertEquals(condition, alert.getCondition());
    }
    
    @Test
    void testBloodOxygenAlertFactory() {
        // create alert factory for bloodoxygen
        AlertFactory factory = new BloodOxygenAlertFactory();
        
        // alert parameters
        String patientId = "123";
        String condition = "Low: 85%";
        long timestamp = System.currentTimeMillis();
        
        // Create alert
        Alert alert = factory.createAlert(patientId, condition, timestamp);
        
        // check it was correctly created
        assertEquals(patientId, alert.getPatientId());
        assertEquals(timestamp, alert.getTimestamp());
        assertEquals(condition, alert.getCondition());
    }
    
    @Test
    void testECGAlertFactory() {
        // create factory for ecg alerts
        AlertFactory factory = new ECGAlertFactory();
        
        // Parameters
        String patientId = "123";
        String condition = "Irregular rhythm";
        long timestamp = System.currentTimeMillis();
        
        // Create the alert
        Alert alert = factory.createAlert(patientId, condition, timestamp);
        
        // check for correctness
        assertEquals(patientId, alert.getPatientId());
        assertEquals(timestamp, alert.getTimestamp());
        assertEquals(condition, alert.getCondition());
    }
}
