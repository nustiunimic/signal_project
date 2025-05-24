package com.data_management;

import java.io.IOException;

public interface DataReader {
    /**
     * Starts reading data continuously and updates the data storage in real time.
     * 
     * @param dataStorage the storage where data will be stored as it is received
     */
    void startReading(DataStorage dataStorage);
}
