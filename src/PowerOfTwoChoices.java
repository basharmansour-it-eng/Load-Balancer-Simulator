import java.util.List;
import java.util.Random;

public class PowerOfTwoChoices implements LoadBalancer {
    private static final Random RANDOM = new Random();

    @Override
    public Server select(List<Server> servers, String taskId, String sessionId) {
       
        List<Server> healthyServers = servers.stream()
                .filter(Server::isHealthy)
                .toList();

        if (healthyServers.isEmpty()) {
            return null;
        }

        if (healthyServers.size() == 1) {
            return healthyServers.get(0);
        }

        int firstIndex = RANDOM.nextInt(healthyServers.size());
       
        int secondIndex = (firstIndex + 1 + RANDOM.nextInt(healthyServers.size() - 1))
                % healthyServers.size();

        Server first = healthyServers.get(firstIndex);
        Server second = healthyServers.get(secondIndex);

        if (first.getConnections() == second.getConnections()) {
        return RANDOM.nextBoolean() ? first : second;
   }
       return first.getConnections() < second.getConnections() ? first : second;
    }
}