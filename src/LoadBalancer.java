import java.util.List;

public interface LoadBalancer {
    Server select(List<Server> servers, String taskId, String sessionId);

    default double computeScore(Server server) {
        return -1.0; // الخوارزميات التي لا تدعم Score تُعيد -1
    }
}