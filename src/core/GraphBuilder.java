package core;

import java.util.*;
import model.Track;

public class GraphBuilder {

    private final HashMap<String, List<Track>> adjacencyList = new HashMap<>();

    public GraphBuilder() {}

    public void addTrack(Track track) {
        String fromId = track.getStartNode().getId();
        String toId   = track.getEndNode().getId();

        adjacencyList.computeIfAbsent(fromId, k -> new ArrayList<>()).add(track);
        adjacencyList.computeIfAbsent(toId,   k -> new ArrayList<>()).add(track);
    }

    public List<Track> getTracksFrom(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, new ArrayList<>());
    }

    public HashMap<String, List<Track>> getAdjacencyList() {
        return adjacencyList;
    }
}