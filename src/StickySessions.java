import java.util.List;
import java.util.Map;

public class StickySessions implements LoadBalancer {
    private final Map<String, Server> sessionMap;
    private final RoundRobin fallback = new RoundRobin();

    public StickySessions(Map<String, Server> sessionMap) {
        this.sessionMap = sessionMap;
    }

    @Override
    public Server select(List<Server> servers, String taskId, String sessionId) {
        if (sessionId == null) {
            return fallback.select(servers, taskId, null);
        }

        Server assigned = sessionMap.get(sessionId);
        if (assigned != null && assigned.isHealthy()) {
            return assigned;
        }

        List<Server> healthyServers = servers.stream()
                .filter(Server::isHealthy)
                .toList();

        if (healthyServers.isEmpty()) {
            return null;
        }

        Server chosen = healthyServers.get(Math.floorMod(sessionId.hashCode(), healthyServers.size()));
        sessionMap.put(sessionId, chosen);

        return chosen;
    }
}