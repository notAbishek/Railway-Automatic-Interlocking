package conflict;

import java.util.*;

public class DeadlockDetector {

    private Set<String> visited     = new HashSet<>();
    private Set<String> currentPath = new HashSet<>();

    // Returns true if adding this train creates a cycle
    // blockedBy: Map<trainId, trainId> — who is blocking who
    public boolean hasCycle(String trainId,
                             Map<String, String> blockedBy) {
        visited.clear();
        currentPath.clear();
        return dfs(trainId, blockedBy);
    }

    // Returns the list of trainIds forming the cycle, empty if none
    public List<String> findCycle(Map<String, String> blockedBy) {
        visited.clear();
        currentPath.clear();

        for (String trainId : blockedBy.keySet()) {
            List<String> cycle = new ArrayList<>();
            Set<String> localPath = new LinkedHashSet<>();
            if (dfsCycle(trainId, blockedBy, localPath, cycle)) {
                return cycle;
            }
        }
        return new ArrayList<>();
    }

    private boolean dfs(String node, Map<String, String> blockedBy) {
        if (currentPath.contains(node)) return true;  // cycle found
        if (visited.contains(node))     return false;  // already resolved
        currentPath.add(node);
        String blocker = blockedBy.get(node);
        if (blocker != null && dfs(blocker, blockedBy)) return true;
        currentPath.remove(node);
        visited.add(node);
        return false;
    }

    private boolean dfsCycle(String node, Map<String, String> blockedBy,
                              Set<String> localPath, List<String> cycle) {
        if (localPath.contains(node)) {
            // Found cycle — collect nodes in the cycle
            boolean inCycle = false;
            for (String n : localPath) {
                if (n.equals(node)) inCycle = true;
                if (inCycle) cycle.add(n);
            }
            return true;
        }
        if (visited.contains(node)) return false;
        localPath.add(node);
        String blocker = blockedBy.get(node);
        if (blocker != null && dfsCycle(blocker, blockedBy, localPath, cycle)) {
            return true;
        }
        localPath.remove(node);
        visited.add(node);
        return false;
    }
}
