import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class LoadBalancerSimulator {
    @SuppressWarnings("UseSpecificCatch")
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(SimulatorUI::new);
    }
}