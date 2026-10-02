import java.util.Comparator;
import java.util.List;

public class WeightedLeastConnections implements LoadBalancer {
   
    @Override
    public Server select(List<Server> servers, String taskId, String sessionId) {
        return servers.stream()
                .filter(Server::isHealthy)
                .min(Comparator.comparingDouble(server ->
                        (double) server.getConnections() / Math.max(1, server.getWeight())))
                .orElse(null);
    }
}