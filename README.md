# Campus-Navigator
A Java console application that helps students find their way around a college campus. It models the campus as a map of buildings and walking paths, and uses core data structures and algorithms to answer navigation questions.

## Features
- Shortest route between two locations (Dijkstra's algorithm with a priority queue)
- Route with the fewest stops (Breadth-First Search with a queue)
- Campus exploration order (Depth-First Search)
- Location search with autocomplete and "did you mean" suggestions (Trie)
- Locations sorted from nearest to farthest (Merge Sort)
- Recent searches (Stack)
- Fast name-to-location lookup (HashMap)

## Data Structures and Algorithms Used
| Feature | DSA Concept | Complexity |
|---|---|---|
| Campus map | Graph (adjacency list) | O(V + E) space |
| Shortest route | Dijkstra with PriorityQueue | O((V + E) log V) |
| Fewest stops | BFS | O(V + E) |
| Explore campus | DFS | O(V + E) |
| Search and autocomplete | Trie | O(length of prefix) |
| Sort by distance | Merge Sort | O(n log n) |
| Recent searches | Stack | O(1) per operation |

## How to Run
1. Install Java 8 or newer.
2. Clone this repository.
3. Compile: javac CampusNavigator.java
4. Run: java CampusNavigator

You can also open the project in IntelliJ IDEA and click Run.

## How to Use
Type a menu number and press Enter. When asked for a location, type its name, for example Main Gate. Capital letters do not matter. Use option 7 to see all valid location names.

## Sample Output
Route: Main Gate -> Admin Block -> CSE Department -> AI-ML Lab
Total distance: 410 m (about 5 min walking)

## Future Scope
- Graphical interface using Java Swing
- Real GPS coordinates and map view
- Handling blocked or closed paths
- Indoor navigation for individual buildings

## Author
Zaara Abrar
B.Tech CSE (AI and ML)
```
