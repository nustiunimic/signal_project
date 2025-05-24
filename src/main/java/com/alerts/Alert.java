package com.alerts;

// Represents an alert
public class Alert implements AlertInterface {
    private String patientId;
    private String condition;
    private long timestamp;

    public Alert(String patientId, String condition, long timestamp) {
        this.patientId = patientId;
        this.condition = condition;
        this.timestamp = timestamp;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getCondition() {
        return condition;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void triggerAlert() {
        // TODO Auto-generated method stub
        System.out.println("Alert triggered!!");
        System.out.println("getAlertMessage()");
        System.out.println("Alert processing completed.");
    }

    @Override
    public String getAlertMessage() {
        // TODO Auto-generated method stub
        return String.format ("Patient %s: %s (Time: %d)", patientId, condition, timestamp);  
      }
}
