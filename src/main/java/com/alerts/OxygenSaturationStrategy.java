package com.alerts;

public class OxygenSaturationStrategy implements AlertStrategy {
    private double threshold;
    
    //constructor
    
    public OxygenSaturationStrategy(double threshold) {
        this.threshold = threshold;
    }
    
    @Override
    public boolean checkAlert(String patientId, double value, long timestamp) {
        //compares the value against the bound
        return value < threshold;
    }
    
    @Override
    public String getConditionDescription(double value) {
        if (value < threshold) {
            return "Low Oxygen Level: " + value + "%";
        }
        return "Normal Oxygen Level: " + value + "%";
    }
}