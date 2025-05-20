package com.alerts;

public class ECGAlertFactory extends AlertFactory{

    @Override
    public Alert createAlert(String patientId, String condition, long timestep){
        return new Alert(patientId, condition, timestep);
    }

}
