import java.util.Comparator;
import java.util.List;

public  class LatencyBased implements LoadBalancer {
        @Override
        public Server select(List<Server> servers, String taskId, String sessionId) {
            return servers.stream()
                    .filter(Server::isHealthy)
                    .min(
                   Comparator.comparingInt(Server::getLatency)
                   .thenComparingInt(Server::getConnections)
)
                    .orElse(null);
        }
    }