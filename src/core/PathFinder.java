package core;


import enums.*;
import java.util.*;
import model.*;

public class PathFinder {

    private final Train        train;
    private final GraphBuilder graph;

    public PathFinder(Train train, GraphBuilder graph) {
        if (train == null) {
            throw new IllegalArgumentException("Train is null");
        }
        if (graph == null) {
            throw new IllegalArgumentException("Graph is null");
        }
        this.train = train;
        this.graph = graph;
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

        // dist — shortest travel time to each node
        Map<String, Double>         dist      = new HashMap<>();
        // prev — which track+direction brought us to this node
        Map<String, TrackTraversal> prevTrack = new HashMap<>();
        // visited
        Set<String>                 visited   = new HashSet<>();

        // PriorityQueue — [nodeId, costSoFar]
        PriorityQueue<double[]> pq = new PriorityQueue<>(
            Comparator.comparingDouble(a -> a[1])
        );

        // Use nodeId as string key, map to index via nodeIndexMap
        Map<String, Integer> nodeIndex = new HashMap<>();
        List<String> nodeIds = new ArrayList<>(
            graph.getAdjacencyList().keySet()
        );
        for (int i = 0; i < nodeIds.size(); i++) {
            nodeIndex.put(nodeIds.get(i), i);
            dist.put(nodeIds.get(i), Double.MAX_VALUE);
        }

        dist.put(startId, 0.0);
        pq.offer(new double[]{ nodeIndex.get(startId), 0.0 });

        while (!pq.isEmpty()) {
            double[] curr      = pq.poll();
            String   currId    = nodeIds.get((int) curr[0]);
            double   currCost  = curr[1];

            if (visited.contains(currId)) continue;
            visited.add(currId);

            if (currId.equals(endId)) break;

            for (Track track : graph.getTracksFrom(currId)) {
                String neighborId = getNeighbor(track, currId);
                if (neighborId == null)     continue; // not connected
                if (visited.contains(neighborId)) continue;

                double weight  = travelTime(track);
                double newCost = currCost + weight;

                if (newCost < dist.get(neighborId)) {
                    dist.put(neighborId, newCost);
                    Direction dir = track.getStartNode().getId().equals(currId)
                        ? Direction.FORWARD
                        : Direction.REVERSE;
                    prevTrack.put(neighborId, new TrackTraversal(track, dir));
                    pq.offer(new double[]{
                        nodeIndex.get(neighborId), newCost
                    });
                }
            }
        }

        return reconstructPath(prevTrack, startId, endId);
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
        // Try FORWARD direction (startNode → endNode)
        if (track.getStartNode().getId().equals(currentNodeId)) {
            // Track type restriction — null allowedTypes means all trains allowed
            if (!track.isAllowedFor(train.getType())) return null;
            return track.getEndNode().getId();
        }
        // Try REVERSE direction (endNode → startNode)
        if (track.getEndNode().getId().equals(currentNodeId)) {
            if (!track.isAllowedFor(train.getType())) return null;
            return track.getStartNode().getId();
        }
        return null;
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
            Map<String, TrackTraversal> prevTrack,
            String startId,
            String endId) {

        List<TrackTraversal> path = new ArrayList<>();
        String current   = endId;

        // Walk backwards from end to start
        while (!current.equals(startId)) {
            TrackTraversal traversal = prevTrack.get(current);

            if (traversal == null) {
                // No path found
                System.out.println("WARNING: No path found for train "
                    + train.getId()
                    + " from " + startId
                    + " to "   + endId);
                return new ArrayList<>();
            }

            path.add(traversal);

            // Step back — which node did we come from?
            current = (traversal.getDirection() == Direction.FORWARD)
                ? traversal.getTrack().getStartNode().getId()
                : traversal.getTrack().getEndNode().getId();
        }

        // Path was built end → start, reverse it
        Collections.reverse(path);
        return path;
    }
}