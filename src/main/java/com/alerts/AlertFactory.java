package com.alerts;

//abstract to use abstract methods of the originial ALert class
public abstract class AlertFactory {

  public abstract Alert createAlert(String patientId, String condition, long timestamp);

    
}


