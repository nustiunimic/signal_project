package com.alerts;

public class PriorityAlertDecorator extends AlertDecorator {
    private String priorityLevel;   
    private String priorityReason;  

    public PriorityAlertDecorator(AlertInterface alert, String priorityLevel, String priorityReason) {
        super(alert);
        this.priorityLevel = priorityLevel.toUpperCase();
        this.priorityReason = priorityReason;
    }

    @Override
    public void triggerAlert() {
        System.out.println("=== PRIORITY ALERT ACTIVATED ===");
        System.out.println(" Priority Level: " + priorityLevel);
        System.out.println(" Priority Reason: " + priorityReason);
        
        switch (priorityLevel) {
            case "CRITICAL":
                System.out.println(" Critical priority: Initiating emergency protocols!");
                System.out.println(" Critical priority: Notifying emergency contacts!");
                System.out.println(" Critical priority: Alerting medical team!");
                break;
                
            case "HIGH":
                System.out.println(" High priority: Escalating to supervisor!");
                System.out.println(" High priority: Sending urgent notifications!");
                break;
                
            case "MEDIUM":
                System.out.println(" Medium priority: Adding to review queue!");
                break;
                
            case "LOW":
                System.out.println(" Low priority: Logging for routine review!");
                break;
                
            default:
                System.out.println("Unknown priority: Using standard protocols!");
        }
        
        System.out.println("--- Original Alert Processing ---");
        
        wrappedAlert.triggerAlert();
        
        System.out.println("--- Priority Alert Post-Processing ---");
        
        if ("CRITICAL".equals(priorityLevel)) {
            System.out.println(" Critical: Emergency response initiated!");
        } else if ("HIGH".equals(priorityLevel)) {
            System.out.println("High: Escalation completed!");
        }
        
        System.out.println(" Priority alert processing completed.");
    }
    
    @Override
    public String getAlertMessage() {
        return wrappedAlert.getAlertMessage() + 
               String.format(" [PRIORITY: %s - %s]", priorityLevel, priorityReason);
    }

    public String getPriorityLevel() {
        return priorityLevel;
    }
    
    public String getPriorityReason() {
        return priorityReason;
    }
    
    public void updatePriority(String newLevel, String newReason) {
        String oldLevel = this.priorityLevel;
        this.priorityLevel = newLevel.toUpperCase();
        this.priorityReason = newReason;
        
        System.out.println(" Priority updated: " + oldLevel + " -> " + this.priorityLevel);
        System.out.println(" New reason: " + newReason);
    }
    
    public boolean isCritical() {
        return "CRITICAL".equals(priorityLevel);
    }
    
    public boolean isHighPriority() {
        return "HIGH".equals(priorityLevel) || "CRITICAL".equals(priorityLevel);
    }
}