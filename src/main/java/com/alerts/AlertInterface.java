package com.alerts;

public interface AlertInterface {
    String getPatientId();
    String getCondition();
    void triggerAlert();
    String getAlertMessage();
    long getTimestamp();
}
