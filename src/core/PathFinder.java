package core;


import enums.*;
import java.util.*;
import model.*;

public class PathFinder {

    private final Train        train;
    private final GraphBuilder graph;
    private final Map<String, Node> nodesById = new HashMap<>();

    public PathFinder(Train train, GraphBuilder graph) {
        if (train == null) {
            throw new IllegalArgumentException("Train is null");
        }
        if (graph == null) {
            throw new IllegalArgumentException("Graph is null");
        }
        this.train = train;
        this.graph = graph;
        indexNodes();
    }

    // Entry point — picks algorithm based on train priority
    public List<TrackTraversal> findPath() {
        // if (train.getPriority() == TrainPriority.GOODS
        //  || train.getPriority() == TrainPriority.LOCAL) {
        //     return bellmanFord();
        // }
        return dijkstra();
    }

    // ─────────────────────────────────────────────
    // DIJKSTRA — EXPRESS and PASSENGER_EXP
    // weight = distance / trackSpeedLimit (travel time in seconds)
    // no junction limit
    // ─────────────────────────────────────────────
    private List<TrackTraversal> dijkstra() {
        String startId = train.getStartNode();
        String endId   = train.getEndNode();

        Map<String, Double> dist = new HashMap<>();
        Map<String, PrevStep> prevStep = new HashMap<>();
        PriorityQueue<SearchEntry> pq = new PriorityQueue<>(
            Comparator.comparingDouble(SearchEntry::cost)
        );

        String startState = stateKey(null, startId);
        dist.put(startState, 0.0);
        pq.offer(new SearchEntry(startState, null, startId, 0.0));

        String bestEndState = null;

        while (!pq.isEmpty()) {
            SearchEntry curr = pq.poll();
            if (curr.cost() > dist.getOrDefault(curr.stateKey(),
                    Double.MAX_VALUE)) {
                continue;
            }

            if (curr.currId().equals(endId)) {
                bestEndState = curr.stateKey();
                break;
            }

            for (Track track : graph.getTracksFrom(curr.currId())) {
                String neighborId = getNeighbor(track, curr.currId());
                if (neighborId == null)     continue; // not connected
                if (!track.isAllowedFor(train.getType())) continue;
                if (!isMovementAllowedAtCurrentNode(curr.currId(),
                        curr.prevId(), neighborId)) {
                    continue;
                }

                double weight  = travelTime(track);
                double newCost = curr.cost() + weight;

                String nextState = stateKey(curr.currId(), neighborId);

                if (newCost < dist.getOrDefault(nextState,
                        Double.MAX_VALUE)) {
                    dist.put(nextState, newCost);
                    Direction dir = track.getStartNode().getId()
                        .equals(curr.currId())
                        ? Direction.FORWARD
                        : Direction.REVERSE;
                    prevStep.put(nextState, new PrevStep(
                        curr.stateKey(), new TrackTraversal(track, dir)
                    ));
                    pq.offer(new SearchEntry(nextState, curr.currId(),
                        neighborId, newCost));
                }
            }
        }

        return reconstructPath(prevStep, startState, bestEndState,
            startId, endId);
    }

    // ─────────────────────────────────────────────
    // BELLMAN-FORD — GOODS and LOCAL
    // weight = distance / trackSpeedLimit
    // K = junction limit (max junctions allowed)
    // runs K+1 rounds
    // ─────────────────────────────────────────────
    // private List<Track> bellmanFord() {
    //     String startId = train.getStartNode();
    //     String endId   = train.getEndNode();

    //     // Junction limit based on train type
    //     int K = (train.getPriority() == TrainPriority.GOODS) ? 3 : 2;

    //     // dist[round][nodeId] = best cost using at most `round` tracks
    //     List<String> nodeIds = new ArrayList<>(
    //         graph.getAdjacencyList().keySet()
    //     );

    //     Map<String, Double> dist    = new HashMap<>();
    //     Map<String, Double> temp    = new HashMap<>();
    //     Map<String, Track>  prevTrack = new HashMap<>();

    //     for (String id : nodeIds) dist.put(id, Double.MAX_VALUE);
    //     dist.put(startId, 0.0);

    //     // Run K+1 rounds (K = max junctions = max edges in this context)
    //     for (int round = 0; round < K + 1; round++) {

    //         // Snapshot — temp copy of dist before this round
    //         for (String id : nodeIds) temp.put(id, dist.get(id));

    //         for (String currId : nodeIds) {
    //             if (dist.get(currId) == Double.MAX_VALUE) continue;

    //             for (Track track : graph.getTracksFrom(currId)) {
    //                 String neighborId = getNeighbor(track, currId);
    //                 if (neighborId == null) continue;

    //                 double weight  = travelTime(track);
    //                 double newCost = dist.get(currId) + weight;

    //                 // Use temp snapshot — not dist — to avoid using
    //                 // updates from same round (critical for K constraint)
    //                 if (newCost < temp.get(neighborId)) {
    //                     temp.put(neighborId, newCost);
    //                     prevTrack.put(neighborId, track);
    //                 }
    //             }
    //         }

    //         // Commit this round's results
    //         for (String id : nodeIds) dist.put(id, temp.get(id));
    //     }

    //     return reconstructPath(prevTrack, startId, endId);
    // }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    // Returns the neighbor node ID based on topology (bidirectional)
    // Returns null if this track is not connected to currentNodeId
    private String getNeighbor(Track track, String currentNodeId) {
        if (track.getStartNode().getId().equals(currentNodeId)) {
            return track.getEndNode().getId();
        }

        // REVERSE direction
        if (track.getEndNode().getId().equals(currentNodeId)) {
            return track.getStartNode().getId();
        }

        return null;
    }

    // Switch routing rules are evaluated when the train is at the junction,
    // using the previous node (incoming side) and candidate next node.
    private boolean isMovementAllowedAtCurrentNode(String currentNodeId,
                                                   String prevNodeId,
                                                   String nextNodeId) {
        Node current = nodesById.get(currentNodeId);
        if (!(current instanceof JunctionNode)) {
            return true;
        }
        if (prevNodeId == null) {
            return true;
        }

        JunctionNode junction = (JunctionNode) current;
        String facing = junction.getFacingPointer();
        String diverging = junction.getDivergingPointer();
        boolean state = junction.getState();

        if (prevNodeId.equals(facing)) {
            if (state) {
                return nextNodeId.equals(diverging);
            }
            return !nextNodeId.equals(facing)
                && !nextNodeId.equals(diverging);
        }

        if (prevNodeId.equals(diverging)) {
            if (!state) {
                return false;
            }
            return nextNodeId.equals(facing);
        }

        if (state) {
            return false;
        }
        return nextNodeId.equals(facing);
    }

    private void indexNodes() {
        for (List<Track> tracks : graph.getAdjacencyList().values()) {
            for (Track track : tracks) {
                Node start = track.getStartNode();
                Node end = track.getEndNode();
                nodesById.putIfAbsent(start.getId(), start);
                nodesById.putIfAbsent(end.getId(), end);
            }
        }
    }

    private String stateKey(String prevId, String currId) {
        return (prevId == null ? "NULL" : prevId) + "->" + currId;
    }

    // Travel time in seconds = distance / minSpeedLimit
    private double travelTime(Track track) {
        if (track.getMinSpeedLimit() <= 0 || track.getDistance() <= 0) {
            return 0; // spawn track or zero distance — no travel time
        }
        return track.getDistance() / track.getMinSpeedLimit();
    }

    // Reconstruct path by walking prevTrack map backwards from end to start
    private List<TrackTraversal> reconstructPath(
            Map<String, PrevStep> prevStep,
            String startState,
            String endState,
            String startId,
            String endId) {

        if (endState == null) {
            System.out.println("WARNING: No path found for train "
                + train.getId()
                + " from " + startId
                + " to "   + endId);
            return new ArrayList<>();
        }

        List<TrackTraversal> path = new ArrayList<>();
        String currentState = endState;

        while (!currentState.equals(startState)) {
            PrevStep step = prevStep.get(currentState);
            if (step == null) {
                System.out.println("WARNING: No path found for train "
                    + train.getId()
                    + " from " + startId
                    + " to "   + endId);
                return new ArrayList<>();
            }
            path.add(step.traversal());
            currentState = step.previousStateKey();
        }

        Collections.reverse(path);
        return path;
    }

    private static final class SearchEntry {
        private final String stateKey;
        private final String prevId;
        private final String currId;
        private final double cost;

        private SearchEntry(String stateKey, String prevId,
                            String currId, double cost) {
            this.stateKey = stateKey;
            this.prevId = prevId;
            this.currId = currId;
            this.cost = cost;
        }

        private String stateKey() { return stateKey; }
        private String prevId() { return prevId; }
        private String currId() { return currId; }
        private double cost() { return cost; }
    }

    private static final class PrevStep {
        private final String previousStateKey;
        private final TrackTraversal traversal;

        private PrevStep(String previousStateKey,
                         TrackTraversal traversal) {
            this.previousStateKey = previousStateKey;
            this.traversal = traversal;
        }

        private String previousStateKey() { return previousStateKey; }
        private TrackTraversal traversal() { return traversal; }
    }
}