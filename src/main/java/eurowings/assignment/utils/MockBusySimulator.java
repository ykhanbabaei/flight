package eurowings.assignment.utils;

public class MockBusySimulator {

    private MockBusySimulator() {}

    public static void simulateDelay(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


}
