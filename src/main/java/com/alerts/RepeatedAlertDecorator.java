package com.alerts;

public class RepeatedAlertDecorator extends AlertDecorator {

    private int repeatCount;
    private long repeatInterval;
    private int currentRepeats;
    private long lastTriggerTime;

    public RepeatedAlertDecorator(AlertInterface alert, int repeatCount, long repeatInterval){
        super(alert);

        this.repeatCount = repeatCount;
        this.repeatInterval = repeatInterval;
        this.currentRepeats = 0;
        this.lastTriggerTime = 0;
    }

    public void triggerAlert() {
        long currentTime = System.currentTimeMillis();

        if(currentRepeats == 0){
            System.out.println("=== REPEATED ALERT : Initial trigger ===");
            wrappedAlert.triggerAlert();
            currentRepeats++;
            lastTriggerTime = currentTime;

            if(repeatCount > 1){
                System.out.println("Will repeat" + (repeatCount - 1) + "more times every" + repeatInterval + "ms");

            }
        }

        else if (currentRepeats < repeatCount) {
            long timeSinceLastTrigger = currentTime - lastTriggerTime;
            
            if(timeSinceLastTrigger >=  repeatInterval){
                System.out.println("=== REPEATED ALERT : Repeat #" + currentRepeats + "===");
                wrappedAlert.triggerAlert();
                currentRepeats++;
                lastTriggerTime = currentTime;

                if(currentRepeats < repeatCount){
                    System.out.println((repeatCount - currentRepeats) + " repeats remaining");
                }
                } else {
                    long timeRemaining = repeatInterval - timeSinceLastTrigger;
                System.out.println("REPEATED ALERT: Too soon! Wait " + timeRemaining + "ms more");
                }
            }
            else {
                System.out.println("REPEATED ALERT: All " + repeatCount + " repeats completed");
            }
        }
         @Override
        public String getAlertMessage() {
            return wrappedAlert.getAlertMessage() + 
               String.format(" [REPEATED: %d/%d times]", currentRepeats, repeatCount);
    }
        public int getCurrentRepeats() {
        return currentRepeats;
    }
        public int getMaxRepeats() {
        return repeatCount;
    }
        public void resetRepeats() {
        this.currentRepeats = 0;
        this.lastTriggerTime = 0;
        System.out.println("Repeat counter has been reset");
    }
        public boolean isCompleted() {
        return currentRepeats >= repeatCount;
    }
        public long getTimeUntilNextRepeat() {
        if (currentRepeats == 0 || currentRepeats >= repeatCount) {
            return 0;
        }
        
        long timeSinceLastTrigger = System.currentTimeMillis() - lastTriggerTime;
        return Math.max(0, repeatInterval - timeSinceLastTrigger);
    }

}

