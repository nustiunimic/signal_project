package com.alerts;

public interface AlertStrategy {

    //decides if an alert should be generated based on the measured value
    boolean checkAlert(String patientId, double value, long timestamp);

    //gets the triggering of the alarm
    String getConditionDescription(double value);
}
