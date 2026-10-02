import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;

public class SimulatorUI extends JFrame implements SimulationListener {
    private static final int MAX_LOG_LINES = 150;

    private static final Color APP_BG      = new Color(13, 18, 32);
    private static final Color PANEL_BG    = new Color(24, 31, 50);
    private static final Color PANEL_SOFT  = new Color(31, 40, 64);
    private static final Color TEXT_MAIN   = new Color(245, 247, 250);
    private static final Color TEXT_MUTED  = new Color(166, 176, 198);
    private static final Color GREEN       = new Color(52, 211, 153);
    private static final Color RED         = new Color(248, 113, 113);
    private static final Color YELLOW      = new Color(251, 191, 36);
    private static final Color BLUE        = new Color(96, 165, 250);

    private static final String[] ALGORITHMS = {
            "Round-Robin", "Least-Connections", "Power of Two Choices",
            "Health Checks & Dynamic Pool", "Weighted Round-Robin",
            "Weighted Least Connections", "Consistent Hashing", "Sticky Sessions",
            "Latency-Based Routing", "Resource-Aware LB",
            "Adaptive Feedback", "Join-Idle-Queue (JIQ)", "Service Mesh (Sidecar)"
    };

    private final SimulationController controller = new SimulationController(this);
    private final JComboBox<String> algorithmSelector = new JComboBox<>(ALGORITHMS);
    private final JButton controlButton = new JButton("بدء المحاكاة");
    private final JTextArea logArea = new JTextArea(10, 60);
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private final Timer uiTimer;

    private final JLabel[] connectionLabels = new JLabel[3];
    private final JLabel[] healthLabels     = new JLabel[3];
    private final JLabel[] metricsLabels    = new JLabel[3];
    private final JLabel[] totalLabels      = new JLabel[3];
    private final JLabel[] scoreLabels      = new JLabel[3]; // ← جديد
    private final JProgressBar[] cpuBars    = new JProgressBar[3];

    private final JLabel totalTasksValue       = new JLabel("0 / " + SimulationController.MAX_TASKS_PER_RUN);
    private final JLabel activeConnectionsValue = new JLabel("0");
    private final JLabel healthyServersValue   = new JLabel("3 / 3");
    private final JLabel activeAlgorithmValue  = new JLabel("Round-Robin");

    public SimulatorUI() {
        super("محاكي خوارزميات موازنة الحمل");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.shutdown();
                dispose();
                
                System.exit(0);
            }
        });

        setupLookAndFeel();

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(APP_BG);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        root.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setContentPane(root);

        uiTimer = new Timer(350, e -> updateUIFromController());

        setupHeader(root);
        setupServerPanels(root);
        setupLogPanel(root);
        updateUIFromController();
        uiTimer.start();

        pack();
        setMinimumSize(new Dimension(960, 650));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public void onLog(String message) {
        logQueue.add(message);
        SwingUtilities.invokeLater(this::updateUIFromController);
    }

    @Override
    public void onStateChanged() {
        SwingUtilities.invokeLater(this::updateUIFromController);
    }

    private void setupLookAndFeel() {
        javax.swing.UIManager.put("ComboBox.background",          PANEL_SOFT);
        javax.swing.UIManager.put("ComboBox.foreground",          TEXT_MAIN);
        javax.swing.UIManager.put("ComboBox.selectionBackground", BLUE);
        javax.swing.UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        javax.swing.UIManager.put("Button.font",  new Font("Tahoma", Font.BOLD,  13));
        javax.swing.UIManager.put("Label.font",   new Font("Tahoma", Font.PLAIN, 13));
    }

    private void setupHeader(JPanel root) {
        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.setOpaque(false);
        header.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel titlePanel = new JPanel(new BorderLayout(4, 4));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("محاكي موازنة الحمل");
        title.setForeground(TEXT_MAIN);
        title.setFont(new Font("Tahoma", Font.BOLD, 24));
        title.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel subtitle = new JLabel("راقب توزيع الطلبات، صحة الخوادم، الاتصالات النشطة، واستهلاك الموارد لحظة بلحظة");
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setFont(new Font("Tahoma", Font.PLAIN, 13));
        subtitle.setHorizontalAlignment(SwingConstants.RIGHT);

        titlePanel.add(title,    BorderLayout.NORTH);
        titlePanel.add(subtitle, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setOpaque(false);
        controls.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JLabel algorithmLabel = new JLabel("الخوارزمية");
        algorithmLabel.setForeground(TEXT_MUTED);
        algorithmLabel.setFont(new Font("Tahoma", Font.BOLD, 12));

        algorithmSelector.setFont(new Font("Tahoma", Font.PLAIN, 13));
        algorithmSelector.setPreferredSize(new Dimension(260, 34));
        algorithmSelector.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        styleButton(controlButton, GREEN, Color.WHITE);
        controlButton.setPreferredSize(new Dimension(155, 36));

        controls.add(algorithmLabel);
        controls.add(algorithmSelector);
        controls.add(controlButton);

        JPanel summary = new JPanel(new GridLayout(1, 4, 10, 0));
        summary.setOpaque(false);
        summary.add(createSummaryCard("مهام هذه الجولة",     totalTasksValue,        BLUE));
        summary.add(createSummaryCard("الاتصالات النشطة",    activeConnectionsValue, YELLOW));
        summary.add(createSummaryCard("الخوادم الصحية",      healthyServersValue,    GREEN));
        summary.add(createSummaryCard("الخوارزمية الحالية",  activeAlgorithmValue,   new Color(196, 181, 253)));

        JPanel topLine = new JPanel(new BorderLayout(12, 12));
        topLine.setOpaque(false);
        topLine.add(titlePanel, BorderLayout.CENTER);
        topLine.add(controls,   BorderLayout.WEST);

        header.add(topLine, BorderLayout.NORTH);
        header.add(summary, BorderLayout.CENTER);

        algorithmSelector.addActionListener(e ->
                controller.setAlgorithm((String) algorithmSelector.getSelectedItem()));

        controlButton.addActionListener(e -> {
            if (controller.isRunning()) {
                controller.stopSimulation();
            } else {
                controller.startSimulation();
            }
        });

        root.add(header, BorderLayout.NORTH);
    }

    private JPanel createSummaryCard(String title, JLabel value, Color accent) {
        JPanel card = new RoundedPanel(16, PANEL_BG);
        card.setLayout(new BorderLayout(6, 4));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        card.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT_MUTED);
        titleLabel.setFont(new Font("Tahoma", Font.BOLD, 12));

        value.setForeground(accent);
        value.setFont(new Font("Tahoma", Font.BOLD, 20));
        value.setHorizontalAlignment(SwingConstants.RIGHT);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(value,      BorderLayout.CENTER);

        return card;
    }

    private void setupServerPanels(JPanel root) {
        JPanel center = new JPanel(new GridLayout(1, 3, 12, 0));
        center.setOpaque(false);

        Color[] accentColors = {
                new Color(96, 165, 250),
                new Color(196, 181, 253),
                new Color(52, 211, 153)
        };

        for (int i = 0; i < controller.getServers().size(); i++) {
            Server server = controller.getServers().get(i);
            JPanel card = createServerCard(server, i, accentColors[i]);
            center.add(card);
        }

        root.add(center, BorderLayout.CENTER);
    }

    private JPanel createServerCard(Server server, int index, Color accentColor) {
        JPanel card = new RoundedPanel(18, PANEL_BG);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(54, 66, 96), 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));
        card.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // ── Header: اسم الخادم + حالته ──────────────────────────────────
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JLabel nameLabel = new JLabel(server.getName());
        nameLabel.setFont(new Font("Tahoma", Font.BOLD, 18));
        nameLabel.setForeground(accentColor);
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        healthLabels[index] = createStatusBadge("صحي", GREEN);

        top.add(nameLabel,           BorderLayout.CENTER);
        top.add(healthLabels[index], BorderLayout.WEST);

        // ── Body: المقاييس ───────────────────────────────────────────────
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        connectionLabels[index] = createMetricValue("0");
        totalLabels[index]      = createMetricValue("0");

        body.add(createMetricRow("الاتصالات النشطة", connectionLabels[index]));
        body.add(Box.createVerticalStrut(8));
        body.add(createMetricRow("إجمالي المهام", totalLabels[index]));
        body.add(Box.createVerticalStrut(14));

        // شريط CPU
        JLabel cpuTitle = new JLabel("استخدام المعالج");
        cpuTitle.setForeground(TEXT_MUTED);
        cpuTitle.setFont(new Font("Tahoma", Font.BOLD, 12));
        cpuTitle.setAlignmentX(Component.RIGHT_ALIGNMENT);
        body.add(cpuTitle);
        body.add(Box.createVerticalStrut(6));

        cpuBars[index] = createCpuBar();
        body.add(cpuBars[index]);
        body.add(Box.createVerticalStrut(14));

        // الذاكرة / التأخير / الوزن
        metricsLabels[index] = new JLabel();
        metricsLabels[index].setForeground(TEXT_MUTED);
        metricsLabels[index].setFont(new Font("Tahoma", Font.PLAIN, 12));
        metricsLabels[index].setHorizontalAlignment(SwingConstants.RIGHT);
        metricsLabels[index].setAlignmentX(Component.RIGHT_ALIGNMENT);
        body.add(metricsLabels[index]);

        // ── Score (جديد) ─────────────────────────────────────────────────
        body.add(Box.createVerticalStrut(8));

        // فاصل بصري خفيف
        JPanel divider = new JPanel();
        divider.setOpaque(true);
        divider.setBackground(new Color(54, 66, 96));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(1, 1));
        body.add(divider);

        body.add(Box.createVerticalStrut(8));

        scoreLabels[index] = new JLabel("Score: —");
        scoreLabels[index].setForeground(TEXT_MUTED);
        scoreLabels[index].setFont(new Font("Tahoma", Font.BOLD, 13));
        scoreLabels[index].setHorizontalAlignment(SwingConstants.RIGHT);
        scoreLabels[index].setAlignmentX(Component.RIGHT_ALIGNMENT);
        body.add(scoreLabels[index]);

        // ── Footer: زر التبديل ───────────────────────────────────────────
        JButton toggleButton = new JButton("تبديل الحالة يدوياً");
        styleButton(toggleButton, new Color(205, 213, 226), Color.BLACK);
        toggleButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        toggleButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        toggleButton.addActionListener(e -> controller.toggleServerHealth(index));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(toggleButton, BorderLayout.CENTER);

        card.add(top,    BorderLayout.NORTH);
        card.add(body,   BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private JProgressBar createCpuBar() {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(0);
        bar.setStringPainted(true);
        bar.setFont(new Font("Tahoma", Font.BOLD, 11));
        bar.setForeground(BLUE);
        bar.setBackground(new Color(47, 57, 84));
        bar.setBorderPainted(false);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        bar.setPreferredSize(new Dimension(180, 24));
        bar.setUI(new BasicProgressBarUI() {
            @Override protected Color getSelectionBackground() { return Color.WHITE; }
            @Override protected Color getSelectionForeground() { return Color.WHITE; }
        });
        return bar;
    }

    private JPanel createMetricRow(String title, JLabel value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT_MUTED);
        titleLabel.setFont(new Font("Tahoma", Font.PLAIN, 12));
        titleLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(titleLabel, BorderLayout.CENTER);
        row.add(value,      BorderLayout.WEST);

        return row;
    }

    private JLabel createMetricValue(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_MAIN);
        label.setFont(new Font("Tahoma", Font.BOLD, 17));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private JLabel createStatusBadge(String text, Color color) {
        JLabel badge = new JLabel(text, SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setForeground(Color.WHITE);
        badge.setBackground(color);
        badge.setFont(new Font("Tahoma", Font.BOLD, 12));
        badge.setBorder(BorderFactory.createEmptyBorder(4, 12, 5, 12));
        return badge;
    }

    private void setupLogPanel(JPanel root) {
        logArea.setEditable(false);
        logArea.setFont(new Font("Tahoma", Font.PLAIN, 13));
        logArea.setBackground(new Color(9, 13, 24));
        logArea.setForeground(new Color(134, 239, 172));
        logArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        logArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(64, 76, 110), 1),
                "سجل الأحداث",
                TitledBorder.RIGHT,
                TitledBorder.TOP,
                new Font("Tahoma", Font.BOLD, 13),
                TEXT_MUTED
        ));
        scroll.setPreferredSize(new Dimension(900, 210));
        scroll.getViewport().setBackground(new Color(9, 13, 24));

        root.add(scroll, BorderLayout.SOUTH);
    }

    private void styleButton(JButton button, Color background, Color foreground) {
        button.setFont(new Font("Tahoma", Font.BOLD, 13));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(7, 14, 8, 14));
    }

    private void updateUIFromController() {
        drainLogQueue();

        boolean running = controller.isRunning();
        controlButton.setText(running ? "إيقاف المحاكاة" : "بدء المحاكاة");
        controlButton.setBackground(running ? RED : GREEN);
        controlButton.setForeground(Color.BLACK);

        int healthyCount      = controller.getHealthyServerCount();
        int activeConnections = controller.getActiveConnections();

        LoadBalancer balancer = controller.getCurrentBalancer();

        for (int i = 0; i < controller.getServers().size(); i++) {
            Server server = controller.getServers().get(i);

            connectionLabels[i].setText(String.valueOf(server.getConnections()));
            totalLabels[i].setText(String.valueOf(server.getTotalHandled()));

            int cpu = server.getCpuUsage();
            cpuBars[i].setValue(cpu);
            cpuBars[i].setString(cpu + "%");
            cpuBars[i].setForeground(cpuColor(cpu));

            metricsLabels[i].setText(String.format(
                    "الذاكرة: %d%%   |   التأخير: %dms   |   الوزن: %d",
                    server.getMemoryUsage(), server.getLatency(), server.getWeight()
            ));

            // ── تحديث Score ──────────────────────────────────────────────
            double score = balancer.computeScore(server);
            if (score < 0) {
                scoreLabels[i].setText("Score: —");
                scoreLabels[i].setForeground(TEXT_MUTED);
            } else {
                scoreLabels[i].setText(String.format("Score: %.2f", score));
                scoreLabels[i].setForeground(scoreColor(score));
            }

            // ── حالة الصحة ───────────────────────────────────────────────
            if (server.isHealthy()) {
                healthLabels[i].setText("صحي");
                healthLabels[i].setBackground(GREEN);
            } else {
                healthLabels[i].setText("معطل");
                healthLabels[i].setBackground(RED);
            }
        }

        totalTasksValue.setText(controller.getTasksGeneratedThisRun()
                + " / " + SimulationController.MAX_TASKS_PER_RUN);
        activeConnectionsValue.setText(String.valueOf(activeConnections));
        healthyServersValue.setText(healthyCount + " / " + controller.getServers().size());
        activeAlgorithmValue.setText(controller.getCurrentAlgorithm());
    }

    private void drainLogQueue() {
        String log;
        StringBuilder builder = new StringBuilder();

        while ((log = logQueue.poll()) != null) {
            builder.append(log).append("\n");
        }

        if (builder.length() == 0) return;

        logArea.append(builder.toString());

        String[] lines = logArea.getText().split("\n");
        if (lines.length > MAX_LOG_LINES + 50) {
            logArea.setText(String.join("\n",
                    Arrays.copyOfRange(lines, lines.length - MAX_LOG_LINES, lines.length)) + "\n");
        }

        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // ── ألوان CPU ─────────────────────────────────────────────────────────
    private Color cpuColor(int cpu) {
        if (cpu >= 90) return RED;
        if (cpu >= 70) return YELLOW;
        return BLUE;
    }

    // ── ألوان Score ───────────────────────────────────────────────────────
    private Color scoreColor(double score) {
        if (score >= 0.7) return RED;    // حمل عالٍ
        if (score >= 0.4) return YELLOW; // حمل متوسط
        return GREEN;                    // حمل منخفض
    }

    // ── RoundedPanel ─────────────────────────────────────────────────────
    static class RoundedPanel extends JPanel {
        private final int   radius;
        private final Color background;

        RoundedPanel(int radius, Color background) {
            this.radius     = radius;
            this.background = background;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}