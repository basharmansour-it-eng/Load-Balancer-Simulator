import java.util.Comparator;
import java.util.List;

public class ResourceAware implements LoadBalancer {

    private static final double WEIGHT_CPU    = 0.7;
    private static final double WEIGHT_MEMORY = 0.3;
    private static final double MAX_CPU       = 100.0;
    private static final double MAX_MEMORY    = 100.0;

    @Override
    public Server select(List<Server> servers, String taskId, String sessionId) {
        if (servers == null || servers.isEmpty()) return null;

        return servers.stream()
                .filter(Server::isHealthy)
                .min(Comparator.comparingDouble(this::computeScore)
                        .thenComparingInt(Server::getConnections))
                .orElse(null);
    }

    @Override
    public double computeScore(Server s) {
        double normalizedCpu    = Math.min(s.getCpuUsage()    / MAX_CPU,    1.0);
        double normalizedMemory = Math.min(s.getMemoryUsage() / MAX_MEMORY, 1.0);
        return (normalizedCpu * WEIGHT_CPU) + (normalizedMemory * WEIGHT_MEMORY);
    }
}