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
        testJunctionTrailingFromDivergingDerailsWhenInactive();
        testJunctionTrailingFromStraightDerailsWhenActive();
        testJunctionFacingMovementRoutesByState();
        testJunctionSetStateRejectedWhenIsolated();
        testJunctionReleaseAfterClearanceUsesFoulingDistance();
        testConflictDetectorOverlapEnvelopeBlocksUnsafeRoute();
        testSignalRedCascadesToRepeater();
        testReservationRejectsInvalidTimeWindow();
        testReservationWrongTrainCannotRelease();
        testTimeWindowTouchingBoundaryNoConflict();
        testOverlapEnvelopeAtExactBoundaryPasses();
        testSignalRequestGreenFailureCascadesRedToRepeater();
        testMeetAndPassDelaysSameDirectionOverlap();
        testDependencyResolverOrdersBlockedTrainAfterHolder();
        testAssessHeadOnSetsDenialReasonAndBlocker();
        testAssessFollowingSetsDenialReasonAndBlocker();
        testAssessRejectsWhenJunctionStillLockedAfterShortClearance();
        testAccidentLikeWrongRouteIntoShortOccupiedEnvelopeBlocked();
        testAccidentLikeSingleLineOppositeMovementsSecondTrainHeld();

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

        long now = java.time.Instant.now().getEpochSecond();
        boolean initiallyFree = !t.isInUse() && t.getActiveReservation() == null;

        t.reserve("TRAIN_A", enums.Direction.FORWARD, now, now + 10);
        boolean afterReserve = t.isInUse()
            && t.getActiveReservation() != null
            && t.getActiveReservation().trainId().equals("TRAIN_A")
            && t.getActiveReservation().direction() == enums.Direction.FORWARD;

        boolean secondReservationRejected = false;
        try {
            t.reserve("TRAIN_B", enums.Direction.REVERSE, now, now + 20);
        } catch (IllegalStateException ex) {
            secondReservationRejected = true;
        }

        t.release("TRAIN_A");
        boolean afterRelease = !t.isInUse()
            && t.getActiveReservation() == null;

        printResult("testTrackReserveAndRelease",
            initiallyFree && afterReserve
            && secondReservationRejected && afterRelease);
    }

    // -- TEST 6 -------------------------------
    static void testJunctionIsolation() {
        model.JunctionNode jct = new model.JunctionNode(
            "J1", "Junction 1", "FACING_NODE", "DIVERGING_NODE");

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
        long now = java.time.Instant.now().getEpochSecond();
        t.reserve("TRAIN_A", enums.Direction.FORWARD, now, now + 10);

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
        long now = java.time.Instant.now().getEpochSecond();
        t.reserve("TRAIN_A", enums.Direction.FORWARD, now, now + 10);

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
        core.MovementContext ctx = new core.MovementContext(
            "TRAIN_A", curr.getId(), new java.util.HashMap<>());
        ctx.setOverlapClear(true);
        ctx.setRequiredClearanceMetres(0.0);
        ctx.setAvailableClearanceMetres(1000.0);
        ctx.setSafeToProceed(true);

        boolean pass = rule.canSetGreen(curr, next, tv, null, ctx);
        printResult("testSignalRuleAllConditionsPass", pass);
    }

    // -- TEST 16 ------------------------------
    static void testSignalRuleFailsIfTrackInUse() {
        model.SignalNode s1 = new model.SignalNode("S1", "S1");
        model.SignalNode s2 = new model.SignalNode("S2", "S2");
        model.Track curr = new model.Track("T1", "T1", s1, s2, 100, 10, 30);
        long now = java.time.Instant.now().getEpochSecond();
        curr.reserve("OTHER_TRAIN", enums.Direction.REVERSE, now, now + 10);
        model.TrackTraversal tv =
            new model.TrackTraversal(curr, enums.Direction.FORWARD);
        signal.SignalRule rule = new signal.SignalRule();
        core.MovementContext ctx = new core.MovementContext(
            "TRAIN_A", curr.getId(), new java.util.HashMap<>());
        ctx.setOverlapClear(true);
        ctx.setRequiredClearanceMetres(0.0);
        ctx.setAvailableClearanceMetres(1000.0);
        ctx.setSafeToProceed(true);

        boolean pass = !rule.canSetGreen(curr, null, tv, null, ctx);
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

    // -- TEST 23 ------------------------------
    static void testJunctionTrailingFromDivergingDerailsWhenInactive() {
        model.SignalNode facing = new model.SignalNode("F","Facing");
        model.SignalNode diverging = new model.SignalNode("D","Diverging");
        model.JunctionNode jct = new model.JunctionNode(
            "JCT", "Junction", facing.getId(), diverging.getId());
        jct.setState(false);

        model.Track in = new model.Track("IN_D","IN_D",
            diverging, jct, 100, 10, 30);
        model.Track out = new model.Track("OUT_F","OUT_F",
            jct, facing, 100, 10, 30);
        model.TrackTraversal tv = new model.TrackTraversal(
            in, enums.Direction.FORWARD);

        model.Train train = new model.Train("TRD", "TrainD",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, diverging.getId(), facing.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));

        core.ConflictDetector cd = new core.ConflictDetector();
        boolean safe = cd.check(in, out, tv, jct, train);
        printResult("testJunctionTrailingFromDivergingDerailsWhenInactive",
            !safe);
    }

    // -- TEST 24 ------------------------------
    static void testJunctionTrailingFromStraightDerailsWhenActive() {
        model.SignalNode facing = new model.SignalNode("F2","Facing2");
        model.SignalNode straight = new model.SignalNode("S2","Straight2");
        model.SignalNode diverging = new model.SignalNode("D2","Diverging2");
        model.JunctionNode jct = new model.JunctionNode(
            "JCT2", "Junction2", facing.getId(), diverging.getId());
        jct.setState(true);

        model.Track in = new model.Track("IN_S","IN_S",
            straight, jct, 100, 10, 30);
        model.Track out = new model.Track("OUT_F2","OUT_F2",
            jct, facing, 100, 10, 30);
        model.TrackTraversal tv = new model.TrackTraversal(
            in, enums.Direction.FORWARD);

        model.Train train = new model.Train("TRS", "TrainS",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, straight.getId(), facing.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));

        core.ConflictDetector cd = new core.ConflictDetector();
        boolean safe = cd.check(in, out, tv, jct, train);
        printResult("testJunctionTrailingFromStraightDerailsWhenActive",
            !safe);
    }

    // -- TEST 25 ------------------------------
    static void testJunctionFacingMovementRoutesByState() {
        model.SignalNode facing = new model.SignalNode("F3", "Facing3");
        model.SignalNode straight = new model.SignalNode("S3", "Straight3");
        model.SignalNode diverging = new model.SignalNode("D3", "Diverging3");
        model.JunctionNode jct = new model.JunctionNode(
            "JCT3", "Junction3", facing.getId(), diverging.getId());

        model.Track in = new model.Track("IN_F", "IN_F",
            facing, jct, 100, 10, 30);
        model.Track outStraight = new model.Track("OUT_S", "OUT_S",
            jct, straight, 100, 10, 30);
        model.Track outDiverging = new model.Track("OUT_D", "OUT_D",
            jct, diverging, 100, 10, 30);

        core.GraphBuilder g = new core.GraphBuilder();
        g.addTrack(in);
        g.addTrack(outStraight);
        g.addTrack(outDiverging);

        jct.setState(false);
        model.Train toStraight = new model.Train("TRF1", "FacingToStraight",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, facing.getId(), straight.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));
        java.util.List<model.TrackTraversal> pathStraight =
            new core.PathFinder(toStraight, g).findPath();

        jct.setState(true);
        model.Train toDiverging = new model.Train("TRF2", "FacingToDiverging",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, facing.getId(), diverging.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));
        java.util.List<model.TrackTraversal> pathDiverging =
            new core.PathFinder(toDiverging, g).findPath();

        boolean pass = pathStraight.size() == 2
            && pathStraight.get(1).getTrack().getId().equals("OUT_S")
            && pathDiverging.size() == 2
            && pathDiverging.get(1).getTrack().getId().equals("OUT_D");

        printResult("testJunctionFacingMovementRoutesByState", pass);
    }

    // -- TEST 26 ------------------------------
    static void testJunctionSetStateRejectedWhenIsolated() {
        model.JunctionNode jct = new model.JunctionNode(
            "J_LOCK", "JunctionLock", "F", "D");
        jct.isolate("TRAIN_X");

        boolean rejected = false;
        try {
            jct.setState(true);
        } catch (IllegalStateException ex) {
            rejected = true;
        }

        printResult("testJunctionSetStateRejectedWhenIsolated", rejected);
    }

    // -- TEST 27 ------------------------------
    static void testJunctionReleaseAfterClearanceUsesFoulingDistance() {
        model.JunctionNode jct = new model.JunctionNode(
            "J_FOUL", "JunctionFouling", "F", "D");
        jct.setFoulingDistanceMetres(120.0);
        jct.lockForRoute("TRAIN_F");

        jct.releaseAfterClearance("TRAIN_F", 50.0);
        boolean stillLocked = jct.isIsolated();

        jct.releaseAfterClearance("TRAIN_F", 130.0);
        boolean released = !jct.isIsolated();

        printResult("testJunctionReleaseAfterClearanceUsesFoulingDistance",
            stillLocked && released);
    }

    // -- TEST 28 ------------------------------
    static void testConflictDetectorOverlapEnvelopeBlocksUnsafeRoute() {
        model.SignalNode s1 = new model.SignalNode("S_OV1", "S_OV1");
        model.SignalNode s2 = new model.SignalNode("S_OV2", "S_OV2");
        model.SignalNode s3 = new model.SignalNode("S_OV3", "S_OV3");

        model.Track current = new model.Track("T_OV1", "T_OV1",
            s1, s2, 100, 10, 30);
        model.Track next = new model.Track("T_OV2", "T_OV2",
            s2, s3, 60, 10, 30);
        current.setOverlapMetres(180.0);

        model.TrackTraversal traversal =
            new model.TrackTraversal(current, enums.Direction.FORWARD);
        model.Train train = new model.Train("TR_OV", "OverlapTrain",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, s1.getId(), s3.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));

        core.ConflictDetector cd = new core.ConflictDetector();
        core.MovementContext ctx = cd.assess(current, next,
            traversal, null, train);

        boolean pass = !ctx.isSafeToProceed()
            && "OVERLAP_NOT_CLEAR".equals(ctx.getDenialReason())
            && ctx.getRequiredClearanceMetres() > ctx.getAvailableClearanceMetres();
        printResult("testConflictDetectorOverlapEnvelopeBlocksUnsafeRoute",
            pass);
    }

    // -- TEST 29 ------------------------------
    static void testSignalRedCascadesToRepeater() {
        model.SignalNode parent = new model.SignalNode(
            "S_PARENT", "Parent", enums.SignalType.HOME);
        model.SignalNode repeater = new model.SignalNode(
            "S_REP", "Repeater", enums.SignalType.REPEATING);
        repeater.setRepeatsSignalId(parent.getId());

        model.SignalNode unrelated = new model.SignalNode(
            "S_OTHER", "Other", enums.SignalType.REPEATING);
        unrelated.setRepeatsSignalId("DIFFERENT_PARENT");

        parent.setState(model.SignalState.GREEN);
        repeater.setState(model.SignalState.GREEN);
        unrelated.setState(model.SignalState.GREEN);

        model.Track pToRep = new model.Track("T_PR", "T_PR",
            parent, repeater, 100, 10, 30);
        model.Track pToOther = new model.Track("T_PO", "T_PO",
            parent, unrelated, 100, 10, 30);

        core.GraphBuilder g = new core.GraphBuilder();
        g.addTrack(pToRep);
        g.addTrack(pToOther);

        signal.SignalController controller = new signal.SignalController();
        controller.setRed(parent, g);

        boolean pass = parent.getState() == model.SignalState.RED
            && repeater.getState() == model.SignalState.RED
            && unrelated.getState() == model.SignalState.GREEN;
        printResult("testSignalRedCascadesToRepeater", pass);
    }

    // -- TEST 30 ------------------------------
    static void testReservationRejectsInvalidTimeWindow() {
        boolean rejected = false;
        try {
            model.Reservation ignored = new model.Reservation(
                "TR_INV", enums.Direction.FORWARD,
                200L, 100L);
            ignored.trainId();
        } catch (IllegalArgumentException ex) {
            rejected = true;
        }
        printResult("testReservationRejectsInvalidTimeWindow", rejected);
    }

    // -- TEST 31 ------------------------------
    static void testReservationWrongTrainCannotRelease() {
        model.SignalNode a = new model.SignalNode("R1A", "R1A");
        model.SignalNode b = new model.SignalNode("R1B", "R1B");
        model.Track t = new model.Track("R1T", "R1T", a, b, 100, 10, 30);
        long now = java.time.Instant.now().getEpochSecond();
        t.reserve("TRAIN_OWNER", enums.Direction.FORWARD, now, now + 10);

        t.release("OTHER_TRAIN");
        boolean stillReserved = t.isInUse()
            && t.getActiveReservation() != null
            && "TRAIN_OWNER".equals(t.getActiveReservation().trainId());
        printResult("testReservationWrongTrainCannotRelease", stillReserved);
    }

    // -- TEST 32 ------------------------------
    static void testTimeWindowTouchingBoundaryNoConflict() {
        java.time.LocalDateTime base =
            java.time.LocalDateTime.of(2026, 6, 25, 9, 0, 0);
        model.TrackInterval a = new model.TrackInterval(
            "TA", "TBND", base, base.plusMinutes(10),
            enums.Direction.FORWARD);
        model.TrackInterval b = new model.TrackInterval(
            "TB", "TBND", base.plusMinutes(10), base.plusMinutes(20),
            enums.Direction.FORWARD);

        conflict.TimeWindowConflict twc = new conflict.TimeWindowConflict();
        printResult("testTimeWindowTouchingBoundaryNoConflict",
            !twc.hasConflict(a, b));
    }

    // -- TEST 33 ------------------------------
    static void testOverlapEnvelopeAtExactBoundaryPasses() {
        model.SignalNode s1 = new model.SignalNode("OE1", "OE1");
        model.SignalNode s2 = new model.SignalNode("OE2", "OE2");
        model.SignalNode s3 = new model.SignalNode("OE3", "OE3");
        model.Track current = new model.Track("OEC", "OEC", s1, s2, 100, 10, 30);
        model.Track next = new model.Track("OEN", "OEN", s2, s3, 180, 10, 30);
        current.setOverlapMetres(180.0);

        model.Train train = new model.Train("TR_OE", "TR_OE",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, s1.getId(), s3.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));
        model.TrackTraversal tv = new model.TrackTraversal(
            current, enums.Direction.FORWARD);

        core.ConflictDetector cd = new core.ConflictDetector();
        core.MovementContext ctx = cd.assess(current, next, tv, null, train);
        boolean pass = ctx.isSafeToProceed()
            && ctx.getRequiredClearanceMetres() == 180.0
            && ctx.getAvailableClearanceMetres() == 180.0;
        printResult("testOverlapEnvelopeAtExactBoundaryPasses", pass);
    }

    // -- TEST 34 ------------------------------
    static void testSignalRequestGreenFailureCascadesRedToRepeater() {
        model.SignalNode parent = new model.SignalNode(
            "SG_PARENT", "SG_PARENT", enums.SignalType.HOME);
        model.SignalNode repeater = new model.SignalNode(
            "SG_REP", "SG_REP", enums.SignalType.REPEATING);
        repeater.setRepeatsSignalId(parent.getId());
        model.SignalNode n3 = new model.SignalNode("SG_N3", "SG_N3");

        model.Track current = new model.Track("SG_CUR", "SG_CUR",
            parent, n3, 100, 10, 30);
        model.Track extra = new model.Track("SG_REP_LINK", "SG_REP_LINK",
            parent, repeater, 100, 10, 30);
        model.Track next = new model.Track("SG_NEXT", "SG_NEXT",
            n3, new model.SignalNode("SG_N4", "SG_N4"), 200, 10, 30);

        core.GraphBuilder g = new core.GraphBuilder();
        g.addTrack(current);
        g.addTrack(extra);
        g.addTrack(next);

        parent.setState(model.SignalState.GREEN);
        repeater.setState(model.SignalState.GREEN);

        model.TrackTraversal tv = new model.TrackTraversal(current,
            enums.Direction.FORWARD);
        core.MovementContext ctx = new core.MovementContext(
            "TR_SG", current.getId(), new java.util.HashMap<>());
        // Force rule failure by marking overlap not clear.
        ctx.setOverlapClear(false);
        ctx.setRequiredClearanceMetres(200.0);
        ctx.setAvailableClearanceMetres(0.0);

        signal.SignalController sc = new signal.SignalController();
        boolean cleared = sc.requestGreen(parent, current, next, tv,
            null, ctx, g);

        boolean pass = !cleared
            && parent.getState() == model.SignalState.RED
            && repeater.getState() == model.SignalState.RED;
        printResult("testSignalRequestGreenFailureCascadesRedToRepeater",
            pass);
    }

    // -- TEST 35 ------------------------------
    static void testMeetAndPassDelaysSameDirectionOverlap() {
        java.time.LocalDateTime dep = java.time.LocalDateTime.of(
            2026, 6, 25, 10, 0, 0);
        model.Train a = new model.Train("MPA", "MPA",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, "N1", "N2", 0, dep, dep.plusMinutes(20));
        model.Train b = new model.Train("MPB", "MPB",
            enums.TrainType.PASSENGER, model.TrainPriority.LOCAL,
            null, "N1", "N2", 0, dep.plusMinutes(1), dep.plusMinutes(25));

        java.util.Map<String, java.util.List<model.TrackInterval>> intervals =
            new java.util.HashMap<>();
        intervals.put("MPA", java.util.Arrays.asList(new model.TrackInterval(
            "MPA", "TRACK_MP", dep, dep.plusMinutes(10),
            enums.Direction.FORWARD)));
        intervals.put("MPB", java.util.Arrays.asList(new model.TrackInterval(
            "MPB", "TRACK_MP", dep.plusMinutes(2), dep.plusMinutes(12),
            enums.Direction.FORWARD)));

        scheduler.MeetAndPassResolver resolver =
            new scheduler.MeetAndPassResolver();
        resolver.resolve(java.util.Arrays.asList(a, b), intervals);

        boolean delayed = b.getDepartureTime().isAfter(dep.plusMinutes(1));
        printResult("testMeetAndPassDelaysSameDirectionOverlap", delayed);
    }

    // -- TEST 36 ------------------------------
    static void testDependencyResolverOrdersBlockedTrainAfterHolder() {
        model.SignalNode s1 = new model.SignalNode("DR1", "DR1");
        model.SignalNode s2 = new model.SignalNode("DR2", "DR2");
        model.Track t = new model.Track("DRT", "DRT", s1, s2, 100, 10, 30);
        long now = java.time.Instant.now().getEpochSecond();
        t.reserve("HOLDER", enums.Direction.FORWARD, now, now + 100);

        core.GraphBuilder g = new core.GraphBuilder();
        g.addTrack(t);

        model.Train holder = new model.Train("HOLDER", "HOLDER",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            t.getId(), s1.getId(), s2.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));
        model.Train blocked = new model.Train("BLOCKED", "BLOCKED",
            enums.TrainType.PASSENGER, model.TrainPriority.LOCAL,
            null, s1.getId(), s2.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));

        java.util.Map<String, java.util.List<model.TrackTraversal>> paths =
            new java.util.HashMap<>();
        paths.put("HOLDER", java.util.Arrays.asList(
            new model.TrackTraversal(t, enums.Direction.FORWARD)));
        paths.put("BLOCKED", java.util.Arrays.asList(
            new model.TrackTraversal(t, enums.Direction.FORWARD)));

        scheduler.DependencyResolver dr = new scheduler.DependencyResolver(g);
        java.util.List<model.Train> ordered = dr.resolve(
            java.util.Arrays.asList(holder, blocked), paths);

        boolean pass = !ordered.isEmpty()
            && "HOLDER".equals(ordered.get(0).getId());
        printResult("testDependencyResolverOrdersBlockedTrainAfterHolder",
            pass);
    }

    // -- TEST 37 ------------------------------
    static void testAssessHeadOnSetsDenialReasonAndBlocker() {
        model.SignalNode a = new model.SignalNode("AH1", "AH1");
        model.SignalNode b = new model.SignalNode("AH2", "AH2");
        model.Track track = new model.Track("AHT", "AHT", a, b, 100, 10, 30);
        long now = java.time.Instant.now().getEpochSecond();
        track.reserve("TRAIN_OCC", enums.Direction.FORWARD, now, now + 10);

        model.Train candidate = new model.Train("TRAIN_NEW", "TRAIN_NEW",
            enums.TrainType.PASSENGER, model.TrainPriority.LOCAL,
            null, a.getId(), b.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));
        model.TrackTraversal tv = new model.TrackTraversal(track,
            enums.Direction.REVERSE);

        core.ConflictDetector cd = new core.ConflictDetector();
        core.MovementContext ctx = cd.assess(track, null, tv, null, candidate);
        boolean pass = !ctx.isSafeToProceed()
            && "HEAD_ON_CONFLICT".equals(ctx.getDenialReason())
            && "TRAIN_OCC".equals(ctx.getBlockerTrainId());
        printResult("testAssessHeadOnSetsDenialReasonAndBlocker", pass);
    }

    // -- TEST 38 ------------------------------
    static void testAssessFollowingSetsDenialReasonAndBlocker() {
        model.SignalNode a = new model.SignalNode("AF1", "AF1");
        model.SignalNode b = new model.SignalNode("AF2", "AF2");
        model.Track track = new model.Track("AFT", "AFT", a, b, 100, 10, 30);
        long now = java.time.Instant.now().getEpochSecond();
        track.reserve("TRAIN_OCC2", enums.Direction.FORWARD, now, now + 10);

        model.Train candidate = new model.Train("TRAIN_NEW2", "TRAIN_NEW2",
            enums.TrainType.PASSENGER, model.TrainPriority.LOCAL,
            null, a.getId(), b.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));
        model.TrackTraversal tv = new model.TrackTraversal(track,
            enums.Direction.FORWARD);

        core.ConflictDetector cd = new core.ConflictDetector();
        core.MovementContext ctx = cd.assess(track, null, tv, null, candidate);
        boolean pass = !ctx.isSafeToProceed()
            && "FOLLOWING_CONFLICT".equals(ctx.getDenialReason())
            && "TRAIN_OCC2".equals(ctx.getBlockerTrainId());
        printResult("testAssessFollowingSetsDenialReasonAndBlocker", pass);
    }

    // -- TEST 39 ------------------------------
    static void testAssessRejectsWhenJunctionStillLockedAfterShortClearance() {
        model.SignalNode in = new model.SignalNode("JL_IN", "JL_IN");
        model.SignalNode out = new model.SignalNode("JL_OUT", "JL_OUT");
        model.JunctionNode jct = new model.JunctionNode(
            "JL_J", "JL_J", in.getId(), out.getId());
        jct.setFoulingDistanceMetres(500.0);

        model.Track t = new model.Track("JL_T", "JL_T", in, jct, 100, 10, 30);
        model.Track next = new model.Track("JL_N", "JL_N", jct, out, 100, 10, 30);
        model.TrackTraversal tv = new model.TrackTraversal(t,
            enums.Direction.FORWARD);
        model.Train tr = new model.Train("JL_TR", "JL_TR",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, in.getId(), out.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));

        core.ConflictDetector cd = new core.ConflictDetector();
        cd.onTrackEntry(tr, t, tv, jct);
        cd.onTrackExit(tr, t, jct); // clears only 100m; fouling needs 500m

        core.MovementContext ctx2 = cd.assess(t, next, tv, jct, tr);
        boolean pass = !ctx2.isSafeToProceed()
            && "JUNCTION_ISOLATED".equals(ctx2.getDenialReason());
        printResult("testAssessRejectsWhenJunctionStillLockedAfterShortClearance",
            pass);
    }

    // -- TEST 40 ------------------------------
    static void testAccidentLikeWrongRouteIntoShortOccupiedEnvelopeBlocked() {
        model.SignalNode home = new model.SignalNode("AC_HOME", "AC_HOME");
        model.SignalNode jnode = new model.SignalNode("AC_J", "AC_J");
        model.SignalNode loop = new model.SignalNode("AC_LOOP", "AC_LOOP");

        model.Track approach = new model.Track("AC_APP", "AC_APP",
            home, jnode, 120, 10, 30);
        model.Track loopEntry = new model.Track("AC_LOOP_E", "AC_LOOP_E",
            jnode, loop, 80, 10, 30);
        approach.setOverlapMetres(180.0);

        long now = java.time.Instant.now().getEpochSecond();
        loopEntry.reserve("STABLED_RAKE", enums.Direction.FORWARD,
            now, now + 600);

        model.Train incoming = new model.Train("AC_TR", "AC_TR",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, home.getId(), loop.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));
        model.TrackTraversal tv = new model.TrackTraversal(approach,
            enums.Direction.FORWARD);

        core.ConflictDetector cd = new core.ConflictDetector();
        core.MovementContext ctx = cd.assess(approach, loopEntry,
            tv, null, incoming);
        boolean pass = !ctx.isSafeToProceed()
            && "OVERLAP_NOT_CLEAR".equals(ctx.getDenialReason());
        printResult("testAccidentLikeWrongRouteIntoShortOccupiedEnvelopeBlocked",
            pass);
    }

    // -- TEST 41 ------------------------------
    static void testAccidentLikeSingleLineOppositeMovementsSecondTrainHeld() {
        model.SignalNode a = new model.SignalNode("SL_A", "SL_A");
        model.SignalNode b = new model.SignalNode("SL_B", "SL_B");
        model.Track single = new model.Track("SL_T", "SL_T", a, b, 300, 10, 30);
        long now = java.time.Instant.now().getEpochSecond();
        single.reserve("TRAIN_UP", enums.Direction.FORWARD, now, now + 100);

        model.Train downTrain = new model.Train("TRAIN_DOWN", "TRAIN_DOWN",
            enums.TrainType.PASSENGER, model.TrainPriority.EXPRESS,
            null, b.getId(), a.getId(), 0,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now().plusHours(1));
        model.TrackTraversal reverse = new model.TrackTraversal(single,
            enums.Direction.REVERSE);

        core.ConflictDetector cd = new core.ConflictDetector();
        core.MovementContext ctx = cd.assess(single, null,
            reverse, null, downTrain);
        boolean pass = !ctx.isSafeToProceed()
            && "HEAD_ON_CONFLICT".equals(ctx.getDenialReason());
        printResult("testAccidentLikeSingleLineOppositeMovementsSecondTrainHeld",
            pass);
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
