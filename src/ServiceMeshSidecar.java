import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceMeshSidecar implements LoadBalancer {

    private static final int FAILURE_THRESHOLD = 3;

    private final LoadBalancer innerBalancer;

    private final Map<String, Integer> circuitBreakerFailures = new ConcurrentHashMap<>();

    public ServiceMeshSidecar() {
        this.innerBalancer = new LeastConnections();
    }

    public ServiceMeshSidecar(LoadBalancer innerBalancer) {
        this.innerBalancer = innerBalancer;
    }

    @Override
    public Server select(List<Server> servers, String taskId, String sessionId) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }

        List<Server> availableServers = servers.stream()
                .filter(Server::isHealthy)
                .filter(s -> circuitBreakerFailures.getOrDefault(s.getName(), 0) < FAILURE_THRESHOLD)
                .toList();

        if (!availableServers.isEmpty()) {
            return innerBalancer.select(availableServers, taskId, sessionId);
        }

        return servers.stream()
                .filter(Server::isHealthy)
                .min(Comparator.comparingInt(
                        (Server s) -> circuitBreakerFailures.getOrDefault(s.getName(), 0)))
                .orElse(null);
    }

    public void recordFailure(Server server) {
        if (server != null) {
            circuitBreakerFailures.merge(server.getName(), 1, Integer::sum);
        }
    }

    public void recordSuccess(Server server) {
        if (server != null) {
            circuitBreakerFailures.put(server.getName(), 0);
        }
    }

    public void clearFailures() {
        circuitBreakerFailures.clear();
    }


    public String getInnerBalancerName() {
        return innerBalancer.getClass().getSimpleName();
    }
}