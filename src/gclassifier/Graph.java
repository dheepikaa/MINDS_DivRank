package gclassifier;

// this program creates the PPI network from the given dataset
import java.util.*;

public class Graph {

    public HashSet<Node> nodes;
    private Set<Edge> edges;
    // map each protein id to a Node in the graph
    private HashMap<Integer, Node> nodeMap;

    public Graph() {
        nodes = new HashSet();
        edges = new HashSet();
        nodeMap = new HashMap();
    }

    public Node getNodebyID(int id){
        return nodeMap.get(id);
    }
    
    public int noNodes() {
        return nodes.size();
    }

    // need to reimplement because edges seem to be bogus
    public int noEdges() {
        return edges.size() / 2;
    }

    public void addEdge(String a, String b, HashMap<String, Integer> protid) {
        if (a == null ? b == null : a.equals(b)) {    // there cannot be an edge to itself
//            System.err.println("Lalalala lololo " + a + " " + b);
            return;
        }
        if (protid.containsKey(a) && protid.containsKey(b)) {
            Integer id1 = protid.get(a);
            Integer id2 = protid.get(b);

            Node n1 = nodeMap.get(id1);
            if (n1 == null) {
                n1 = new Node(id1, a);
                nodes.add(n1);
                nodeMap.put(id1, n1);
            }

            Node n2 = nodeMap.get(id2);
            if (n2 == null) {
                n2 = new Node(id2, b);
                nodes.add(n2);
                nodeMap.put(id2, n2);
            }

            n1.neighbors.add(n2);
            n2.neighbors.add(n1);

            Edge e1 = new Edge();
            e1.node1 = n1;
            e1.node2 = n2;
            edges.add(e1);

            Edge e2 = new Edge();  //the same edge is added again from the perspective of node2
            e2.node1 = n2;
            e2.node2 = n1;
            edges.add(e2);
        }
    }

    void addEdgeMaxSize(String a, String b, HashMap<String, Integer> protid, int maxSize) {
        if (a == null ? b == null : a.equals(b)) {    // there cannot be an edge to itself
//            System.err.println("Lalalala lololo " + a + " " + b);
            return;
        }      
        
        if (protid.containsKey(a) && protid.containsKey(b)){// && noEdges() < maxSize) {
            Integer id1 = protid.get(a);
            Integer id2 = protid.get(b);

            Node n1 = nodeMap.get(id1);
            if (n1 == null) {
                if (nodes.size() < maxSize) {
                    n1 = new Node(id1, a);
                    nodes.add(n1);
                    nodeMap.put(id1, n1);
                } else {
                    return;
                }
            }

            Node n2 = nodeMap.get(id2);
            if (n2 == null) {
                if (nodes.size() < maxSize) {
                    n2 = new Node(id2, b);
                    nodes.add(n2);
                    nodeMap.put(id2, n2);
                } else {
                    return;
                }
            }

            n1.neighbors.add(n2);
            n2.neighbors.add(n1);

            Edge e1 = new Edge();
            e1.node1 = n1;
            e1.node2 = n2;
            edges.add(e1);

            Edge e2 = new Edge();  //the same edge is added again from the perspective of node2
            e2.node1 = n2;
            e2.node2 = n1;
            edges.add(e2);
        }
    }

    // add edge even if nodes is not in sample list
    void addArbitraryEdge(String a, String b, HashMap<String, Integer> protid) {
        if (a == null ? b == null : a.equals(b)) {    // there cannot be an edge to itself
            return;
        }

        Integer id1 = protid.get(a);
        if (id1 == null) {
            id1 = protid.size() + 1;
            protid.put(a, id1);
        }

        Integer id2 = protid.get(b);
        if (id2 == null) {
            id2 = protid.size() + 1;
            protid.put(b, id2);
        }

        Node n1 = nodeMap.get(id1);
        if (n1 == null) {
            n1 = new Node(id1, a);
            nodes.add(n1);
            nodeMap.put(id1, n1);
        }

        Node n2 = nodeMap.get(id2);
        if (n2 == null) {
            n2 = new Node(id2, b);
            nodes.add(n2);
            nodeMap.put(id2, n2);
        }

        n1.neighbors.add(n2);
        n2.neighbors.add(n1);

        Edge e1 = new Edge();
        e1.node1 = n1;
        e1.node2 = n2;
        edges.add(e1);

        Edge e2 = new Edge();  //the same edge is added again from the perspective of node2
        e2.node1 = n2;
        e2.node2 = n1;
        edges.add(e2);
    }
    
    public void deleteEdge(int id1, int id2){
        Node n1 = nodeMap.get(id1);
        Node n2 = nodeMap.get(id2);
        if (n1 == null || n2 == null || !n1.neighbors.contains(n2) || !n2.neighbors.contains(n1)){
            System.err.println("Delete unexisted edge!");
        } else {
            n1.neighbors.remove(n2);
            n2.neighbors.remove(n1);
        }
    }
}
