package signal;

import core.GraphBuilder;
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
            String         trainId,
            Map<String, String> blockedBy,
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
                               junction, trainId, blockedBy)) {
            setRed(signal);
            return false;
        }
        SignalState prev = signal.getState();
        signal.setState(SignalState.GREEN);
        log(signal, prev, SignalState.GREEN, trainId);
        propagateToRepeater(signal, SignalState.GREEN, graph);
        return true;
    }

    // Always safe to call. Reverts signal to RED.
    public void setRed(SignalNode signal) {
        SignalState prev = signal.getState();
        signal.setState(SignalState.RED);
        log(signal, prev, SignalState.RED, "SYSTEM");
    }

    // Set YELLOW — next signal is RED
    public void setYellow(SignalNode signal, String trainId) {
        SignalState prev = signal.getState();
        signal.setState(SignalState.YELLOW);
        log(signal, prev, SignalState.YELLOW, trainId);
    }

    // Set DOUBLE_YELLOW — next signal is YELLOW
    public void setDoubleYellow(SignalNode signal, String trainId) {
        SignalState prev = signal.getState();
        signal.setState(SignalState.DOUBLE_YELLOW);
        log(signal, prev, SignalState.DOUBLE_YELLOW, trainId);
    }

    // Validate train speed against track limits. Emergency RED if over max.
    public void validateSpeed(Train train, Track track,
                               SignalNode signal) {
        if (train.getSpeed() > track.getEffectiveMaxSpeed()) {
            System.out.println("EMERGENCY: " + train.getId()
                + " speed " + train.getSpeed()
                + " exceeds max " + track.getEffectiveMaxSpeed()
                + " on " + track.getId());
            setRed(signal);
        }
    }

    // Repeater must mirror its parent signal
    private void propagateToRepeater(SignalNode changed,
                                      SignalState newState,
                                      GraphBuilder graph) {
        // V1: iterate nodes and find REPEATING signals
        // whose repeatsSignalId == changed.getId()
        // Full implementation in V2 when node registry added to GraphBuilder
        for (List<Track> tracks : graph.getAdjacencyList().values()) {
            for (Track t : tracks) {
                if (t.getStartNode() instanceof SignalNode) {
                    SignalNode sn = (SignalNode) t.getStartNode();
                    if (sn.isRepeater()
                     && changed.getId().equals(sn.getRepeatsSignalId())) {
                        sn.setState(newState);
                        log(sn, SignalState.RED, newState, "REPEATER");
                    }
                }
                if (t.getEndNode() instanceof SignalNode) {
                    SignalNode sn = (SignalNode) t.getEndNode();
                    if (sn.isRepeater()
                     && changed.getId().equals(sn.getRepeatsSignalId())) {
                        sn.setState(newState);
                        log(sn, SignalState.RED, newState, "REPEATER");
                    }
                }
            }
        }
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
