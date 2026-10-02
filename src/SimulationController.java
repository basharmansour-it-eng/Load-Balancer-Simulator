import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SimulationController {
    public static final int MAX_TASKS_PER_RUN = 30;

    private static final int TASK_INTERVAL_MS    = 1000;
    private static final int METRICS_INTERVAL_MS = 2000;
    private static final int PROCESS_MIN_MS      = 2000;
    private static final int PROCESS_MAX_MS      = 4000;
    private static final int CPU_OVERLOAD_LIMIT  = 100;

    private static final Random RANDOM = new Random();

    private final List<Server> servers = Arrays.asList(
            new Server("Server-A", 3, 50),
            new Server("Server-B", 2, 120),
            new Server("Server-C", 1, 30)
    );

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final List<ScheduledFuture<?>> completionFutures = new CopyOnWriteArrayList<>();
    private final Map<String, Server> sessionAffinityMap = new ConcurrentHashMap<>();
    private final SimulationListener listener;

    private ScheduledFuture<?> taskFuture;
    private ScheduledFuture<?> metricsFuture;

    private volatile boolean running = false;
    private LoadBalancer currentBalancer = new RoundRobin();
    private String currentAlgorithm = "Round-Robin";
    private int taskCounter = 0;
    private int tasksGeneratedThisRun = 0;

    public SimulationController(SimulationListener listener) {
        this.listener = listener;
        resetAllServersForNewRun();
    }

    public List<Server> getServers() {
        return servers;
    }

    public boolean isRunning() {
        return running;
    }

    public String getCurrentAlgorithm() {
        return currentAlgorithm;
    }

    public LoadBalancer getCurrentBalancer() {
        return currentBalancer;
    }

    public int getTasksGeneratedThisRun() {
        return tasksGeneratedThisRun;
    }

    public int getActiveConnections() {
        return servers.stream()
                .mapToInt(Server::getConnections)
                .sum();
    }

    public int getHealthyServerCount() {
        return (int) servers.stream()
                .filter(Server::isHealthy)
                .count();
    }

    public synchronized void setAlgorithm(String algorithm) {
        currentAlgorithm = algorithm;

        switch (algorithm) {
            case "Round-Robin"                  -> currentBalancer = new RoundRobin();
            case "Least-Connections"            -> currentBalancer = new LeastConnections();
            case "Power of Two Choices"         -> currentBalancer = new PowerOfTwoChoices();
            case "Health Checks & Dynamic Pool" -> currentBalancer = new HealthCheckDynamicPool(servers);
            case "Weighted Round-Robin"         -> currentBalancer = new WeightedRoundRobin(servers);
            case "Weighted Least Connections"   -> currentBalancer = new WeightedLeastConnections();
            case "Consistent Hashing"           -> currentBalancer = new ConsistentHashing(servers);
            case "Sticky Sessions" -> {
                sessionAffinityMap.clear();
                currentBalancer = new StickySessions(sessionAffinityMap);
            }
            case "Latency-Based Routing" -> currentBalancer = new LatencyBased();
            case "Resource-Aware LB"     -> currentBalancer = new ResourceAware();
            case "Adaptive Feedback"     -> currentBalancer = new AdaptiveFeedback();
            case "Join-Idle-Queue (JIQ)" -> currentBalancer = new JoinIdleQueue();
            case "Service Mesh (Sidecar)" -> {
                ServiceMeshSidecar smc = new ServiceMeshSidecar(new RoundRobin());
                currentBalancer = smc;
                appendLog("Service Mesh: الخوارزمية الداخلية = " + smc.getInnerBalancerName());
            }
            default                      -> currentBalancer = new RoundRobin();
        }

        appendLog("الخوارزمية المفعلة: " + algorithm);
        notifyStateChanged();
    }

    public synchronized void startSimulation() {
        if (running) {
            return;
        }

        cancelCompletionTasks();
        resetAllServersForNewRun();

        if (currentBalancer instanceof JoinIdleQueue jiq) {
            jiq.clearQueue();
        }

        if (currentBalancer instanceof ServiceMeshSidecar smc) {
            smc.clearFailures();
        }

        setAlgorithm(currentAlgorithm);

        taskCounter = 0;
        tasksGeneratedThisRun = 0;
        running = true;

        taskFuture = scheduler.scheduleAtFixedRate(() -> {
            if (running) {
                assignTask();
            }
        }, 0, TASK_INTERVAL_MS, TimeUnit.MILLISECONDS);

        metricsFuture = scheduler.scheduleAtFixedRate(() -> {
            if (running) {
                updateServerMetrics();
            }
        }, METRICS_INTERVAL_MS, METRICS_INTERVAL_MS, TimeUnit.MILLISECONDS);

        appendLog("بدأت المحاكاة. سيتم إنشاء " + MAX_TASKS_PER_RUN + " مهمة فقط.");
        notifyStateChanged();
    }

    public synchronized void stopSimulation() {
        if (!running) {
            return;
        }

        running = false;

        if (taskFuture != null) {
            taskFuture.cancel(false);
        }

        if (metricsFuture != null) {
            metricsFuture.cancel(false);
        }

        cancelCompletionTasks();
        resetServerLoadsOnly();

        appendLog("تم إيقاف المحاكاة وتمت إعادة استهلاك الخوادم إلى 0%.");
        notifyStateChanged();
    }

    public void toggleServerHealth(int serverIndex) {
        if (serverIndex < 0 || serverIndex >= servers.size()) {
            return;
        }

        Server server = servers.get(serverIndex);
        server.manualToggleHealth();
        appendLog("تم تبديل حالة " + server.getName() + " يدوياً إلى "
                + (server.isHealthy() ? "صحي" : "معطل"));
        notifyStateChanged();
    }

    public void shutdown() {
        stopSimulation();
        scheduler.shutdownNow();
    }

    private void assignTask() {
        if (tasksGeneratedThisRun >= MAX_TASKS_PER_RUN) {
            stopSimulation();
            return;
        }

        tasksGeneratedThisRun++;

        String taskId    = "Task-" + (++taskCounter);
        String sessionId = "Session-" + (taskCounter % 5);
        Server target    = currentBalancer.select(servers, taskId, sessionId);

        if (target == null) {
            appendLog("لا توجد خوادم متاحة للمهمة #" + taskCounter);
        } else {
            int cpuIncrease = target.assignConnection();

            appendLog(String.format(
                    "%s [%s] -> %s | زاد CPU بمقدار %d%% | CPU الحالي: %d%% | الاتصالات: %d | التأخير: %dms",
                    taskId, sessionId, target.getName(), cpuIncrease,
                    target.getCpuUsage(), target.getConnections(), target.getLatency()
            ));

            if (target.getCpuUsage() >= CPU_OVERLOAD_LIMIT && target.isHealthy()) {
                target.overloadCrash();
                appendLog(target.getName() + " انهار بسبب الحمل الزائد! (CPU=100%)");
                if (currentBalancer instanceof ServiceMeshSidecar smc) {
                    smc.recordFailure(target);
                }
            }

            int processTime = PROCESS_MIN_MS + RANDOM.nextInt(PROCESS_MAX_MS - PROCESS_MIN_MS + 1);

            ScheduledFuture<?> future = scheduler.schedule(() -> {
                target.completeConnection();
                if (currentBalancer instanceof JoinIdleQueue jiq) {
                    jiq.notifyIdle(target);
                }
                if (currentBalancer instanceof ServiceMeshSidecar smc) {
                    smc.recordSuccess(target);
                }
            }, processTime, TimeUnit.MILLISECONDS);

            completionFutures.add(future);
        }

        if (tasksGeneratedThisRun >= MAX_TASKS_PER_RUN) {
            appendLog("تم إنشاء " + MAX_TASKS_PER_RUN + " مهمة. ستتوقف المحاكاة الآن.");
            stopSimulation();
        }

        notifyStateChanged();
    }

    private void updateServerMetrics() {
        for (Server server : servers) {
            server.updateMetrics();

            int variance = (int) (server.getBaseLatency() * 0.2);
            server.setLatency(server.getBaseLatency()
                    + RANDOM.nextInt(Math.max(1, variance * 2 + 1)) - variance);

            // فحص الـ overload وتسجيل الفشل لـ ServiceMesh
            if (server.getCpuUsage() >= CPU_OVERLOAD_LIMIT && server.isHealthy()) {
                server.overloadCrash();
                appendLog(server.getName() + " انهار بسبب الحمل الزائد! (CPU=100%)");
                if (currentBalancer instanceof ServiceMeshSidecar smc) {
                    smc.recordFailure(server);
                }
            }
        }

        notifyStateChanged();
    }

    private void cancelCompletionTasks() {
        for (ScheduledFuture<?> future : completionFutures) {
            future.cancel(false);
        }
        completionFutures.clear();
    }

    private void resetAllServersForNewRun() {
        for (Server server : servers) {
            server.resetForNewRun();
        }
    }

    private void resetServerLoadsOnly() {
        for (Server server : servers) {
            server.resetLoadOnly();
        }
    }

    private void appendLog(String message) {
        listener.onLog("[" + LocalTime.now().toString().substring(0, 8) + "]  " + message);
    }

    private void notifyStateChanged() {
        listener.onStateChanged();
    }
}