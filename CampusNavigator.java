import java.util.*;

public class CampusNavigator {

    static final int INF = Integer.MAX_VALUE;
    static class Edge {
        final int to, weight;
        Edge(int to, int weight) { this.to = to; this.weight = weight; }
    }

    static class Entry {                       // used for sorting by distance
        final String name;
        final int distance;
        Entry(String name, int distance) { this.name = name; this.distance = distance; }
    }

    static class PathResult {
        final int[] dist, parent;
        PathResult(int[] dist, int[] parent) { this.dist = dist; this.parent = parent; }
    }

    // ======================= GRAPH =======================

    static class CampusGraph {
        final List<String> names = new ArrayList<>();
        final Map<String, Integer> idOf = new HashMap<>();      // HashMap: name -> id
        final List<List<Edge>> adj = new ArrayList<>();         // adjacency list

        int size() { return names.size(); }

        int addLocation(String name) {
            String key = name.toLowerCase();
            if (idOf.containsKey(key)) return idOf.get(key);
            int id = names.size();
            names.add(name);
            idOf.put(key, id);
            adj.add(new ArrayList<>());
            return id;
        }

        // Undirected edge: you can walk both ways
        void addPath(String a, String b, int metres) {
            int u = addLocation(a), v = addLocation(b);
            adj.get(u).add(new Edge(v, metres));
            adj.get(v).add(new Edge(u, metres));
        }

        int find(String name) {
            return idOf.getOrDefault(name.trim().toLowerCase(), -1);
        }

        // ---------- Dijkstra: O((V + E) log V) ----------
        PathResult dijkstra(int src) {
            int n = size();
            int[] dist = new int[n];
            int[] parent = new int[n];
            Arrays.fill(dist, INF);
            Arrays.fill(parent, -1);

            PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[1], b[1]));
            dist[src] = 0;
            pq.add(new int[]{src, 0});

            while (!pq.isEmpty()) {
                int[] cur = pq.poll();
                int u = cur[0];
                if (cur[1] > dist[u]) continue;             // outdated entry, skip
                for (Edge e : adj.get(u)) {
                    int nd = dist[u] + e.weight;
                    if (nd < dist[e.to]) {                   // found a shorter way
                        dist[e.to] = nd;
                        parent[e.to] = u;
                        pq.add(new int[]{e.to, nd});
                    }
                }
            }
            return new PathResult(dist, parent);
        }

        // ---------- BFS: O(V + E), fewest stops ----------
        int[] bfs(int src) {
            int n = size();
            int[] parent = new int[n];
            boolean[] seen = new boolean[n];
            Arrays.fill(parent, -1);

            Queue<Integer> q = new ArrayDeque<>();
            q.add(src);
            seen[src] = true;

            while (!q.isEmpty()) {
                int u = q.poll();
                for (Edge e : adj.get(u)) {
                    if (!seen[e.to]) {
                        seen[e.to] = true;
                        parent[e.to] = u;
                        q.add(e.to);
                    }
                }
            }
            return parent;
        }

        // ---------- DFS: O(V + E) ----------
        void dfs(int u, boolean[] seen, List<String> order) {
            seen[u] = true;
            order.add(names.get(u));
            for (Edge e : adj.get(u)) {
                if (!seen[e.to]) dfs(e.to, seen, order);
            }
        }

        // Rebuild path src -> dest using the parent array
//        List<Integer> buildPath(int[] parent, int src, int dest) {
//            LinkedList<Integer> path = new LinkedList<>();
//            for (int at = dest; at != -1; at = parent[at]) path.addFirst(at);
//            if (path.isEmpty() || path.getFirst() != src) return new ArrayList<>();   // unreachable
//            return path;
//        }

        List<Integer> buildPath(int[] parent, int src, int dest) {
            List<Integer> path = new ArrayList<>();
            for (int at = dest; at != -1; at = parent[at]) path.add(at);
            Collections.reverse(path);
            if (path.isEmpty() || path.get(0) != src) return new ArrayList<>();   // unreachable
            return path;
        }

        int pathLength(List<Integer> path) {
            int total = 0;
            for (int i = 0; i + 1 < path.size(); i++) {
                for (Edge e : adj.get(path.get(i))) {
                    if (e.to == path.get(i + 1)) { total += e.weight; break; }
                }
            }
            return total;
        }
    }

    // ======================= TRIE =======================

    static class Trie {
        static class Node {
            final Map<Character, Node> next = new TreeMap<>();   // TreeMap -> alphabetical output
            String word;                                         // set only where a name ends
        }

        final Node root = new Node();

        void insert(String name) {
            Node cur = root;
            for (char c : name.toLowerCase().toCharArray()) {
                cur = cur.next.computeIfAbsent(c, k -> new Node());
            }
            cur.word = name;
        }

        List<String> startsWith(String prefix) {
            Node cur = root;
            for (char c : prefix.toLowerCase().toCharArray()) {
                cur = cur.next.get(c);
                if (cur == null) return new ArrayList<>();
            }
            List<String> out = new ArrayList<>();
            collect(cur, out);
            return out;
        }

        private void collect(Node n, List<String> out) {
            if (n.word != null) out.add(n.word);
            for (Node child : n.next.values()) collect(child, out);
        }
    }

    // ======================= MERGE SORT: O(n log n) =======================

    static void mergeSort(Entry[] a, int l, int r) {
        if (l >= r) return;
        int m = (l + r) / 2;
        mergeSort(a, l, m);
        mergeSort(a, m + 1, r);
        merge(a, l, m, r);
    }

    static void merge(Entry[] a, int l, int m, int r) {
        Entry[] tmp = new Entry[r - l + 1];
        int i = l, j = m + 1, k = 0;
        while (i <= m && j <= r) tmp[k++] = (a[i].distance <= a[j].distance) ? a[i++] : a[j++];
        while (i <= m) tmp[k++] = a[i++];
        while (j <= r) tmp[k++] = a[j++];
        System.arraycopy(tmp, 0, a, l, tmp.length);
    }

    // ======================= APP STATE =======================

    static final CampusGraph graph = new CampusGraph();
    static final Trie trie = new Trie();
    static final Deque<String> recent = new ArrayDeque<>();     // used as a Stack
    static final Scanner sc = new Scanner(System.in);

    // ======================= CAMPUS DATA (edit this!) =======================

    static void loadCampus() {
        graph.addPath("Main Gate", "Admin Block", 150);
        graph.addPath("Main Gate", "Parking", 80);
        graph.addPath("Admin Block", "Library", 120);
        graph.addPath("Admin Block", "CSE Department", 200);
        graph.addPath("Parking", "Canteen", 250);
        graph.addPath("Library", "CSE Department", 90);
        graph.addPath("Library", "Seminar Hall", 140);
        graph.addPath("CSE Department", "AI-ML Lab", 60);
        graph.addPath("CSE Department", "Canteen", 130);
        graph.addPath("Canteen", "Hostel", 300);
        graph.addPath("Canteen", "Sports Ground", 180);
        graph.addPath("Seminar Hall", "Auditorium", 100);
        graph.addPath("Auditorium", "Sports Ground", 220);
        graph.addPath("Hostel", "Sports Ground", 160);
        graph.addPath("Admin Block", "Medical Room", 70);

        for (String name : graph.names) trie.insert(name);       // fill Trie from the graph
    }

    // ======================= HELPERS =======================

    static String ask(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    // Finds a location id; if not found, suggests names using the Trie
    static int resolve(String input) {
        int id = graph.find(input);
        if (id == -1) {
            System.out.println("  Location not found.");
            List<String> suggestions = trie.startsWith(input);
            if (!suggestions.isEmpty()) {
                System.out.println("  Did you mean: " + String.join(", ", suggestions));
            }
        } else {
            recent.push(graph.names.get(id));                    // Stack push
            if (recent.size() > 10) recent.removeLast();         // keep last 10 only
        }
        return id;
    }

    static void printPath(List<Integer> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append(" -> ");
            sb.append(graph.names.get(path.get(i)));
        }
        System.out.println("  Route: " + sb);
    }

    static void showAllLocations() {
        System.out.println("  Locations: " + String.join(", ", graph.names));
    }

    // ======================= MENU ACTIONS =======================

    static void shortestPath() {
        int src = resolve(ask("  From: "));
        if (src == -1) return;
        int dest = resolve(ask("  To:   "));
        if (dest == -1) return;

        PathResult r = graph.dijkstra(src);
        List<Integer> path = graph.buildPath(r.parent, src, dest);
        if (path.isEmpty()) { System.out.println("  No route found."); return; }

        printPath(path);
        System.out.println("  Total distance: " + r.dist[dest] + " m (about "
                + Math.round(r.dist[dest] / 80.0) + " min walking)");
    }

    static void fewestStops() {
        int src = resolve(ask("  From: "));
        if (src == -1) return;
        int dest = resolve(ask("  To:   "));
        if (dest == -1) return;

        int[] parent = graph.bfs(src);
        List<Integer> path = graph.buildPath(parent, src, dest);
        if (path.isEmpty()) { System.out.println("  No route found."); return; }

        printPath(path);
        System.out.println("  Stops: " + (path.size() - 1) + " hops, distance: "
                + graph.pathLength(path) + " m");
    }

    static void exploreCampus() {
        int src = resolve(ask("  Start from: "));
        if (src == -1) return;

        List<String> order = new ArrayList<>();
        graph.dfs(src, new boolean[graph.size()], order);
        System.out.println("  DFS visit order: " + String.join(" -> ", order));
    }

    static void searchLocation() {
        String prefix = ask("  Type a name or prefix: ");
        List<String> matches = trie.startsWith(prefix);
        if (matches.isEmpty()) System.out.println("  No matches.");
        else System.out.println("  Matches: " + String.join(", ", matches));
    }

    static void nearbySorted() {
        int src = resolve(ask("  Your current location: "));
        if (src == -1) return;

        PathResult r = graph.dijkstra(src);
        List<Entry> list = new ArrayList<>();
        for (int i = 0; i < graph.size(); i++) {
            if (i != src && r.dist[i] != INF) list.add(new Entry(graph.names.get(i), r.dist[i]));
        }
        Entry[] arr = list.toArray(new Entry[0]);
        mergeSort(arr, 0, arr.length - 1);

        System.out.println("  Locations from nearest to farthest:");
        for (Entry e : arr) System.out.printf("    %-16s %d m%n", e.name, e.distance);
    }

    static void recentSearches() {
        if (recent.isEmpty()) { System.out.println("  No recent searches."); return; }
        System.out.println("  Recent (latest first): " + String.join(", ", recent));
    }

    // ======================= MAIN =======================

    public static void main(String[] args) {
        loadCampus();
        System.out.println("=== Smart Campus Navigation System ===");
        showAllLocations();

        while (true) {
            System.out.println("\n1. Shortest route (Dijkstra)");
            System.out.println("2. Fewest stops route (BFS)");
            System.out.println("3. Explore campus from a location (DFS)");
            System.out.println("4. Search location / autocomplete (Trie)");
            System.out.println("5. Nearest to farthest (Merge Sort)");
            System.out.println("6. Recent searches (Stack)");
            System.out.println("7. Show all locations");
            System.out.println("0. Exit");

            String choice = ask("Choose: ");
            switch (choice) {
                case "1": shortestPath(); break;
                case "2": fewestStops(); break;
                case "3": exploreCampus(); break;
                case "4": searchLocation(); break;
                case "5": nearbySorted(); break;
                case "6": recentSearches(); break;
                case "7": showAllLocations(); break;
                case "0": System.out.println("Goodbye!"); return;
                default:  System.out.println("  Invalid choice.");
            }
        }
    }
}