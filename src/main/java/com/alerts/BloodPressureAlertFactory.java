package com.alerts;

public class BloodPressureAlertFactory extends AlertFactory{

    @Override
    public Alert createAlert(String patiendId, String condition, long timestamp){
        return new Alert(patiendId, condition, timestamp);
    }
}
