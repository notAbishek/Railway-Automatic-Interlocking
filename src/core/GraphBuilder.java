package core;

import java.util.*;
import model.Track;
import model.SignalNode;

public class GraphBuilder {

    private final HashMap<String, List<Track>> adjacencyList = new HashMap<>();

    public GraphBuilder() {}

    public void addTrack(Track track) {
        String fromId = track.getStartNode().getId();
        String toId   = track.getEndNode().getId();

        adjacencyList.computeIfAbsent(fromId, k -> new ArrayList<>()).add(track);
        adjacencyList.computeIfAbsent(toId,   k -> new ArrayList<>()).add(track);

        // Auto-assign protectsTrackId on SignalNodes from topology
        if (track.getStartNode() instanceof SignalNode) {
            ((SignalNode) track.getStartNode())
                .setProtectsTrackId(track.getId());
        }
        if (track.getEndNode() instanceof SignalNode) {
            ((SignalNode) track.getEndNode())
                .setProtectsTrackId(track.getId());
        }
    }

    public List<Track> getTracksFrom(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, new ArrayList<>());
    }

    public HashMap<String, List<Track>> getAdjacencyList() {
        return adjacencyList;
    }
}