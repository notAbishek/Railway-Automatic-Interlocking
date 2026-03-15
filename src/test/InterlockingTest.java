package test;

public class InterlockingTest {
    public static void main(String[] args) {
        System.out.println("=== INTERLOCKING TEST SUITE ===\n");

        testGraphBuilderAddsNodes();
        testPathFinderStraightLine();
        testPathFinderReverseDirection();
        testAllowedTypesFilter();
        testTrackReserveAndRelease();
        testJunctionIsolation();
        testHeadOnConflict();
        testFollowingConflict();
        testNoConflictWhenFree();
        testDeadlockDetectorCycle();
        testDeadlockDetectorNoCycle();
        testTimeWindowConflictOverlap();
        testTimeWindowNoConflict();
        testSignalDefaultRed();
        testSignalRuleAllConditionsPass();
        testSignalRuleFailsIfTrackInUse();
        testTSREffectiveSpeed();
        testOverlapDefault();
        testLastVehicleConfirmation();
        testShuntingResolverSimple();
        testDispatcherTimeOrderFirst();
        testDispatcherPriorityTiebreak();

        System.out.println("\n=== TEST SUITE COMPLETE ===");
    }

    // -- TEST 1 -------------------------------
    static void testGraphBuilderAddsNodes() {
        model.SignalNode s1 = new model.SignalNode("S1", "Signal 1");
        model.SignalNode s2 = new model.SignalNode("S2", "Signal 2");
        model.Track t1 = new model.Track("T1", "Track 1", s1, s2, 100, 10, 30);
        core.GraphBuilder g = new core.GraphBuilder();
        g.addTrack(t1);

        boolean pass = g.getAdjacencyList().containsKey("S1")
                    && g.getAdjacencyList().containsKey("S2");
        printResult("testGraphBuilderAddsNodes", pass);
    }

    // -- TEST 2 -------------------------------
    static void testPathFinderStraightLine() {
        model.SignalNode s1 = new model.SignalNode("S1", "Signal 1");
        model.SignalNode s2 = new model.SignalNode("S2", "Signal 2");
        model.SignalNode s3 = new model.SignalNode("S3", "Signal 3");
        model.Track t1 = new model.Track("T1", "T1", s1, s2, 100, 10, 30);
        model.Track t2 = new model.Track("T2", "T2", s2, s3, 100, 10, 30);
        core.GraphBuilder g = new core.GraphBuilder();
        g.addTrack(t1);
        g.addTrack(t2);
        model.Train train = new model.Train("TR1", "Train1",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, "S1", "S3", 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));
        core.PathFinder pf = new core.PathFinder(train, g);
        java.util.List<model.TrackTraversal> path = pf.findPath();

        boolean pass = path.size() == 2
            && path.get(0).getTrack().getId().equals("T1")
            && path.get(1).getTrack().getId().equals("T2")
            && path.get(0).getDirection() == enums.Direction.FORWARD
            && path.get(1).getDirection() == enums.Direction.FORWARD;
        printResult("testPathFinderStraightLine", pass);
    }

    // -- TEST 3 -------------------------------
    static void testPathFinderReverseDirection() {
        model.SignalNode s1 = new model.SignalNode("S1", "S1");
        model.SignalNode s2 = new model.SignalNode("S2", "S2");
        model.SignalNode s3 = new model.SignalNode("S3", "S3");
        model.Track t1 = new model.Track("T1", "T1", s1, s2, 100, 10, 30);
        model.Track t2 = new model.Track("T2", "T2", s2, s3, 100, 10, 30);
        core.GraphBuilder g = new core.GraphBuilder();
        g.addTrack(t1);
        g.addTrack(t2);
        model.Train train = new model.Train("TR2", "Train2",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, "S3", "S1", 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));
        core.PathFinder pf = new core.PathFinder(train, g);
        java.util.List<model.TrackTraversal> path = pf.findPath();

        boolean pass = path.size() == 2
            && path.get(0).getTrack().getId().equals("T2")
            && path.get(1).getTrack().getId().equals("T1")
            && path.get(0).getDirection() == enums.Direction.REVERSE
            && path.get(1).getDirection() == enums.Direction.REVERSE;
        printResult("testPathFinderReverseDirection", pass);
    }

    // -- TEST 4 -------------------------------
    static void testAllowedTypesFilter() {
        model.SignalNode s1 = new model.SignalNode("S1", "S1");
        model.SignalNode s2 = new model.SignalNode("S2", "S2");
        model.Track t1 = new model.Track("T1", "T1", s1, s2, 100, 10, 30);
        t1.setAllowedTypes(
            java.util.Arrays.asList(enums.TrainType.PASSENGER));
        core.GraphBuilder g = new core.GraphBuilder();
        g.addTrack(t1);
        model.Train goodsTrain = new model.Train("TR3", "Goods",
            enums.TrainType.GOODS, model.TrainPriority.GOODS,
            null, "S1", "S2", 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));
        core.PathFinder pf = new core.PathFinder(goodsTrain, g);
        java.util.List<model.TrackTraversal> path = pf.findPath();

        printResult("testAllowedTypesFilter", path.isEmpty());
    }

    // -- TEST 5 -------------------------------
    static void testTrackReserveAndRelease() {
        model.SignalNode s1 = new model.SignalNode("S1", "S1");
        model.SignalNode s2 = new model.SignalNode("S2", "S2");
        model.Track t = new model.Track("T1", "T1", s1, s2, 100, 10, 30);

        boolean initiallyFree = !t.isInUse() && t.getUsedBy().isEmpty();

        t.reserve("TRAIN_A", enums.Direction.FORWARD);
        boolean afterReserve = t.isInUse()
            && t.getUsedBy().contains("TRAIN_A")
            && t.getOccupiedDirection() == enums.Direction.FORWARD;

        t.release("TRAIN_A");
        boolean afterRelease = !t.isInUse()
            && t.getUsedBy().isEmpty()
            && t.getOccupiedDirection() == null;

        printResult("testTrackReserveAndRelease",
            initiallyFree && afterReserve && afterRelease);
    }

    // -- TEST 6 -------------------------------
    static void testJunctionIsolation() {
        model.JunctionNode jct = new model.JunctionNode(
            "J1", "Junction 1", enums.JunctionDirection.RIGHT,
            "PRIMARY_NODE", "SECONDARY_NODE");

        boolean initiallyFree = !jct.isIsolated();
        jct.isolate("TRAIN_A");
        boolean isolatedAfterLock = jct.isIsolated();
        jct.release("TRAIN_A");
        boolean freeAfterRelease = !jct.isIsolated();

        printResult("testJunctionIsolation",
            initiallyFree && isolatedAfterLock && freeAfterRelease);
    }

    // -- TEST 7 -------------------------------
    static void testHeadOnConflict() {
        model.SignalNode s1 = new model.SignalNode("S1", "S1");
        model.SignalNode s2 = new model.SignalNode("S2", "S2");
        model.Track t = new model.Track("T1", "T1", s1, s2, 100, 10, 30);
        t.reserve("TRAIN_A", enums.Direction.FORWARD);

        model.TrackTraversal reverseTraversal =
            new model.TrackTraversal(t, enums.Direction.REVERSE);
        conflict.HeadOnConflict hoc = new conflict.HeadOnConflict();

        boolean detected = hoc.isHeadOn(t, reverseTraversal);
        printResult("testHeadOnConflict", detected);
    }

    // -- TEST 8 -------------------------------
    static void testFollowingConflict() {
        model.SignalNode s1 = new model.SignalNode("S1", "S1");
        model.SignalNode s2 = new model.SignalNode("S2", "S2");
        model.Track t = new model.Track("T1", "T1", s1, s2, 100, 10, 30);
        t.reserve("TRAIN_A", enums.Direction.FORWARD);

        model.TrackTraversal sameTraversal =
            new model.TrackTraversal(t, enums.Direction.FORWARD);
        conflict.FollowingConflict fc = new conflict.FollowingConflict();

        boolean detected = fc.isFollowing(t, sameTraversal);
        printResult("testFollowingConflict", detected);
    }

    // -- TEST 9 -------------------------------
    static void testNoConflictWhenFree() {
        model.SignalNode s1 = new model.SignalNode("S1", "S1");
        model.SignalNode s2 = new model.SignalNode("S2", "S2");
        model.Track t = new model.Track("T1", "T1", s1, s2, 100, 10, 30);
        model.TrackTraversal tr =
            new model.TrackTraversal(t, enums.Direction.FORWARD);

        conflict.HeadOnConflict hoc = new conflict.HeadOnConflict();
        conflict.FollowingConflict fc = new conflict.FollowingConflict();

        boolean pass = !hoc.isHeadOn(t, tr) && !fc.isFollowing(t, tr);
        printResult("testNoConflictWhenFree", pass);
    }

    // -- TEST 10 ------------------------------
    static void testDeadlockDetectorCycle() {
        java.util.Map<String, String> blockedBy = new java.util.HashMap<>();
        blockedBy.put("T1", "T2");
        blockedBy.put("T2", "T3");
        blockedBy.put("T3", "T1");
        conflict.DeadlockDetector dd = new conflict.DeadlockDetector();
        boolean cycleFound = dd.hasCycle("T1", blockedBy);
        printResult("testDeadlockDetectorCycle", cycleFound);
    }

    // -- TEST 11 ------------------------------
    static void testDeadlockDetectorNoCycle() {
        java.util.Map<String, String> blockedBy = new java.util.HashMap<>();
        blockedBy.put("T1", "T2");
        blockedBy.put("T2", "T3");
        conflict.DeadlockDetector dd = new conflict.DeadlockDetector();
        boolean cycleFound = dd.hasCycle("T1", blockedBy);
        printResult("testDeadlockDetectorNoCycle", !cycleFound);
    }

    // -- TEST 12 ------------------------------
    static void testTimeWindowConflictOverlap() {
        java.time.LocalDateTime base =
            java.time.LocalDateTime.of(2026, 6, 25, 8, 0, 0);
        model.TrackInterval a = new model.TrackInterval(
            "TRAIN_A", "T001",
            base,
            base.plusMinutes(20),
            enums.Direction.FORWARD);
        model.TrackInterval b = new model.TrackInterval(
            "TRAIN_B", "T001",
            base.plusMinutes(10),
            base.plusMinutes(30),
            enums.Direction.REVERSE);
        conflict.TimeWindowConflict twc = new conflict.TimeWindowConflict();
        printResult("testTimeWindowConflictOverlap", twc.hasConflict(a, b));
    }

    // -- TEST 13 ------------------------------
    static void testTimeWindowNoConflict() {
        java.time.LocalDateTime base =
            java.time.LocalDateTime.of(2026, 6, 25, 8, 0, 0);
        model.TrackInterval a = new model.TrackInterval(
            "TRAIN_A", "T001",
            base,
            base.plusMinutes(10),
            enums.Direction.FORWARD);
        model.TrackInterval b = new model.TrackInterval(
            "TRAIN_B", "T001",
            base.plusMinutes(15),
            base.plusMinutes(30),
            enums.Direction.REVERSE);
        conflict.TimeWindowConflict twc = new conflict.TimeWindowConflict();
        printResult("testTimeWindowNoConflict", !twc.hasConflict(a, b));
    }

    // -- TEST 14 ------------------------------
    static void testSignalDefaultRed() {
        model.SignalNode s = new model.SignalNode("S1", "Signal 1");
        boolean isRed = s.getState() == model.SignalState.RED;
        printResult("testSignalDefaultRed", isRed);
    }

    // -- TEST 15 ------------------------------
    static void testSignalRuleAllConditionsPass() {
        model.SignalNode s1 = new model.SignalNode("S1", "S1");
        model.SignalNode s2 = new model.SignalNode("S2", "S2");
        model.Track curr = new model.Track("T1", "T1", s1, s2, 100, 10, 30);
        model.Track next = new model.Track("T2", "T2", s2,
            new model.SignalNode("S3", "S3"), 100, 10, 30);
        model.TrackTraversal tv =
            new model.TrackTraversal(curr, enums.Direction.FORWARD);
        signal.SignalRule rule = new signal.SignalRule();

        boolean pass = rule.canSetGreen(curr, next, tv, null,
            "TRAIN_A", new java.util.HashMap<>());
        printResult("testSignalRuleAllConditionsPass", pass);
    }

    // -- TEST 16 ------------------------------
    static void testSignalRuleFailsIfTrackInUse() {
        model.SignalNode s1 = new model.SignalNode("S1", "S1");
        model.SignalNode s2 = new model.SignalNode("S2", "S2");
        model.Track curr = new model.Track("T1", "T1", s1, s2, 100, 10, 30);
        curr.reserve("OTHER_TRAIN", enums.Direction.REVERSE);
        model.TrackTraversal tv =
            new model.TrackTraversal(curr, enums.Direction.FORWARD);
        signal.SignalRule rule = new signal.SignalRule();

        boolean pass = !rule.canSetGreen(curr, null, tv, null,
            "TRAIN_A", new java.util.HashMap<>());
        printResult("testSignalRuleFailsIfTrackInUse", pass);
    }

    // -- TEST 17 ------------------------------
    static void testTSREffectiveSpeed() {
        model.SignalNode s1 = new model.SignalNode("S1", "S1");
        model.SignalNode s2 = new model.SignalNode("S2", "S2");
        model.Track t = new model.Track("T1", "T1", s1, s2, 100, 10, 50);

        boolean noTSR = t.getEffectiveMaxSpeed() == 50.0;

        t.setTemporarySpeedRestriction(20.0,
            java.time.LocalDateTime.now().plusHours(1));
        boolean withTSR = t.getEffectiveMaxSpeed() == 20.0;

        t.clearTemporarySpeedRestriction();
        boolean clearedTSR = t.getEffectiveMaxSpeed() == 50.0;

        printResult("testTSREffectiveSpeed", noTSR && withTSR && clearedTSR);
    }

    // -- TEST 18 ------------------------------
    static void testOverlapDefault() {
        model.SignalNode s1 = new model.SignalNode("S1", "S1");
        model.SignalNode s2 = new model.SignalNode("S2", "S2");
        model.Track t = new model.Track("T1", "T1", s1, s2, 100, 10, 30);
        printResult("testOverlapDefault", t.getOverlapMetres() == 180.0);
    }

    // -- TEST 19 ------------------------------
    static void testLastVehicleConfirmation() {
        model.Train train = new model.Train("T1", "Train1",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, "S1", "S4", 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));

        boolean notConfirmed = !train.isLastVehicleConfirmed();
        train.confirmLastVehicle();
        boolean confirmed = train.isLastVehicleConfirmed();
        train.resetLastVehicle();
        boolean reset = !train.isLastVehicleConfirmed();

        printResult("testLastVehicleConfirmation",
            notConfirmed && confirmed && reset);
    }

    // -- TEST 20 ------------------------------
    static void testShuntingResolverSimple() {
        java.util.Map<String, String> current = new java.util.HashMap<>();
        current.put("TRAIN_1", "TRACK_A");
        current.put("TRAIN_2", "TRACK_C");

        java.util.Map<String, String> goal = new java.util.HashMap<>();
        goal.put("TRAIN_1", "TRACK_C");
        goal.put("TRAIN_2", "TRACK_A");

        model.SignalNode nA = new model.SignalNode("TRACK_A", "A");
        model.SignalNode nB = new model.SignalNode("TRACK_B", "B");
        model.SignalNode nC = new model.SignalNode("TRACK_C", "C");
        model.Track tAB = new model.Track("T_AB", "AB", nA, nB, 50, 5, 20);
        model.Track tBC = new model.Track("T_BC", "BC", nB, nC, 50, 5, 20);
        core.GraphBuilder g = new core.GraphBuilder();
        g.addTrack(tAB);
        g.addTrack(tBC);

        conflict.ShuntingResolver sr = new conflict.ShuntingResolver();
        java.util.List<String> moves = sr.resolve(current, goal, g);

        printResult("testShuntingResolverSimple", !moves.isEmpty());
    }

    // -- TEST 21 ------------------------------
    static void testDispatcherTimeOrderFirst() {
        java.time.format.DateTimeFormatter fmt =
            core.Dispatcher.FORMAT;
        model.Train local = new model.Train("LOCAL", "Local Train",
            enums.TrainType.EMU, model.TrainPriority.LOCAL,
            null, "S1", "S4", 0,
            java.time.LocalDateTime.parse("25-06-2026 08:00:00", fmt),
            java.time.LocalDateTime.parse("25-06-2026 10:00:00", fmt));
        model.Train express = new model.Train("EXP", "Express Train",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, "S1", "S4", 0,
            java.time.LocalDateTime.parse("25-06-2026 20:00:00", fmt),
            java.time.LocalDateTime.parse("25-06-2026 22:00:00", fmt));
        core.Dispatcher d = new core.Dispatcher();
        d.addTrain(express);
        d.addTrain(local);
        model.Train first = d.dispatch();
        printResult("testDispatcherTimeOrderFirst",
            first.getId().equals("LOCAL"));
    }

    // -- TEST 22 ------------------------------
    static void testDispatcherPriorityTiebreak() {
        java.time.format.DateTimeFormatter fmt =
            core.Dispatcher.FORMAT;
        java.time.LocalDateTime sameTime =
            java.time.LocalDateTime.parse("25-06-2026 08:00:00", fmt);
        model.Train local = new model.Train("LOCAL", "Local",
            enums.TrainType.EMU, model.TrainPriority.LOCAL,
            null, "S1", "S4", 0, sameTime, sameTime.plusHours(2));
        model.Train express = new model.Train("EXP", "Express",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, "S1", "S4", 0, sameTime, sameTime.plusHours(2));
        core.Dispatcher d = new core.Dispatcher();
        d.addTrain(local);
        d.addTrain(express);
        model.Train first = d.dispatch();
        printResult("testDispatcherPriorityTiebreak",
            first.getId().equals("EXP"));
    }

    // -- HELPER -------------------------------
    static void printResult(String testName, boolean pass) {
        System.out.println((pass ? "PASS" : "FAIL") + " - " + testName);
    }
}
