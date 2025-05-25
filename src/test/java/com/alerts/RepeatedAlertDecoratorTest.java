package com.alerts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RepeatedAlertDecoratorTest {

    
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
            System.out.println("BasicAlert triggered.");

        }

        @Override
        public String getAlertMessage() {
            return "Basic Alert Message";
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
    private RepeatedAlertDecorator decorator;

    @BeforeEach
    void setup() {
        baseAlert = new BasicAlert();
        decorator = new RepeatedAlertDecorator(baseAlert, 3, 100); // max 3, 100ms între ele
    }

    @Test
    void testInitialTrigger() {
        decorator.triggerAlert();
        assertEquals(1, decorator.getCurrentRepeats());
        assertEquals(1, baseAlert.getTriggerCount());
    }

    @Test
    void testTooSoonRepeat() {
        decorator.triggerAlert();
        decorator.triggerAlert(); // imediat după -> nu ar trebui să declanșeze
        assertEquals(1, decorator.getCurrentRepeats());
        assertEquals(1, baseAlert.getTriggerCount());
    }

    @Test
    void testTriggerWithDelays() throws InterruptedException {
        decorator.triggerAlert(); // 1
        Thread.sleep(110);
        decorator.triggerAlert(); // 2
        Thread.sleep(110);
        decorator.triggerAlert(); // 3
        assertEquals(3, decorator.getCurrentRepeats());
        assertEquals(3, baseAlert.getTriggerCount());
        assertTrue(decorator.isCompleted());
    }

    @Test
    void testNoTriggerAfterCompleted() throws InterruptedException {
        decorator.triggerAlert(); // 1
        Thread.sleep(110);
        decorator.triggerAlert(); // 2
        Thread.sleep(110);
        decorator.triggerAlert(); // 3
        Thread.sleep(110);
        decorator.triggerAlert(); // NU trebuie să se mai declanșeze
        assertEquals(3, baseAlert.getTriggerCount());
    }

    @Test
    void testResetRepeats() {
        decorator.triggerAlert();
        decorator.resetRepeats();
        assertEquals(0, decorator.getCurrentRepeats());
        assertFalse(decorator.isCompleted());
    }

    @Test
    void testGetAlertMessageFormat() {
        decorator.triggerAlert();
        String msg = decorator.getAlertMessage();
        assertTrue(msg.contains("[REPEATED: 1/3"));
    }

    @Test
    void testTimeUntilNextRepeat() throws InterruptedException {
        decorator.triggerAlert();
        Thread.sleep(30);
        long time = decorator.getTimeUntilNextRepeat();
        assertTrue(time > 0 && time <= 100);
    }

    @Test
    void testMaxRepeatGetter() {
        assertEquals(3, decorator.getMaxRepeats());
    }
}
