package core;

import java.util.*;
import model.Node;
import model.Track;

public class GraphBuilder {

    private final HashMap<String, List<Track>> adjacencyList = new HashMap<>();

    public GraphBuilder() {}

    public void addNode(Node node) {
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
    }

    public void addTrack(Track track) {
        String fromId = track.getStartNode().getId();
        String toId   = track.getEndNode().getId();

        if (!adjacencyList.containsKey(fromId)) {
            throw new IllegalArgumentException(
                "Node " + fromId + " not found. Add node before adding track."
            );
        }
        if (!adjacencyList.containsKey(toId)) {
            throw new IllegalArgumentException(
                "Node " + toId + " not found. Add node before adding track."
            );
        }

        adjacencyList.get(fromId).add(track);
    }


    public HashMap<String, List<Track>> getAdjacencyList() {
        return adjacencyList;
    }

    public List<Track> getTracksFrom(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, new ArrayList<>());
    }

    public int getDegree(String nodeId) {
        return getTracksFrom(nodeId).size();
    }


    // public void printGraph() {
    //     System.out.println("=== GRAPH ===");
    //     for (Map.Entry<String, List<Track>> entry : adjacencyList.entrySet()) {
    //         String nodeId = entry.getKey();
    //         List<Track> outgoing = entry.getValue();
    //         if (outgoing.isEmpty()) {
    //             System.out.println(nodeId + " → (no outgoing tracks)");
    //         } else {
    //             for (Track t : outgoing) {
    //                 System.out.println(nodeId
    //                     + " →[" + t.getId()
    //                     + " | " + t.getDistance() + "m]→ "
    //                     + t.getEndNode().getId());
    //             }
    //         }
    //     }
    //     System.out.println("=============");
    // }
}