package com.alerts;

public class BloodPressureStrategy implements AlertStrategy{

    private double lowerThreshold;
    private double upperThreshold;
    private String type;

    //constructor for bloodpressurestrategy

    public BloodPressureStrategy(double lowerThreshold, double upperThreshold, String type){
        this.lowerThreshold=lowerThreshold;
        this.upperThreshold=upperThreshold;
        this.type=type;
    }

    @Override
    public boolean checkAlert(String patientId, double value, long timestamp){
        return value < lowerThreshold || value > upperThreshold;
    }

    @Override
    public String getConditionDescription(double value){
        if (value < lowerThreshold){
            return "Critical Low "+ type + " BP: "+value+ " mmHg";
        }else if (value> upperThreshold){
            return "Critical "+ type + " BP: "+value+ "mmHg";
         
        }
        return "Normal "+ type + " BP: "+value+ "mmHg";
        

    }
}
