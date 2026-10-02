import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JoinIdleQueue implements LoadBalancer {

    private final Queue<Server> idleQueue = new ConcurrentLinkedQueue<>();

    private final LoadBalancer fallbackBalancer;

    public JoinIdleQueue() {
        this.fallbackBalancer = new LeastConnections();
    }

    public JoinIdleQueue(LoadBalancer fallbackBalancer) {
        this.fallbackBalancer = fallbackBalancer;
    }

    @Override
    public Server select(List<Server> servers, String taskId, String sessionId) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }

        while (true) {
            Server idleServer = idleQueue.poll();
            if (idleServer == null) {
                break; 
            }
            if (idleServer.isHealthy() && idleServer.getConnections() == 0) {
                return idleServer;
            }
        }

        return fallbackBalancer.select(servers, taskId, sessionId);
    }

    public void notifyIdle(Server server) {
        if (server == null) {
            return;
        }
        if (server.isHealthy() && server.getConnections() == 0) {
            idleQueue.offer(server);
        }
    }

    public void clearQueue() {
        idleQueue.clear();
    }

    public String getFallbackBalancerName() {
        return fallbackBalancer.getClass().getSimpleName();
    }
}