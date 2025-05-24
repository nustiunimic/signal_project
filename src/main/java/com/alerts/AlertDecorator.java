package com.alerts;

public abstract class AlertDecorator implements AlertInterface{
    protected AlertInterface wrappedAlert;

    public AlertDecorator(AlertInterface alert){
        this.wrappedAlert = alert;
    }

    @Override
    public String getPatientId(){
        return wrappedAlert.getPatientId();
    }

    @Override
    public String getCondition(){
        return wrappedAlert.getCondition();
    }

    @Override
    public long getTimestamp(){
        return wrappedAlert.getTimestamp();
    }

    @Override
    public void triggerAlert(){
        wrappedAlert.triggerAlert();
    }

    @Override
    public String getAlertMessage(){
        return wrappedAlert.getAlertMessage();
    }



}
