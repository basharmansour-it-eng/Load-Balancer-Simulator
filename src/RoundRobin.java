import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobin implements LoadBalancer {
   
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public Server select(List<Server> servers, String taskId, String sessionId) {
      int size = servers.size();
      int start = Math.floorMod(index.getAndIncrement(), size);

    for (int i = 0; i < size; i++) {
        Server server = servers.get((start + i) % size);
        if (server.isHealthy()) {
            return server;
      }
    }
    return null;
  }
}