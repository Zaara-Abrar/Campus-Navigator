import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Styled frontend (GUI) for the Smart Campus Navigation System.
 * It does NOT repeat any logic: every answer comes from the backend in CampusNavigator.java
 * (graph, Dijkstra, BFS, DFS, Trie, Merge Sort, Stack).
 *
 * Keep this file in the same folder as CampusNavigator.java and run THIS file's main().
 */
public class CampusGUI extends JFrame {

    // ======================= COLOURS & FONT (change these to restyle everything) =======================
    static final String FONT = "Segoe UI";
    static final Color BG = new Color(241, 245, 249);          // window background
    static final Color BORDER = new Color(226, 232, 240);      // card borders
    static final Color TEXT = new Color(30, 41, 59);           // main text
    static final Color MUTED = new Color(100, 116, 139);       // secondary text
    static final Color PRIMARY = new Color(37, 99, 235);       // main button colour
    static final Color PRIMARY_HOVER = new Color(29, 78, 216);
    static final Color GREY = new Color(100, 116, 139);        // Clear button
    static final Color GREY_HOVER = new Color(71, 85, 105);
    static final Color NODE = new Color(59, 130, 246);         // normal location
    static final Color ROUTE = new Color(249, 115, 22);        // route / visited
    static final Color START = new Color(34, 197, 94);         // route start
    static final Color END = new Color(239, 68, 68);           // route destination
    static final Color HEADER_LEFT = new Color(30, 58, 138);   // header gradient
    static final Color HEADER_RIGHT = new Color(79, 70, 229);

    private final JComboBox<String> fromBox;
    private final JComboBox<String> toBox;
    private final JTextField searchField = new JTextField();
    private final DefaultListModel<String> suggestionModel = new DefaultListModel<>();
    private final JList<String> suggestionList = new JList<>(suggestionModel);
    private final JTextArea output = new JTextArea();
    private final MapPanel mapPanel = new MapPanel();

    public CampusGUI() {
        super("Campus Navigation System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBackground(BG);
        content.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        add(content, BorderLayout.CENTER);

        // ---------- TOP: choose From / To ----------
        String[] names = CampusNavigator.graph.names.toArray(new String[0]);
        fromBox = styledCombo(names);
        toBox = styledCombo(names);
        toBox.setSelectedIndex(names.length - 1);

        JPanel selector = card(null);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        row.add(label("From"));
        row.add(fromBox);
        row.add(label("To"));
        row.add(toBox);
        selector.add(row, BorderLayout.CENTER);
        content.add(selector, BorderLayout.NORTH);

        // ---------- LEFT: search with autocomplete (Trie) ----------
        JPanel searchCard = card("Search location (Trie)");
        searchCard.setPreferredSize(new Dimension(220, 0));
        searchField.setFont(new Font(FONT, Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        suggestionList.setFont(new Font(FONT, Font.PLAIN, 14));
        suggestionList.setFixedCellHeight(28);
        suggestionList.setSelectionBackground(new Color(219, 234, 254));
        suggestionList.setSelectionForeground(TEXT);
        JScrollPane listScroll = new JScrollPane(suggestionList);
        listScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        JPanel searchInner = new JPanel(new BorderLayout(0, 8));
        searchInner.setOpaque(false);
        searchInner.add(searchField, BorderLayout.NORTH);
        searchInner.add(listScroll, BorderLayout.CENTER);
        searchCard.add(searchInner, BorderLayout.CENTER);
        content.add(searchCard, BorderLayout.WEST);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateSuggestions(); }
            public void removeUpdate(DocumentEvent e) { updateSuggestions(); }
            public void changedUpdate(DocumentEvent e) { updateSuggestions(); }
        });
        suggestionList.addListSelectionListener(e -> {
            String picked = suggestionList.getSelectedValue();
            if (!e.getValueIsAdjusting() && picked != null) toBox.setSelectedItem(picked);
        });
        updateSuggestions();

        // ---------- CENTER: campus map ----------
        content.add(mapPanel, BorderLayout.CENTER);

        // ---------- BOTTOM: buttons + result ----------
        JPanel buttons = new JPanel(new GridLayout(2, 3, 10, 10));
        buttons.setOpaque(false);
        buttons.add(button("Shortest Route (Dijkstra)", PRIMARY, PRIMARY_HOVER, this::shortestRoute));
        buttons.add(button("Fewest Stops (BFS)", PRIMARY, PRIMARY_HOVER, this::fewestStops));
        buttons.add(button("Explore Campus (DFS)", PRIMARY, PRIMARY_HOVER, this::exploreCampus));
        buttons.add(button("Nearest to Farthest (Merge Sort)", PRIMARY, PRIMARY_HOVER, this::nearestToFarthest));
        buttons.add(button("Recent Searches (Stack)", PRIMARY, PRIMARY_HOVER, this::recentSearches));
        buttons.add(button("Clear", GREY, GREY_HOVER, () -> { mapPanel.clear(); output.setText(""); }));

        output.setEditable(false);
        output.setFont(new Font("Consolas", Font.PLAIN, 15));
        output.setForeground(TEXT);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setMargin(new Insets(4, 4, 4, 4));
        JScrollPane outScroll = new JScrollPane(output);
        outScroll.setBorder(null);
        JPanel resultCard = card("Result");
        resultCard.add(outScroll, BorderLayout.CENTER);
        resultCard.setPreferredSize(new Dimension(0, 170));

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(resultCard, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        setSize(1150, 780);
        setMinimumSize(new Dimension(950, 650));
        setLocationRelativeTo(null);
    }

    // ======================= STYLING HELPERS =======================

    private JPanel buildHeader() {
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setPaint(new GradientPaint(0, 0, HEADER_LEFT, getWidth(), 0, HEADER_RIGHT));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("Campus Navigation System");
        title.setFont(new Font(FONT, Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Find the shortest way around your college campus");
        sub.setFont(new Font(FONT, Font.PLAIN, 14));
        sub.setForeground(new Color(199, 210, 254));
        header.add(title);
        header.add(Box.createVerticalStrut(2));
        header.add(sub);
        return header;
    }

    private static JPanel card(String title) {
        JPanel p = new JPanel(new BorderLayout(6, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        if (title != null) {
            JLabel l = new JLabel(title);
            l.setFont(new Font(FONT, Font.BOLD, 13));
            l.setForeground(MUTED);
            p.add(l, BorderLayout.NORTH);
        }
        return p;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, Font.BOLD, 14));
        l.setForeground(TEXT);
        return l;
    }

    private static JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(new Font(FONT, Font.PLAIN, 14));
        c.setPreferredSize(new Dimension(190, 32));
        c.setBackground(Color.WHITE);
        return c;
    }

    private StyledButton button(String text, Color base, Color hover, Runnable action) {
        StyledButton b = new StyledButton(text, base, hover);
        b.addActionListener(e -> action.run());
        return b;
    }

    /** Rounded button with a hover colour. */
    static class StyledButton extends JButton {
        private final Color base, hover;
        private boolean over = false;

        StyledButton(String text, Color base, Color hover) {
            super(text);
            this.base = base;
            this.hover = hover;
            setFont(new Font(FONT, Font.BOLD, 13));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(220, 40));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { over = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getModel().isPressed() ? hover.darker() : over ? hover : base);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g.dispose();
            super.paintComponent(g0);       // draws the button text
        }
    }

    // ======================= BUTTON ACTIONS =======================

    private void shortestRoute() {
        int src = fromBox.getSelectedIndex(), dest = toBox.getSelectedIndex();
        if (src == dest) { sameLocation(); return; }
        remember(src);
        remember(dest);

        CampusNavigator.PathResult r = CampusNavigator.graph.dijkstra(src);
        List<Integer> path = CampusNavigator.graph.buildPath(r.parent, src, dest);
        if (path.isEmpty()) { mapPanel.clear(); show("No route found."); return; }

        mapPanel.showRoute(path);
        show("SHORTEST ROUTE (Dijkstra)\n" + describe(path)
                + "\nTotal distance: " + r.dist[dest] + " m (about "
                + Math.round(r.dist[dest] / 80.0) + " min walking)");
    }

    private void fewestStops() {
        int src = fromBox.getSelectedIndex(), dest = toBox.getSelectedIndex();
        if (src == dest) { sameLocation(); return; }
        remember(src);
        remember(dest);

        int[] parent = CampusNavigator.graph.bfs(src);
        List<Integer> path = CampusNavigator.graph.buildPath(parent, src, dest);
        if (path.isEmpty()) { mapPanel.clear(); show("No route found."); return; }

        mapPanel.showRoute(path);
        show("FEWEST STOPS ROUTE (BFS)\n" + describe(path)
                + "\nStops: " + (path.size() - 1) + " hops, distance: "
                + CampusNavigator.graph.pathLength(path) + " m");
    }

    private void exploreCampus() {
        int src = fromBox.getSelectedIndex();
        remember(src);

        List<String> order = new ArrayList<>();
        CampusNavigator.graph.dfs(src, new boolean[CampusNavigator.graph.size()], order);

        mapPanel.showOrder(toIds(order));
        show("CAMPUS TOUR from " + CampusNavigator.graph.names.get(src) + " (DFS)\n"
                + String.join(" -> ", order));
    }

    private void nearestToFarthest() {
        int src = fromBox.getSelectedIndex();
        remember(src);

        CampusNavigator.PathResult r = CampusNavigator.graph.dijkstra(src);
        List<CampusNavigator.Entry> list = new ArrayList<>();
        for (int i = 0; i < CampusNavigator.graph.size(); i++) {
            if (i != src && r.dist[i] != CampusNavigator.INF) {
                list.add(new CampusNavigator.Entry(CampusNavigator.graph.names.get(i), r.dist[i]));
            }
        }
        CampusNavigator.Entry[] arr = list.toArray(new CampusNavigator.Entry[0]);
        CampusNavigator.mergeSort(arr, 0, arr.length - 1);

        StringBuilder sb = new StringBuilder("NEAREST TO FARTHEST from "
                + CampusNavigator.graph.names.get(src) + " (Merge Sort)\n");
        List<String> orderedNames = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            sb.append(String.format("%2d. %-16s %d m%n", i + 1, arr[i].name, arr[i].distance));
            orderedNames.add(arr[i].name);
        }
        mapPanel.showOrder(toIds(orderedNames));
        show(sb.toString());
    }

    private void recentSearches() {
        if (CampusNavigator.recent.isEmpty()) { show("No recent searches yet."); return; }
        show("RECENT SEARCHES (Stack, latest first)\n" + String.join(", ", CampusNavigator.recent));
    }

    // ======================= SMALL HELPERS =======================

    private void updateSuggestions() {
        suggestionModel.clear();
        for (String s : CampusNavigator.trie.startsWith(searchField.getText().trim())) {
            suggestionModel.addElement(s);
        }
    }

    private void remember(int id) {                       // Stack push, keep last 10
        CampusNavigator.recent.push(CampusNavigator.graph.names.get(id));
        if (CampusNavigator.recent.size() > 10) CampusNavigator.recent.removeLast();
    }

    private String describe(List<Integer> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append(" -> ");
            sb.append(CampusNavigator.graph.names.get(path.get(i)));
        }
        return sb.toString();
    }

    private List<Integer> toIds(List<String> names) {
        List<Integer> ids = new ArrayList<>();
        for (String n : names) ids.add(CampusNavigator.graph.find(n));
        return ids;
    }

    private void sameLocation() {
        mapPanel.clear();
        show("Start and destination are the same place. Please choose two different locations.");
    }

    private void show(String text) { output.setText(text); output.setCaretPosition(0); }

    // ======================= MAP DRAWING =======================

    static class MapPanel extends JPanel {
        private List<Integer> highlighted = new ArrayList<>();
        private boolean routeMode = false;      // true: route (orange path, green start, red end)

        // x, y between 0 and 1 (left to right, top to bottom) for each sample location
        private static final Map<String, double[]> PRESET = new HashMap<>();
        static {
            PRESET.put("main gate", new double[]{0.00, 0.50});
            PRESET.put("admin block", new double[]{0.22, 0.38});
            PRESET.put("medical room", new double[]{0.22, 0.06});
            PRESET.put("library", new double[]{0.45, 0.20});
            PRESET.put("cse department", new double[]{0.47, 0.52});
            PRESET.put("ai-ml lab", new double[]{0.25, 0.70});
            PRESET.put("parking", new double[]{0.04, 0.88});
            PRESET.put("canteen", new double[]{0.54, 0.88});
            PRESET.put("hostel", new double[]{0.80, 0.94});
            PRESET.put("sports ground", new double[]{0.95, 0.64});
            PRESET.put("auditorium", new double[]{0.95, 0.26});
            PRESET.put("seminar hall", new double[]{0.72, 0.10});
        }

        MapPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(BORDER, 1));
            setPreferredSize(new Dimension(700, 450));
        }

        void showRoute(List<Integer> path) { highlighted = path; routeMode = true; repaint(); }
        void showOrder(List<Integer> order) { highlighted = order; routeMode = false; repaint(); }
        void clear() { highlighted = new ArrayList<>(); repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // soft background
            g.setPaint(new GradientPaint(0, 0, new Color(248, 250, 255),
                    0, getHeight(), new Color(224, 234, 252)));
            g.fillRect(0, 0, getWidth(), getHeight());

            CampusNavigator.CampusGraph graph = CampusNavigator.graph;
            int n = graph.size();
            if (n == 0) return;

            // Positions: hand-placed for the sample campus; if you add/rename locations,
            // anything missing from PRESET makes it fall back to an even oval so it still draws.
            int w = getWidth(), h = getHeight();
            int[] x = new int[n], y = new int[n];
            boolean allPlaced = true;
            for (String name : graph.names) if (!PRESET.containsKey(name.toLowerCase())) allPlaced = false;
            for (int i = 0; i < n; i++) {
                if (allPlaced) {
                    double[] p = PRESET.get(graph.names.get(i).toLowerCase());
                    x[i] = 80 + (int) (p[0] * (w - 160));
                    y[i] = 40 + (int) (p[1] * (h - 100));
                } else {
                    int rx = Math.max(60, w / 2 - 100), ry = Math.max(60, h / 2 - 55);
                    double angle = 2 * Math.PI * i / n - Math.PI / 2;
                    x[i] = w / 2 + (int) (rx * Math.cos(angle));
                    y[i] = h / 2 + (int) (ry * Math.sin(angle));
                }
            }

            // Which paths belong to the highlighted route
            Set<String> routeEdges = new HashSet<>();
            if (routeMode) {
                for (int i = 0; i + 1 < highlighted.size(); i++) {
                    int a = highlighted.get(i), b = highlighted.get(i + 1);
                    routeEdges.add(Math.min(a, b) + "-" + Math.max(a, b));
                }
            }

            // 1) lines
            for (int u = 0; u < n; u++) {
                for (CampusNavigator.Edge e : graph.adj.get(u)) {
                    if (u > e.to) continue;                       // draw each path once
                    boolean onRoute = routeEdges.contains(u + "-" + e.to);
                    g.setColor(onRoute ? ROUTE : new Color(186, 198, 214));
                    g.setStroke(new BasicStroke(onRoute ? 5f : 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(x[u], y[u], x[e.to], y[e.to]);
                }
            }

            // 2) distance labels on the lines
            Font weightFont = new Font(FONT, Font.PLAIN, 11);
            FontMetrics wfm = g.getFontMetrics(weightFont);
            g.setFont(weightFont);
            g.setStroke(new BasicStroke(1f));
            for (int u = 0; u < n; u++) {
                for (CampusNavigator.Edge e : graph.adj.get(u)) {
                    if (u > e.to) continue;
                    String s = e.weight + "m";
                    int mx = (x[u] + x[e.to]) / 2, my = (y[u] + y[e.to]) / 2, tw = wfm.stringWidth(s);
                    g.setColor(Color.WHITE);
                    g.fillRoundRect(mx - tw / 2 - 5, my - 9, tw + 10, 17, 10, 10);
                    g.setColor(BORDER);
                    g.drawRoundRect(mx - tw / 2 - 5, my - 9, tw + 10, 17, 10, 10);
                    g.setColor(new Color(71, 85, 105));
                    g.drawString(s, mx - tw / 2, my + 4);
                }
            }

            // 3) locations (circles + names)
            Font numFont = new Font(FONT, Font.BOLD, 12);
            Font nameFont = new Font(FONT, Font.BOLD, 12);
            FontMetrics nfm = g.getFontMetrics(numFont);
            FontMetrics lfm = g.getFontMetrics(nameFont);
            for (int i = 0; i < n; i++) {
                int idx = highlighted.indexOf(i);
                Color fill = NODE;
                String inside = null;
                int r = 14;
                if (idx >= 0) {
                    if (routeMode) {
                        boolean first = idx == 0, last = idx == highlighted.size() - 1;
                        fill = first ? START : last ? END : ROUTE;
                        inside = first ? "S" : last ? "D" : null;
                        r = (first || last) ? 17 : 15;
                    } else {
                        fill = ROUTE;
                        inside = String.valueOf(idx + 1);         // visiting order number
                        r = 15;
                    }
                }
                g.setColor(new Color(0, 0, 0, 45));               // soft shadow
                g.fillOval(x[i] - r + 2, y[i] - r + 3, 2 * r, 2 * r);
                g.setColor(fill);
                g.fillOval(x[i] - r, y[i] - r, 2 * r, 2 * r);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(3f));
                g.drawOval(x[i] - r, y[i] - r, 2 * r, 2 * r);

                if (inside != null) {
                    g.setFont(numFont);
                    g.setColor(Color.WHITE);
                    g.drawString(inside, x[i] - nfm.stringWidth(inside) / 2,
                            y[i] + (nfm.getAscent() - nfm.getDescent()) / 2);
                }

                String label = graph.names.get(i);
                int lw = lfm.stringWidth(label);
                int top = y[i] + r + 4;
                g.setColor(new Color(255, 255, 255, 235));         // name pill
                g.fillRoundRect(x[i] - lw / 2 - 6, top, lw + 12, 20, 10, 10);
                g.setFont(nameFont);
                g.setColor(TEXT);
                g.drawString(label, x[i] - lw / 2, top + 14);
            }

            drawLegend(g);
        }

        private void drawLegend(Graphics2D g) {
            Font f = new Font(FONT, Font.PLAIN, 12);
            g.setFont(f);
            int lx = 16, ly = 22;
            if (highlighted.isEmpty()) {
                g.setColor(MUTED);
                g.drawString("Choose From and To, then click a button.", lx, ly);
                return;
            }
            String[] texts = routeMode
                    ? new String[]{"Start", "Destination", "Route"}
                    : new String[]{"Visiting order (1 = first)"};
            Color[] colors = routeMode ? new Color[]{START, END, ROUTE} : new Color[]{ROUTE};
            for (int i = 0; i < texts.length; i++) {
                g.setColor(colors[i]);
                g.fillOval(lx, ly - 10 + i * 22, 12, 12);
                g.setColor(TEXT);
                g.drawString(texts[i], lx + 20, ly + i * 22);
            }
        }
    }

    // ======================= START =======================

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");    // smoother text
        System.setProperty("swing.aatext", "true");
        CampusNavigator.loadCampus();                          // fill graph + Trie from the backend
        SwingUtilities.invokeLater(() -> new CampusGUI().setVisible(true));
    }
}