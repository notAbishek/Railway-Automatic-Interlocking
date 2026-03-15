package scheduler;

import conflict.DeadlockDetector;
import conflict.ShuntingResolver;
import core.GraphBuilder;
import java.util.*;
import model.*;

public class DependencyResolver {

    private final GraphBuilder      graph;
    private final DeadlockDetector  deadlockDetector = new DeadlockDetector();
    private final ShuntingResolver  shuntingResolver  = new ShuntingResolver();

    public DependencyResolver(GraphBuilder graph) {
        this.graph = graph;
    }

    // Returns trains in safe dispatch order.
    // If circular deadlock found, shunting sequence prepended.
    public List<Train> resolve(
            List<Train> trains,
            Map<String, List<TrackTraversal>> paths) {

        // Step 1: Build dependency map
        Map<String, String> blockedBy = new HashMap<>();
        Map<String, Train>  trainMap  = new HashMap<>();
        for (Train t : trains) {
            trainMap.put(t.getId(), t);
        }

        for (Train train : trains) {
            List<TrackTraversal> path = paths.get(train.getId());
            if (path == null) continue;
            for (TrackTraversal traversal : path) {
                Track track = traversal.getTrack();
                if (track.isInUse()) {
                    List<String> users = track.getUsedBy();
                    for (String userId : users) {
                        if (!userId.equals(train.getId())) {
                            blockedBy.put(train.getId(), userId);
                            break;
                        }
                    }
                }
                if (blockedBy.containsKey(train.getId())) break;
            }
        }

        // Step 2: Topological sort (Kahn's algorithm using in-degree)
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();

        for (Train t : trains) {
            inDegree.put(t.getId(), 0);
        }

        for (Train t : trains) {
            String blocked = t.getId();
            String blocker = blockedBy.getOrDefault(blocked, null);
            if (blocker == null) {
                continue;
            }
            inDegree.put(blocked,
                inDegree.getOrDefault(blocked, 0) + 1);
            dependents.computeIfAbsent(blocker, k -> new ArrayList<>())
                       .add(blocked);
        }

        Queue<String>  queue  = new LinkedList<>();
        List<Train>    result = new ArrayList<>();

        for (Train t : trains) {
            if (inDegree.getOrDefault(t.getId(), 0) == 0) {
                queue.offer(t.getId());
            }
        }

        while (!queue.isEmpty()) {
            String id = queue.poll();
            Train t = trainMap.get(id);
            if (t != null) result.add(t);

            List<String> deps = dependents.get(id);
            if (deps != null) {
                for (String dep : deps) {
                    int newDeg = inDegree.get(dep) - 1;
                    inDegree.put(dep, newDeg);
                    if (newDeg == 0) queue.offer(dep);
                }
            }
        }

        // Step 3: If result.size() < trains.size() → cycle detected
        if (result.size() < trains.size()) {
            System.out.println("CYCLE DETECTED in dependency graph");
            List<String> cycle = deadlockDetector.findCycle(blockedBy);
            System.out.println("Cycle: " + cycle);

            Map<String, String> currentPositions = new HashMap<>();
            Map<String, String> goalPositions    = new HashMap<>();
            for (Train t : trains) {
                if (t.getTrackOnUse() != null) {
                    currentPositions.put(t.getId(), t.getTrackOnUse());
                }
                List<TrackTraversal> path = paths.get(t.getId());
                if (path != null && !path.isEmpty()) {
                    goalPositions.put(t.getId(),
                        path.get(path.size() - 1).getTrackId());
                }
            }

            List<String> shuntingMoves =
                shuntingResolver.resolve(currentPositions,
                                          goalPositions, graph);
            if (shuntingMoves.isEmpty()) {
                System.out.println("UNRESOLVABLE DEADLOCK — "
                    + "all signals RED — manual intervention required");
            } else {
                System.out.println("SHUNTING SEQUENCE: " + shuntingMoves);
                List<Train> shuntedResult = new ArrayList<>();
                for (String moveId : shuntingMoves) {
                    Train t = trainMap.get(moveId);
                    if (t != null && !shuntedResult.contains(t)) {
                        shuntedResult.add(t);
                    }
                }
                for (Train t : result) {
                    if (!shuntedResult.contains(t)) {
                        shuntedResult.add(t);
                    }
                }
                result = shuntedResult;
            }
        }

        return result;
    }
}
