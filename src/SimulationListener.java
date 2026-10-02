public interface SimulationListener {
    void onLog(String message);

    void onStateChanged();
}