import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WeightedRoundRobin implements LoadBalancer {

    private final List<Server> weightedServers = new ArrayList<>();
    private final AtomicInteger index = new AtomicInteger(0);

    public WeightedRoundRobin(List<Server> servers) {
        for (Server server : servers) {
            for (int i = 0; i < server.getWeight(); i++) {
                weightedServers.add(server);
            }
        }
    }

    @Override
    public Server select(List<Server> servers, String taskId, String sessionId) {
    if (weightedServers.isEmpty()) return null;

    int size  = weightedServers.size();
    int start = Math.floorMod(index.getAndIncrement(), size); 

    for (int i = 0; i < size; i++) {
        Server server = weightedServers.get((start + i) % size);
        if (server.isHealthy()) return server;
    }
    return null; 
  }
}