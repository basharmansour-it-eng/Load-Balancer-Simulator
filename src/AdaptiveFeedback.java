import java.util.Comparator;
import java.util.List;

public class AdaptiveFeedback implements LoadBalancer {

    private static final double WEIGHT_LATENCY = 0.5;
    private static final double WEIGHT_CPU = 0.3;
    private static final double WEIGHT_CONNECTIONS = 0.2;

    private static final double MAX_LATENCY_MS = 200.0;
    private static final double MAX_CPU_USAGE = 100.0;
    private static final double MAX_CONNECTIONS = 20.0;

    @Override
    public Server select(List<Server> servers, String taskId, String sessionId) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }

        return servers.stream()
                .filter(Server::isHealthy)
                .min(Comparator.comparingDouble(this::calculatePenalty)
                .thenComparingDouble(s -> Math.random()))
                .orElse(null);
    }

    private double calculatePenalty(Server s) {
        double normLatency = Math.min(s.getLatency() / MAX_LATENCY_MS, 1.0);
        double normCpu = Math.min(s.getCpuUsage() / MAX_CPU_USAGE, 1.0);
        double normConnections = Math.min(s.getConnections() / MAX_CONNECTIONS, 1.0);

        return (normLatency * WEIGHT_LATENCY) +
               (normCpu * WEIGHT_CPU) +
               (normConnections * WEIGHT_CONNECTIONS);
    }
}