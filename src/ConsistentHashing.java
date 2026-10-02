import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashing implements LoadBalancer {
    private static final int VIRTUAL_NODES = 10;

    private final TreeMap<Long, Server> ring = new TreeMap<>();

    public ConsistentHashing(List<Server> servers) {
        for (Server server : servers) {
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                ring.put(hash(server.getName() + "#" + i), server);
            }
        }
    }

    @Override
    public Server select(List<Server> servers, String taskId, String sessionId) {
        if (ring.isEmpty()) {
            return null;
        }

        long hash = hash(taskId != null ? taskId : String.valueOf(System.nanoTime()));
        SortedMap<Long, Server> tail = ring.tailMap(hash);
        Server server = tail.isEmpty()
                ? ring.get(ring.firstKey())
                : tail.get(tail.firstKey());

        if (server.isHealthy()) {
            return server;
        }

        return servers.stream()
                .filter(Server::isHealthy)
                .findFirst()
                .orElse(null);
    }

    private long hash(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest(key.getBytes(StandardCharsets.UTF_8));

            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }

            return hash;
        } catch (NoSuchAlgorithmException e) {
            return key.hashCode();
        }
    }
}