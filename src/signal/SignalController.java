package signal;

import core.GraphBuilder;
import core.MovementContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.*;

public class SignalController {

    private final SignalRule rule = new SignalRule();

    // Attempt to set GREEN. Returns true if successful.
    public boolean requestGreen(
            SignalNode     signal,
            Track          track,
            Track          nextTrack,
            TrackTraversal traversal,
            JunctionNode   junction,
            MovementContext context,
            GraphBuilder   graph) {

        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (track == null) {
            throw new IllegalArgumentException("Track cannot be null");
        }
        if (signal.getProtectsTrackId() == null) {
            return false;
        }

        if (!rule.canSetGreen(track, nextTrack, traversal,
                               junction, context)) {
            setRed(signal, graph);
            return false;
        }
        applySignalState(signal, SignalState.GREEN,
            context.getTrainId(), graph);
        return true;
    }

    // Always safe to call. Reverts signal and linked repeaters to RED.
    public void setRed(SignalNode signal, GraphBuilder graph) {
        applySignalState(signal, SignalState.RED, "SYSTEM", graph);
    }

    // V2 — not yet called
    // Set YELLOW — next signal is RED
    public void setYellow(SignalNode signal, String trainId) {
        SignalState prev = signal.getState();
        signal.setState(SignalState.YELLOW);
        log(signal, prev, SignalState.YELLOW, trainId);
    }

    // V2 — not yet called
    // Set DOUBLE_YELLOW — next signal is YELLOW
    public void setDoubleYellow(SignalNode signal, String trainId) {
        SignalState prev = signal.getState();
        signal.setState(SignalState.DOUBLE_YELLOW);
        log(signal, prev, SignalState.DOUBLE_YELLOW, trainId);
    }

    // Validate train speed against track limits. Emergency RED if over max.
    public void validateSpeed(Train train, Track track,
                               SignalNode signal,
                               GraphBuilder graph) {
        if (train.getSpeed() > track.getEffectiveMaxSpeed()) {
            System.out.println("EMERGENCY: " + train.getId()
                + " speed " + train.getSpeed()
                + " exceeds max " + track.getEffectiveMaxSpeed()
                + " on " + track.getId());
            setRed(signal, graph);
        }
    }

    // Atomically applies an aspect to parent signal and all linked repeaters.
    private void applySignalState(SignalNode signal,
                                   SignalState newState,
                                   String actor,
                                   GraphBuilder graph) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (graph == null) {
            throw new IllegalArgumentException(
                "GraphBuilder cannot be null for signal propagation");
        }

        Map<String, SignalNode> toUpdate = collectLinkedSignals(signal,
            graph);
        toUpdate.put(signal.getId(), signal);

        for (SignalNode s : toUpdate.values()) {
            SignalState prev = s.getState();
            s.setState(newState);
            log(s, prev, newState, actor);
        }
    }

    private Map<String, SignalNode> collectLinkedSignals(
            SignalNode changed,
            GraphBuilder graph) {
        Map<String, SignalNode> repeaters = new LinkedHashMap<>();

        for (List<Track> tracks : graph.getAdjacencyList().values()) {
            for (Track t : tracks) {
                if (t.getStartNode() instanceof SignalNode) {
                    SignalNode sn = (SignalNode) t.getStartNode();
                    if (sn.isRepeater()
                     && changed.getId().equals(sn.getRepeatsSignalId())) {
                        repeaters.putIfAbsent(sn.getId(), sn);
                    }
                }
                if (t.getEndNode() instanceof SignalNode) {
                    SignalNode sn = (SignalNode) t.getEndNode();
                    if (sn.isRepeater()
                     && changed.getId().equals(sn.getRepeatsSignalId())) {
                        repeaters.putIfAbsent(sn.getId(), sn);
                    }
                }
            }
        }

        return repeaters;
    }

    // Every state change must be logged — SAFETY.md S-09
    private void log(SignalNode signal, SignalState from,
                      SignalState to, String actor) {
        System.out.println("[SIGNAL] "
            + java.time.LocalDateTime.now()
            + " | " + signal.getId()
            + " | " + from + " → " + to
            + " | by: " + actor
            + " | protects: " + signal.getProtectsTrackId());
    }
}
