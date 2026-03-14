package core;

import java.util.*;
import model.Track;

public class GraphBuilder {

    private final HashMap<String, List<Track>> adjacencyList = new HashMap<>();

    public GraphBuilder() {
    }

    public void addTrack(Track track) {
        String fromId = track.getStartNode().getId();
        String toId   = track.getEndNode().getId();

        adjacencyList.computeIfAbsent(fromId, k -> new ArrayList<>()).add(track);
        adjacencyList.computeIfAbsent(toId,   k -> new ArrayList<>()).add(track);
    }


    public HashMap<String, List<Track>> getAdjacencyList() {
        return adjacencyList;
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