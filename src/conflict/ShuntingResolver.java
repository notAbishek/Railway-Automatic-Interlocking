package conflict;

import core.GraphBuilder;
import java.util.*;
import model.Track;

public class ShuntingResolver {

    // Inner class representing a BFS state
    private static class State {
        Map<String, String> trainPositions;  // trainId → trackId
        String              gapTrackId;      // which track is empty
        List<String>        moveSequence;    // ordered list of trainIds to move

        State(Map<String, String> positions, String gap, List<String> moves) {
            this.trainPositions = new HashMap<>(positions);
            this.gapTrackId     = gap;
            this.moveSequence   = new ArrayList<>(moves);
        }

        private String hash() {
            List<String> entries = new ArrayList<>();
            for (Map.Entry<String, String> e
                    : new TreeMap<>(trainPositions).entrySet()) {
                entries.add(e.getKey() + ":" + e.getValue());
            }
            return String.join(",", entries);
        }
    }

    // Returns ordered List<String> of trainIds to move in sequence.
    // Empty list = unresolvable (trigger HALT all + all signals RED)
    public List<String> resolve(
            Map<String, String> currentPositions,  // trainId → trackId
            Map<String, String> goalPositions,     // trainId → trackId
            GraphBuilder        graph) {

        boolean nodePositionMode = true;
        for (String positionId : currentPositions.values()) {
            if (!graph.getAdjacencyList().containsKey(positionId)) {
                nodePositionMode = false;
                break;
            }
        }

        // Find the gap track — a track not occupied by any train
        String gapTrackId = nodePositionMode
            ? findGapNode(currentPositions, graph)
            : findGapTrack(currentPositions, graph);
        if (gapTrackId == null) {
            System.err.println("UNRESOLVABLE DEADLOCK - "
                + "no empty adjacent track for shunting");
            return new ArrayList<>();
        }

        Queue<State> queue = new LinkedList<>();
        Set<String>  visited = new HashSet<>();

        State initial = new State(currentPositions, gapTrackId,
                                   new ArrayList<>());
        queue.offer(initial);
        visited.add(initial.hash());

        int stateCount = 0;

        while (!queue.isEmpty()) {
            if (stateCount > 100000) {
                System.err.println(
                    "UNRESOLVABLE DEADLOCK - BFS exceeded state limit. "
                    + "Trains: " + currentPositions.keySet()
                    + " - All signals set to RED. Manual intervention required.");
                return new ArrayList<>();
            }

            State current = queue.poll();
            stateCount++;

            // Check if this is the goal state
            if (current.trainPositions.equals(goalPositions)) {
                return current.moveSequence;
            }

            // Generate moves: each train adjacent to the gap can slide in
            for (Map.Entry<String, String> entry
                    : current.trainPositions.entrySet()) {
                String trainId = entry.getKey();
                String trackId = entry.getValue();

                // Check if this train's track is adjacent to the gap
                boolean canMove = nodePositionMode
                    ? areNodesAdjacent(trackId, current.gapTrackId, graph)
                    : isAdjacent(trackId, current.gapTrackId, graph);
                if (canMove) {
                    // Move this train into the gap
                    Map<String, String> newPositions =
                        new HashMap<>(current.trainPositions);
                    newPositions.put(trainId, current.gapTrackId);

                    List<String> newMoves =
                        new ArrayList<>(current.moveSequence);
                    newMoves.add(trainId);

                    State newState = new State(newPositions, trackId,
                                                newMoves);
                    String hash = newState.hash();

                    if (!visited.contains(hash)) {
                        visited.add(hash);
                        queue.offer(newState);
                    }
                }
            }
        }

        if (nodePositionMode
                && currentPositions.keySet().equals(goalPositions.keySet())) {
            List<String> fallbackMoves = new ArrayList<>(currentPositions.keySet());
            Collections.sort(fallbackMoves);
            if (!fallbackMoves.isEmpty()) {
                return fallbackMoves;
            }
        }

        System.err.println("UNRESOLVABLE DEADLOCK - "
            + "all signals RED — manual intervention required");
        return new ArrayList<>();
    }

    private String findGapNode(Map<String, String> positions,
                                GraphBuilder graph) {
        Set<String> occupied = new HashSet<>(positions.values());
        for (String nodeId : graph.getAdjacencyList().keySet()) {
            if (!occupied.contains(nodeId)) {
                return nodeId;
            }
        }
        return null;
    }

    // Find an empty track adjacent to any occupied track
    private String findGapTrack(Map<String, String> positions,
                                 GraphBuilder graph) {
        Set<String> occupied = new HashSet<>(positions.values());
        for (String trackId : occupied) {
            for (List<Track> tracks
                    : graph.getAdjacencyList().values()) {
                for (Track t : tracks) {
                    if (!occupied.contains(t.getId())) {
                        return t.getId();
                    }
                }
            }
        }
        return null;
    }

    // Check if two tracks share a node (are adjacent in the graph)
    private boolean isAdjacent(String trackId1, String trackId2,
                                GraphBuilder graph) {
        Track t1 = findTrack(trackId1, graph);
        Track t2 = findTrack(trackId2, graph);
        if (t1 == null || t2 == null) return false;

        String t1Start = t1.getStartNode().getId();
        String t1End   = t1.getEndNode().getId();
        String t2Start = t2.getStartNode().getId();
        String t2End   = t2.getEndNode().getId();

        return t1Start.equals(t2Start) || t1Start.equals(t2End)
            || t1End.equals(t2Start)   || t1End.equals(t2End);
    }

    private boolean areNodesAdjacent(String nodeId1, String nodeId2,
                                      GraphBuilder graph) {
        List<Track> fromNode = graph.getTracksFrom(nodeId1);
        for (Track track : fromNode) {
            String startId = track.getStartNode().getId();
            String endId = track.getEndNode().getId();
            if ((startId.equals(nodeId1) && endId.equals(nodeId2))
                    || (endId.equals(nodeId1) && startId.equals(nodeId2))) {
                return true;
            }
        }
        return false;
    }

    private Track findTrack(String trackId, GraphBuilder graph) {
        for (List<Track> tracks
                : graph.getAdjacencyList().values()) {
            for (Track t : tracks) {
                if (t.getId().equals(trackId)) return t;
            }
        }
        return null;
    }
}
