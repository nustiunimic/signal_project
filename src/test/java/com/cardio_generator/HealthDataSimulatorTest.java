package com.cardio_generator;

import com.cardio_generator.outputs.ConsoleOutputStrategy;
import com.cardio_generator.outputs.OutputStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class HealthDataSimulatorTest {

    private HealthDataSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = HealthDataSimulator.getInstance();
        simulator.stopSimulation();
    }

    @AfterEach
    void tearDown() {
        simulator.stopSimulation();
    }

    @Test
    void testSingletonInstance() {
        HealthDataSimulator sim2 = HealthDataSimulator.getInstance();
        assertSame(simulator, sim2);
    }

    @Test
    void testSetAndGetPatientCount() {
        simulator.setPatientCount(100);
        assertEquals(100, simulator.getPatientCount());
    }

    @Test
    void testSetOutputStrategyWhenNotRunning() {
        OutputStrategy strategy = new ConsoleOutputStrategy();
        simulator.setOutputStrategy(strategy);
        assertTrue(true);
    }

    @Test
    void testStartAndStopSimulation() throws IOException {
        assertFalse(simulator.isSimulationRunning());
        simulator.startSimulation(new String[]{"--patient-count", "10", "--output", "console"});
        assertTrue(simulator.isSimulationRunning());
        simulator.stopSimulation();
        assertFalse(simulator.isSimulationRunning());
    }

    @Test
void testInvalidArgumentsHandled() {
    assertThrows(IllegalArgumentException.class, () -> simulator.startSimulation(new String[]{"--unknown"}));
}


    @Test
    void testDoubleStartDoesNotCrash() throws IOException {
        simulator.startSimulation(new String[]{"--patient-count", "5"});
        assertTrue(simulator.isSimulationRunning());
        simulator.startSimulation(new String[]{"--patient-count", "5"});
    }
}
