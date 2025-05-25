package com.data_management;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileDataReader implements DataReader {

    private final String filePath;

    public FileDataReader(String filePath){
        this.filePath = filePath;
    }

    @Override
    public void startReading(DataStorage dataStorage){
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))){
            String line;

            while((line = reader.readLine()) != null){
                String[] parts = line.split(",");

                if(parts.length !=4) continue;

                int patientId = Integer.parseInt(parts[0]);
                double value = Double.parseDouble(parts[1]);
                String type = parts[2];
                long timestamp = Long.parseLong(parts[3]);
                
                dataStorage.addPatientData(patientId, value, type, timestamp);

            }
        
        }
        catch (IOException e){
            System.err.println("Error reading file :" + e.getMessage());
            e.printStackTrace();
        }
    }
}
