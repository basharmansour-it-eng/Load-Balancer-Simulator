import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthCheckDynamicPool implements LoadBalancer {

    private final Set<Server> activePool = ConcurrentHashMap.newKeySet();

    private final ScheduledExecutorService healthChecker =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "HealthChecker-Thread");
                t.setDaemon(true); 
                return t;
            });

    private static final int CHECK_INTERVAL_SECONDS = 5;

    public HealthCheckDynamicPool(List<Server> servers) {
        servers.stream()
                .filter(Server::isHealthy)
                .forEach(activePool::add);

        healthChecker.scheduleAtFixedRate(
                () -> runHealthCheck(servers),
                CHECK_INTERVAL_SECONDS,
                CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void runHealthCheck(List<Server> servers) {
        for (Server server : servers) {
            if (server.isHealthy()) {
                boolean added = activePool.add(server);
                if (added) {
                    System.out.printf("[HealthCheck] %s عاد للعمل → أُضيف للـ Pool%n",
                            server.getName());
                }
            } else {
                boolean removed = activePool.remove(server);
                if (removed) {
                    System.out.printf("[HealthCheck] %s معطوب → حُذف من الـ Pool%n",
                            server.getName());
                }
            }
        }

        System.out.printf("[HealthCheck] Pool الحالي: %d خادم نشط%n",
                activePool.size());
    }

    @Override
    public Server select(List<Server> servers, String taskId, String sessionId) {

        // الـ Pool فارغ تماماً — كل الخوادم معطوبة
        if (activePool.isEmpty()) {
            System.out.println("[HealthCheck] تحذير: الـ Pool فارغ، لا يوجد خوادم متاحة");
            return null;
        }

        return activePool.stream()
                .min(Comparator.comparingInt(Server::getConnections))
                .orElse(null);
    }

    public void shutdown() {
        healthChecker.shutdownNow();
    }
}