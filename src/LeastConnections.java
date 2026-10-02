import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class LeastConnections implements LoadBalancer {

   @Override
   public Server select(List<Server> servers, String taskId, String sessionId) {
   
    List<Server> healthy = servers.stream()
            .filter(Server::isHealthy)
            .collect(Collectors.toList());

    if (healthy.isEmpty()) return null;

    int minConns = healthy.stream()
            .mapToInt(Server::getConnections)
            .min()
            .getAsInt();

    List<Server> candidates = healthy.stream()
            .filter(s -> s.getConnections() == minConns)
            .collect(Collectors.toList());

    return candidates.get(new Random().nextInt(candidates.size()));
  
  }
}