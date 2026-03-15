package scheduler;

import core.GraphBuilder;
import model.*;
import java.util.*;

public class TrainScheduler {

    private final GraphBuilder graph;
    private IntervalBuilder      intervalBuilder;
    private DependencyResolver   dependencyResolver;
    private MeetAndPassResolver  meetAndPassResolver;

    public TrainScheduler(GraphBuilder graph) {
        this.graph = graph;
    }

    // Inner class for scheduler results
    public static class SchedulerResult {
        public final List<Train>                            orderedTrains;
        public final Map<String, List<TrackTraversal>>      paths;
        public final Map<String, List<TrackInterval>>       intervals;

        public SchedulerResult(
                List<Train> orderedTrains,
                Map<String, List<TrackTraversal>> paths,
                Map<String, List<TrackInterval>> intervals) {
            this.orderedTrains = orderedTrains;
            this.paths         = paths;
            this.intervals     = intervals;
        }
    }

    // Main entry point. Call this before Dispatcher.
    // Returns trains in correct dispatch order with adjusted times.
    public SchedulerResult schedule(List<Train> trains) {
        intervalBuilder     = new IntervalBuilder(graph);
        dependencyResolver  = new DependencyResolver(graph);
        meetAndPassResolver = new MeetAndPassResolver();

        // Step 1: Build paths
        Map<String, List<TrackTraversal>> paths =
            intervalBuilder.buildPaths(trains);

        // Step 2: Build intervals
        Map<String, List<TrackInterval>> intervals =
            intervalBuilder.buildIntervals(trains, paths);

        // Step 3: Dependency resolution (topological sort)
        List<Train> ordered =
            dependencyResolver.resolve(trains, paths);

        // Step 4: Meet-and-pass conflict resolution
        meetAndPassResolver.resolve(ordered, intervals);

        // Step 5: Return result
        return new SchedulerResult(ordered, paths, intervals);
    }
}
