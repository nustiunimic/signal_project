package com.alerts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PriorityAlertDecoratorTest {

    static class BasicAlert implements AlertInterface {
        private int triggerCount = 0;

        @Override
        public String getPatientId() {
            return "patient-001";
        }

        @Override
        public String getCondition() {
            return "high blood pressure";
        }

        @Override
        public void triggerAlert() {
            triggerCount++;
        }

        @Override
        public String getAlertMessage() {
            return "Base Alert";
        }

        @Override
        public long getTimestamp() {
            return System.currentTimeMillis();
        }

        public int getTriggerCount() {
            return triggerCount;
        }
    }

    private BasicAlert baseAlert;
    private PriorityAlertDecorator decorator;

    @BeforeEach
    void setup() {
        baseAlert = new BasicAlert();
        decorator = new PriorityAlertDecorator(baseAlert, "critical", "Heart rate too high");
    }

    @Test
    void testTriggerAlertIncrementsWrappedAlert() {
        assertEquals(0, baseAlert.getTriggerCount());
        decorator.triggerAlert();
        assertEquals(1, baseAlert.getTriggerCount());
    }

    @Test
    void testAlertMessageFormat() {
        String message = decorator.getAlertMessage();
        assertTrue(message.contains("Base Alert"));
        assertTrue(message.contains("[PRIORITY: CRITICAL - Heart rate too high]"));
    }

    @Test
    void testIsCriticalAndHighPriority() {
        assertTrue(decorator.isCritical());
        assertTrue(decorator.isHighPriority());
    }

    @Test
    void testUpdatePriority() {
        decorator.updatePriority("medium", "Stable condition");
        assertEquals("MEDIUM", decorator.getPriorityLevel());
        assertEquals("Stable condition", decorator.getPriorityReason());
        assertFalse(decorator.isCritical());
        assertFalse(decorator.isHighPriority());
    }

    @Test
    void testNonCriticalHighPriorityLevel() {
        PriorityAlertDecorator highPriority = new PriorityAlertDecorator(baseAlert, "high", "High BP");
        assertFalse(highPriority.isCritical());
        assertTrue(highPriority.isHighPriority());
    }

    @Test
    void testLowPriority() {
        PriorityAlertDecorator lowPriority = new PriorityAlertDecorator(baseAlert, "low", "Minor issue");
        assertFalse(lowPriority.isCritical());
        assertFalse(lowPriority.isHighPriority());
    }
}
