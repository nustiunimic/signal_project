package com.alerts;

public class HeartRateStrategy implements AlertStrategy {

     private double maxRate;
    
    //constructor HeartRateStrategy

    public HeartRateStrategy(double maxRate) {
        this.maxRate = maxRate;
    }
    
    @Override
    public boolean checkAlert(String patientId, double value, long timestamp) {
        //checks the value against the limit
        return value > maxRate;
    }
    
    @Override
    public String getConditionDescription(double value) {
        if (value > maxRate) {
            return "High Heart Rate: " + value + " bpm";
        }
        return "Normal Heart Rate: " + value + " bpm";
    }

}
