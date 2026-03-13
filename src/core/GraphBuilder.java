package core;

import model.Node;
import model.Track;

import java.util.HashMap;
import java.util.ArrayList;

public class GraphBuilder {
    HashMap<String, ArrayList<String>> nodes = new HashMap<>();

    public GraphBuilder(Node startNode, Node endNode) {
        ArrayList<String> arr1 = new ArrayList<>();
        arr1.add(endNode.getId());
        arr1.add(startNode.getId());
        ArrayList<String> arr2 = new ArrayList<>();
        arr1.add(startNode.getId());
        nodes.put(startNode.getId(), arr1);
        nodes.put(endNode.getId(), arr2);
    }
}