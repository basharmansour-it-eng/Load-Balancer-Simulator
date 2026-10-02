import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final Random RANDOM = new Random();

    private final String name;
    private final AtomicInteger connections = new AtomicInteger(0);
    private final AtomicInteger totalHandled = new AtomicInteger(0);
    private final AtomicInteger cpuUsage = new AtomicInteger(0);
    private final AtomicInteger memoryUsage = new AtomicInteger(0);

    private volatile boolean healthy = true;
    private volatile boolean manualOff = false;

    private final int weight;
    private final int baseLatency;
    private volatile int latency;

    public Server(String name, int weight, int baseLatency) {
        this.name = name;
        this.weight = weight;
        this.baseLatency = baseLatency;
        this.latency = baseLatency;
    }

    public String getName() {
        return name;
    }

    public int getConnections() {
        return connections.get();
    }

    public int getTotalHandled() {
        return totalHandled.get();
    }

    public boolean isHealthy() {
        return healthy;
    }

    public int getWeight() {
        return weight;
    }

    public int getLatency() {
        return latency;
    }

    public int getBaseLatency() {
        return baseLatency;
    }

    public int getCpuUsage() {
        return cpuUsage.get();
    }

    public int getMemoryUsage() {
        return memoryUsage.get();
    }

    public int assignConnection() {
        connections.incrementAndGet();
        totalHandled.incrementAndGet();

        int increase = 15 + RANDOM.nextInt(6);
        cpuUsage.updateAndGet(v -> Math.min(100, v + increase));
        memoryUsage.updateAndGet(v -> Math.min(100, v + 2 + RANDOM.nextInt(4)));

        return increase;
    }

    public void completeConnection() {
        int current = connections.decrementAndGet();
        if (current < 0) {
            connections.set(0);
        }

        cpuUsage.updateAndGet(v -> Math.max(0, v - (6 + RANDOM.nextInt(5))));
        memoryUsage.updateAndGet(v -> Math.max(0, v - (1 + RANDOM.nextInt(4))));
    }

    public void manualToggleHealth() {
        manualOff = !manualOff;
        healthy = !manualOff;
    }

    public void overloadCrash() {
        healthy = false;
    }

    public void updateMetrics() {
        memoryUsage.updateAndGet(v -> Math.max(0, Math.min(100, v + RANDOM.nextInt(3) - 1)));
    }

    public void resetForNewRun() {
        connections.set(0);
        totalHandled.set(0);
        cpuUsage.set(0);
        memoryUsage.set(0);
        healthy = true;
        manualOff = false;
        latency = baseLatency;
    }

    public void resetLoadOnly() {
        connections.set(0);
        cpuUsage.set(0);
        memoryUsage.set(0);
        latency = baseLatency;
    }

    public void setLatency(int latency) {
        this.latency = latency;
    }
}