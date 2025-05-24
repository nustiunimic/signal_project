package com.cardio_generator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.cardio_generator.generators.AlertGenerator;

import com.cardio_generator.generators.BloodPressureDataGenerator;
import com.cardio_generator.generators.BloodSaturationDataGenerator;
import com.cardio_generator.generators.BloodLevelsDataGenerator;
import com.cardio_generator.generators.ECGDataGenerator;
import com.cardio_generator.outputs.ConsoleOutputStrategy;
import com.cardio_generator.outputs.FileOutputStrategy;
import com.cardio_generator.outputs.OutputStrategy;
import com.cardio_generator.outputs.TcpOutputStrategy;
import com.cardio_generator.outputs.WebSocketOutputStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Main class for simulating health data for multiple patients.
 * Supports configurable patient count and multiple output strategies including
 * console, file, TCP, and WebSocket.
 * 
 * @author Maria
 */

public class HealthDataSimulator {
    private static HealthDataSimulator instance;
    private static final Object lock = new Object();

    private int patientCount = 50; // Default number of patients
    private ScheduledExecutorService scheduler;
    private OutputStrategy outputStrategy = new ConsoleOutputStrategy(); // Default output strategy
    private final Random random = new Random();

    private boolean isRunning = false;
    private List<Integer> patientIds;

    private HealthDataSimulator(){
        System.out.println("HealthDataSimulator singleton instance created.");
    }

     public static HealthDataSimulator getInstance() {
        // First check (no synchronization) - for performance
        if (instance == null) {
            // Synchronize only when instance is null
            synchronized (lock) {
                // Second check (with synchronization) - for thread safety
                if (instance == null) {
                    instance = new HealthDataSimulator();
                }
            }
        }
        return instance;
    }

    public void startSimulation(String[] args) throws IOException {
        if (isRunning) {
            System.out.println("Simulation is already running!");
            return;
        }

        parseArguments(args);
        
        scheduler = Executors.newScheduledThreadPool(patientCount * 4);
        patientIds = initializePatientIds(patientCount);
        Collections.shuffle(patientIds); // Randomize the order of patient IDs

        scheduleTasksForPatients(patientIds);
        isRunning = true;
        
        System.out.println("HealthDataSimulator started with " + patientCount + " patients");
    }

    public void stopSimulation() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        isRunning = false;
        System.out.println("HealthDataSimulator stopped");
    }

    public boolean isSimulationRunning() {
        return isRunning;
    }
    
    public int getPatientCount() {
        return patientCount;
    }
    
    public void setPatientCount(int count) {
        if (!isRunning) {
            patientCount = count;
            System.out.println("Patient count set to: " + count);
        } else {
            System.out.println("Cannot change patient count while simulation is running!");
        }
    }

    public void setOutputStrategy(OutputStrategy strategy) {
        if (!isRunning) {
            this.outputStrategy = strategy;
            System.out.println("Output strategy updated");
        } else {
            System.out.println("Cannot change output strategy while simulation is running!");
        }
    }


    /**
     * Main entry point for the simulator application.
     * Parses command-line arguments and starts scheduling data generators.
     * 
     * @param args Command-line arguments for configuring the simulator
     * @throws IOException If an output directory needs to be created and fails
     */

    public static void main(String[] args) throws IOException {
        // ✅ SCHIMBAT: Folosim getInstance() pentru a obține singura instanță
        HealthDataSimulator simulator = HealthDataSimulator.getInstance();
        
        // Demonstrăm că este singleton - același obiect
        HealthDataSimulator simulator2 = HealthDataSimulator.getInstance();
        System.out.println("Same instance? " + (simulator == simulator2)); // Should print true
        
        // Start simulation
        simulator.startSimulation(args);
        
        // Add shutdown hook to gracefully stop simulation
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down simulator...");
            simulator.stopSimulation();
        }));
    }

    /**
     * Parses the command-line arguments to configure output strategies and patient count.
     * 
     * @param args The arguments passed via command line
     * @throws IOException If creating a directory for file output fails
     */

    private void parseArguments(String[] args) throws IOException {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                    printHelp();
                    System.exit(0);
                    break;
                case "--patient-count":
                    if (i + 1 < args.length) {
                        try {
                            patientCount = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err
                                    .println("Error: Invalid number of patients. Using default value: " + patientCount);
                        }
                    }
                    break;
                case "--output":
                    if (i + 1 < args.length) {
                        String outputArg = args[++i];
                        if (outputArg.equals("console")) {
                            outputStrategy = new ConsoleOutputStrategy();
                        } else if (outputArg.startsWith("file:")) {
                            String baseDirectory = outputArg.substring(5);
                            Path outputPath = Paths.get(baseDirectory);
                            if (!Files.exists(outputPath)) {
                                Files.createDirectories(outputPath);
                            }
                            outputStrategy = new FileOutputStrategy(baseDirectory);
                        } else if (outputArg.startsWith("websocket:")) {
                            try {
                                int port = Integer.parseInt(outputArg.substring(10));
                                // Initialize your WebSocket output strategy here
                                outputStrategy = new WebSocketOutputStrategy(port);
                                System.out.println("WebSocket output will be on port: " + port);
                            } catch (NumberFormatException e) {
                                System.err.println(
                                        "Invalid port for WebSocket output. Please specify a valid port number.");
                            }
                        } else if (outputArg.startsWith("tcp:")) {
                            try {
                                int port = Integer.parseInt(outputArg.substring(4));
                                // Initialize your TCP socket output strategy here
                                outputStrategy = new TcpOutputStrategy(port);
                                System.out.println("TCP socket output will be on port: " + port);
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid port for TCP output. Please specify a valid port number.");
                            }
                        } else {
                            System.err.println("Unknown output type. Using default (console).");
                        }
                    }
                    break;
                default:
                    System.err.println("Unknown option '" + args[i] + "'");
                    printHelp();
                    System.exit(1);
            }
        }
    }
    /**
     * Displays help instructions for running the simulator.
     */

    private static void printHelp() {
        System.out.println("Usage: java HealthDataSimulator [options]");
        System.out.println("Options:");
        System.out.println("  -h                       Show help and exit.");
        System.out.println(
                "  --patient-count <count>  Specify the number of patients to simulate data for (default: 50).");
        System.out.println("  --output <type>          Define the output method. Options are:");
        System.out.println("                             'console' for console output,");
        System.out.println("                             'file:<directory>' for file output,");
        System.out.println("                             'websocket:<port>' for WebSocket output,");
        System.out.println("                             'tcp:<port>' for TCP socket output.");
        System.out.println("Example:");
        System.out.println("  java HealthDataSimulator --patient-count 100 --output websocket:8080");
        System.out.println(
                "  This command simulates data for 100 patients and sends the output to WebSocket clients connected to port 8080.");
    }

    /**
     * Generates and returns a list of patients IDs from 1 to patientCount.
     * 
     * @param patientCount Number of patients to generate IDs for
     * @return List of patient IDs
     */

    private  List<Integer> initializePatientIds(int patientCount) {
        List<Integer> patientIds = new ArrayList<>();
        for (int i = 1; i <= patientCount; i++) {
            patientIds.add(i);
        }
        return patientIds;
    }

    /**
     * Schedules tasks for each patient to generate various health data.
     * 
     * @param patientIds The list of patient IDs to schedule generators for 
     */

    private void scheduleTasksForPatients(List<Integer> patientIds) {
        ECGDataGenerator ecgDataGenerator = new ECGDataGenerator(patientCount);
        BloodSaturationDataGenerator bloodSaturationDataGenerator = new BloodSaturationDataGenerator(patientCount);
        BloodPressureDataGenerator bloodPressureDataGenerator = new BloodPressureDataGenerator(patientCount);
        BloodLevelsDataGenerator bloodLevelsDataGenerator = new BloodLevelsDataGenerator(patientCount);
        AlertGenerator alertGenerator = new AlertGenerator(patientCount);

        for (int patientId : patientIds) {
            scheduleTask(() -> ecgDataGenerator.generate(patientId, outputStrategy), 1, TimeUnit.SECONDS);
            scheduleTask(() -> bloodSaturationDataGenerator.generate(patientId, outputStrategy), 1, TimeUnit.SECONDS);
            scheduleTask(() -> bloodPressureDataGenerator.generate(patientId, outputStrategy), 1, TimeUnit.MINUTES);
            scheduleTask(() -> bloodLevelsDataGenerator.generate(patientId, outputStrategy), 2, TimeUnit.MINUTES);
            scheduleTask(() -> alertGenerator.generate(patientId, outputStrategy), 20, TimeUnit.SECONDS);
        }
    }
/**
 * Schedules a reccuring task with a random intial delay.
 * 
 * @param task The task to be scheduled
 * @param period The period between successive executions
 * @param timeUnit The time unit of the period
 */
    private void scheduleTask(Runnable task, long period, TimeUnit timeUnit) {
        scheduler.scheduleAtFixedRate(task, random.nextInt(5), period, timeUnit);
    }
}
