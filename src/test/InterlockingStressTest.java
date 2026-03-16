package test;

import core.ConflictDetector;
import core.GraphBuilder;
import core.MovementContext;
import enums.Direction;
import enums.SignalType;
import enums.TrainType;
import java.time.LocalDateTime;
import java.util.Random;
import model.JunctionNode;
import model.SignalNode;
import model.SignalState;
import model.Track;
import model.TrackInterval;
import model.TrackTraversal;
import model.Train;
import model.TrainPriority;
import signal.SignalController;

public class InterlockingStressTest {

    private static int passCount = 0;
    private static int failCount = 0;

    public static void main(String[] args) {
        System.out.println("=== INTERLOCKING STRESS TEST SUITE ===\n");

        stressRandomTimeWindowConflicts(2500);
        stressRandomReservationExclusivity(2000);
        stressRandomHeadOnAndFollowingDecisions(1200);
        stressRandomOverlapEnvelopeDecisions(1000);
        stressRandomSignalCascadeConsistency(500);

        System.out.println("\n=== STRESS TEST SUMMARY ===");
        System.out.println("PASS: " + passCount);
        System.out.println("FAIL: " + failCount);
        if (failCount == 0) {
            System.out.println("RESULT: PASS");
        } else {
            System.out.println("RESULT: FAIL");
            System.exit(1);
        }
    }

    private static void stressRandomTimeWindowConflicts(int rounds) {
        Random rnd = new Random(20260316L);
        conflict.TimeWindowConflict twc = new conflict.TimeWindowConflict();

        for (int i = 0; i < rounds; i++) {
            LocalDateTime base = LocalDateTime.of(2026, 6, 25, 8, 0, 0)
                .plusSeconds(rnd.nextInt(7200));

            int aLen = 1 + rnd.nextInt(900);
            int bOffset = rnd.nextInt(1200) - 300;
            int bLen = 1 + rnd.nextInt(900);

            String trackA = "T" + rnd.nextInt(5);
            String trackB = rnd.nextDouble() < 0.7 ? trackA : "T" + rnd.nextInt(5);

            TrackInterval a = new TrackInterval(
                "A" + i,
                trackA,
                base,
                base.plusSeconds(aLen),
                rnd.nextBoolean() ? Direction.FORWARD : Direction.REVERSE);

            LocalDateTime bEnter = base.plusSeconds(bOffset);
            TrackInterval b = new TrackInterval(
                "B" + i,
                trackB,
                bEnter,
                bEnter.plusSeconds(bLen),
                rnd.nextBoolean() ? Direction.FORWARD : Direction.REVERSE);

            boolean expected = trackA.equals(trackB)
                && a.getEnterTime().isBefore(b.getExitTime())
                && b.getEnterTime().isBefore(a.getExitTime());

            boolean actual = twc.hasConflict(a, b);
            assertCondition("stressRandomTimeWindowConflicts", expected == actual,
                "Mismatch at iteration " + i + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void stressRandomReservationExclusivity(int rounds) {
        Random rnd = new Random(9162026L);
        SignalNode a = new SignalNode("R_A", "R_A");
        SignalNode b = new SignalNode("R_B", "R_B");
        Track track = new Track("R_T", "R_T", a, b, 120, 10, 30);

        String owner = null;
        for (int i = 0; i < rounds; i++) {
            String trainId = "TR_" + rnd.nextInt(40);
            Direction dir = rnd.nextBoolean() ? Direction.FORWARD : Direction.REVERSE;
            long now = 1_000_000L + i;

            if (rnd.nextDouble() < 0.6) {
                boolean succeeded = true;
                try {
                    track.reserve(trainId, dir, now, now + 30);
                    owner = trainId;
                } catch (IllegalStateException ex) {
                    succeeded = false;
                }

                if (succeeded) {
                    assertCondition("stressRandomReservationExclusivity",
                        track.isInUse() && track.getActiveReservation() != null,
                        "Track should be in use after successful reserve");
                } else {
                    assertCondition("stressRandomReservationExclusivity",
                        track.getActiveReservation() != null,
                        "Reserve failed without active owner");
                }
            } else {
                track.release(trainId);
                if (owner != null && owner.equals(trainId)) {
                    owner = null;
                }

                if (owner == null) {
                    assertCondition("stressRandomReservationExclusivity",
                        !track.isInUse() && track.getActiveReservation() == null,
                        "Track should be free after owner release");
                } else {
                    assertCondition("stressRandomReservationExclusivity",
                        track.getActiveReservation() != null
                            && owner.equals(track.getActiveReservation().trainId()),
                        "Wrong train released active reservation");
                }
            }
        }
    }

    private static void stressRandomHeadOnAndFollowingDecisions(int rounds) {
        Random rnd = new Random(16032026L);
        ConflictDetector detector = new ConflictDetector();

        for (int i = 0; i < rounds; i++) {
            SignalNode start = new SignalNode("H_S_" + i, "H_S_" + i);
            SignalNode end = new SignalNode("H_E_" + i, "H_E_" + i);
            Track t = new Track("H_T_" + i, "H_T_" + i, start, end, 100, 10, 30);

            Direction occupiedDir = rnd.nextBoolean() ? Direction.FORWARD : Direction.REVERSE;
            Direction candidateDir = rnd.nextBoolean() ? Direction.FORWARD : Direction.REVERSE;

            long now = 2_000_000L + i;
            t.reserve("OCC_" + i, occupiedDir, now, now + 50);

            Train candidate = new Train(
                "NEW_" + i,
                "NEW_" + i,
                TrainType.PASSENGER,
                TrainPriority.LOCAL,
                null,
                start.getId(),
                end.getId(),
                0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1));

            TrackTraversal tr = new TrackTraversal(t, candidateDir);
            MovementContext ctx = detector.assess(t, null, tr, null, candidate);

            if (occupiedDir == candidateDir) {
                assertCondition("stressRandomHeadOnAndFollowingDecisions",
                    !ctx.isSafeToProceed()
                        && "FOLLOWING_CONFLICT".equals(ctx.getDenialReason()),
                    "Expected FOLLOWING_CONFLICT at iteration " + i);
            } else {
                assertCondition("stressRandomHeadOnAndFollowingDecisions",
                    !ctx.isSafeToProceed()
                        && "HEAD_ON_CONFLICT".equals(ctx.getDenialReason()),
                    "Expected HEAD_ON_CONFLICT at iteration " + i);
            }
        }
    }

    private static void stressRandomOverlapEnvelopeDecisions(int rounds) {
        Random rnd = new Random(320262026L);
        ConflictDetector detector = new ConflictDetector();

        for (int i = 0; i < rounds; i++) {
            SignalNode a = new SignalNode("O_A_" + i, "O_A_" + i);
            SignalNode b = new SignalNode("O_B_" + i, "O_B_" + i);
            SignalNode c = new SignalNode("O_C_" + i, "O_C_" + i);

            Track current = new Track("O_CUR_" + i, "O_CUR_" + i, a, b,
                80 + rnd.nextInt(220), 10, 30);
            Track next = new Track("O_NXT_" + i, "O_NXT_" + i, b, c,
                40 + rnd.nextInt(240), 10, 30);

            double overlap = 60 + rnd.nextInt(220);
            current.setOverlapMetres(overlap);

            JunctionNode junction = null;
            if (rnd.nextBoolean()) {
                junction = new JunctionNode("O_J_" + i, "O_J_" + i,
                    a.getId(), c.getId());
                double fouling = rnd.nextInt(250);
                junction.setFoulingDistanceMetres(fouling);
            }

            if (rnd.nextDouble() < 0.3) {
                long now = 3_000_000L + i;
                next.reserve("NEXT_OCC_" + i, Direction.FORWARD, now, now + 100);
            }

            Train t = new Train("O_TR_" + i, "O_TR_" + i,
                TrainType.PASSENGER, TrainPriority.EXPRESS,
                null, a.getId(), c.getId(), 0,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1));

            MovementContext ctx = detector.assess(
                current,
                next,
                new TrackTraversal(current, Direction.FORWARD),
                junction,
                t);

            double required = Math.max(overlap,
                junction == null ? 0.0 : junction.getFoulingDistanceMetres());
            double available = next.isInUse() ? 0.0 : next.getDistance();
            boolean expectedSafe = available >= required;

            assertCondition("stressRandomOverlapEnvelopeDecisions",
                ctx.isSafeToProceed() == expectedSafe,
                "Overlap envelope mismatch at iteration " + i
                    + " expectedSafe=" + expectedSafe
                    + " required=" + required
                    + " available=" + available);
        }
    }

    private static void stressRandomSignalCascadeConsistency(int rounds) {
        Random rnd = new Random(420262026L);
        SignalController controller = new SignalController();

        for (int i = 0; i < rounds; i++) {
            SignalNode parent = new SignalNode(
                "S_PARENT_" + i, "S_PARENT_" + i, SignalType.HOME);
            SignalNode rep1 = new SignalNode(
                "S_REP1_" + i, "S_REP1_" + i, SignalType.REPEATING);
            SignalNode rep2 = new SignalNode(
                "S_REP2_" + i, "S_REP2_" + i, SignalType.REPEATING);
            SignalNode other = new SignalNode(
                "S_OTHER_" + i, "S_OTHER_" + i, SignalType.REPEATING);

            rep1.setRepeatsSignalId(parent.getId());
            rep2.setRepeatsSignalId(parent.getId());
            other.setRepeatsSignalId("DIFFERENT_" + i);

            parent.setState(rnd.nextBoolean() ? SignalState.GREEN : SignalState.YELLOW);
            rep1.setState(SignalState.GREEN);
            rep2.setState(SignalState.DOUBLE_YELLOW);
            other.setState(SignalState.GREEN);

            GraphBuilder graph = new GraphBuilder();
            graph.addTrack(new Track("SC_T1_" + i, "SC_T1_" + i,
                parent, rep1, 50, 10, 30));
            graph.addTrack(new Track("SC_T2_" + i, "SC_T2_" + i,
                parent, rep2, 50, 10, 30));
            graph.addTrack(new Track("SC_T3_" + i, "SC_T3_" + i,
                parent, other, 50, 10, 30));

            controller.setRed(parent, graph);

            boolean pass = parent.getState() == SignalState.RED
                && rep1.getState() == SignalState.RED
                && rep2.getState() == SignalState.RED
                && other.getState() == SignalState.GREEN;

            assertCondition("stressRandomSignalCascadeConsistency", pass,
                "Cascade mismatch at iteration " + i);
        }
    }

    private static void assertCondition(String testName,
                                        boolean condition,
                                        String detail) {
        if (condition) {
            passCount++;
        } else {
            failCount++;
            System.out.println("FAIL - " + testName + " | " + detail);
        }
    }
}
