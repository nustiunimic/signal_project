package data_management;

import com.data_management.DataReader;
import com.data_management.DataStorage;
import com.data_management.FileDataReader;
import com.data_management.PatientRecord;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class DataReaderTest {

    private Path tempFilePath;
    private DataStorage dataStorage;
    private DataReader dataReader;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException{
        /*simulating output in format of HealthDataSimulator:
        *patientId,value,type,timestamp
        */
        
        tempFilePath= tempDir.resolve("simulator_output.txt");
        Files.write(tempFilePath, List.of(
            
                "1001,98.0,Saturation,1714376000000",
                "1001,85.0,HeartRate,1714376060000",
                "1001,190.0,SystolicBP,1714376120000"
        ));

        dataStorage =new DataStorage();
        dataReader =new FileDataReader(tempFilePath.toString());
    }

    @Test
    void testReadDataStoresCorrectNumberOfRecords() throws IOException{
        dataReader.readData(dataStorage);
        List<PatientRecord> records = dataStorage.getRecords(1001,1714375999000L, 1714376121000L );
    assertEquals(3, records.size(), "Should store 3 records for patient 1001");
    }
    
    @Test 
    /* testing the normal functionaity of this class */
    void testReadDataParsesCorrectValues() throws IOException {
        dataReader.readData(dataStorage);
        List<PatientRecord> records = dataStorage.getRecords(1001, 1714375999000L, 1714376121000L);
        
        PatientRecord systolic = records.stream()
                .filter(r -> "SystolicBP".equals(r.getRecordType()))
                .findFirst()
                .orElse(null);
                
        assertNotNull(systolic);
        assertEquals(190.0, systolic.getMeasurementValue(), 0.01);
        assertEquals(1714376120000L, systolic.getTimestamp());
    }
    
    /* testing edge cases */


    @Test
    void testReadDataWithInvalidFormat(@TempDir Path tempDir) throws IOException {
        Path invalidFile = tempDir.resolve("invalid.txt");
        Files.write(invalidFile, List.of(
                "1001,98.0,Saturation", // Missing timestamp
                "invalid line",         // Completely invalid
                "1001,190.0,SystolicBP,1714376120000" // Valid line
        ));
        
        DataStorage testStorage = new DataStorage();
        DataReader invalidReader = new FileDataReader(invalidFile.toString());
        invalidReader.readData(testStorage);
        
        List<PatientRecord> records = testStorage.getRecords(1001, 0L, Long.MAX_VALUE);
        assertEquals(1, records.size(), "Only one valid record should be added");
    }
    
    @Test
    void testReadDataWithEmptyFile(@TempDir Path tempDir) throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.write(emptyFile, List.of());
        
        DataStorage testStorage = new DataStorage();
        DataReader emptyReader = new FileDataReader(emptyFile.toString());
        emptyReader.readData(testStorage);
        
        List<PatientRecord> records = testStorage.getRecords(1001, 0L, Long.MAX_VALUE);
        assertEquals(0, records.size(), "No records should be added from empty file");
    }
    
    @Test
    void testReadDataWithNonExistentFile() {
        DataReader nonExistentReader = new FileDataReader("nonexistent.txt");
        
        assertThrows(IOException.class, () -> {
            nonExistentReader.readData(dataStorage);
        }, "Should throw IOException for non-existent file");
    }
    

}
