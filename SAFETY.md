# SAFETY.md — Interlocking System Safety Principles

> This document defines every safety rule this system must never violate.
> Code changes, feature additions, and refactors must be verified against
> every principle listed here before merging.
>
> One rule broken = system is unsafe. No exceptions.

---

## Why This Document Exists

On June 2, 2023, the Coromandel Express was given a green signal on the
main line but was wrongly switched to a loop line via a change in
electronic interlocking state. 296 people died.

The root cause: a human changed the interlocking state without the
safety verification logic catching it. There was no software layer
that said "verify before acting."

This system is designed so that scenario is architecturally impossible.
Every signal state change passes through ConflictDetector first.
Always. No exceptions. No shortcuts.

---

## Part 1 — Fundamental Safety Principles

### S-01 — Fail-Safe Default
```
Every signal defaults to RED on initialization.
Every signal reverts to RED when:
  - the train clears the block
  - the system loses track of a train's position
  - any verification step fails or throws an exception
  - the system restarts

GREEN is a privilege granted after verification.
RED is the natural resting state.

Code rule:
  SignalNode constructor always sets state = SignalState.RED
  No signal is ever constructed in GREEN or YELLOW state
  setGreen() only called by SignalController, never directly
  SignalController only called after ConflictDetector.check() returns true
```

### S-02 — No Signal Changes Without Verification
```
The chain must never be bypassed:

  ALLOWED:
  ConflictDetector.check() → true → SignalController.setGreen()

  FORBIDDEN:
  signal.setState(GREEN)              ← direct call, no verification
  signalController.setGreen(signal)   ← bypasses ConflictDetector

  This is exactly what caused the Balasore accident.
  The EI state was changed without the safety check catching it.

Code rule:
  SignalNode.setState() is package-private within signal/
  Only SignalController can call it
  SignalController is only called by ConflictDetector result
```

### S-03 — Track Ownership Is Exclusive
```
At any moment, a track can be reserved by ONE direction only.

  track.occupiedDirection = RIGHT → no LEFT train can enter
  track.occupiedDirection = LEFT  → no RIGHT train can enter
  track.occupiedDirection = null  → track is free

Head-on collision is impossible if this rule holds.

Code rule:
  track.reserve(trainId, direction) checks occupiedDirection first
  If occupiedDirection != null and != incoming direction → throw
  Never silently allow conflicting reservation
```

### S-04 — Track Must Be Physically Verified Before GREEN
```
Four conditions must ALL be true before any signal turns GREEN:

  1. track.inUse == false
  2. track.usedBy.isEmpty() == true
  3. track.occupiedDirection == null OR matches incoming train
  4. No cycle exists in the reservation graph (deadlock check)

All four. Not three. Not "probably four."

Code rule:
  ConflictDetector.check() returns true ONLY when all four pass
  Any single failure → return false → signal stays RED
  This check runs before EVERY track entry, not just the first
```

### S-05 — Conservative Time Window
```
When computing track occupancy intervals:
  travelTime = Math.ceil(distance / minSpeedLimit)

Always overestimate how long a track is occupied.
Never underestimate.

Underestimate → two trains on same track in the model
             → ConflictDetector misses the conflict
             → real-world collision

Code rule:
  IntervalBuilder always uses Math.ceil()
  IntervalBuilder always uses minSpeedLimit, never maxSpeedLimit
  A train holds the interval until it physically exits — no early release
```

### S-06 — Speed Limits Are Hard Ceilings
```
maxSpeedLimit is a physical safety constraint set by infrastructure.
It is never changed at runtime.
A train exceeding maxSpeedLimit triggers emergency signal RED.

minSpeedLimit is an operational floor.
A train below minSpeedLimit blocks the line — also triggers warning.

Code rule:
  SignalController.isSpeedValid() enforces both limits
  violation of maxSpeedLimit → immediate RED, log emergency
  Track speed limits are set at construction, immutable after
```

### S-07 — Circular Deadlock Detection and Resolution
```
Circular dependency scenario:
  T1 blocked by T2
  T2 blocked by T3
  T3 blocked by T1

DFS cycle detection finds this.
Topological Sort cannot resolve it.

Resolution — BFS State Space Search (Railway Shunting):
  Model the problem as a graph of train positions
  Each "state" = positions of all trains + gap location
  BFS explores all valid single-train moves from current state
  First state where all trains reach their destinations = solution

The gap (empty track block) is the key:
  Only trains adjacent to the gap can move into it
  Wrong move = gap shifts, different set of trains can move
  BFS guarantees minimum moves to resolve
  No train moves until the full resolution sequence is computed

Code rule:
  DeadlockDetector detects cycle first (two HashSets)
  If cycle found → ShuntingResolver runs BFS
  ShuntingResolver returns ordered List<Move>
  Dispatcher executes moves in exact order
  Any deviation from sequence → HALT all trains, signal all RED
```

### S-08 — No Train Moves Without a Valid Path
```
PathFinder returning an empty list = no safe path exists.
This must never be silently ignored.

Code rule:
  IntervalBuilder throws IllegalStateException if path is empty
  Dispatcher never dispatches a train with no computed path
  A train with no path stays at origin with signal RED
  System logs the blockage clearly for operator intervention
```

### S-09 — State Changes Are Logged — Always
```
Every signal state change must be logged with:
  timestamp, signal ID, old state, new state, train ID, reason

Every track reservation must be logged with:
  timestamp, track ID, train ID, direction, enter time

Every conflict detected must be logged with:
  timestamp, conflict type, trains involved, resolution taken

Purpose:
  Post-accident analysis requires a complete immutable audit trail
  Indian Railways uses microprocessor data loggers for this reason
  This system must do the same

Code rule:
  No state change occurs without a log entry
  Logs are append-only — never overwritten or deleted
  Log format is machine-parseable (structured, not freeform)
```

### S-10 — System Restart Is Safe
```
On restart, every signal must default to RED.
No signal state is assumed from memory.
All track reservations are cleared.
All trains are treated as stopped at their last known position.

The system must re-verify every track before resuming.

Code rule:
  No signal state is persisted across restarts without re-verification
  On startup, GraphBuilder initializes all signals RED
  Dispatcher re-runs IntervalBuilder before dispatching any train
```

---

## Part 2 — What This System Resolves vs IR's Current Issues

| IR Issue | Root Cause | This System's Solution |
|---|---|---|
| Balasore 2023 (296 dead) | Human changed EI state without safety check | S-02: no signal changes without ConflictDetector |
| Kanchanjunga 2024 (9 dead) | Goods train passed red signal, no ATP | S-01: fail-safe RED, S-04: four-condition check |
| Signal failures 58% reduced but still exist | Manual intervention in signal changes | S-02: signal changes are software-only, no human override |
| Ambiguous protocol for signal failures | No clear rules when auto signal fails | S-08: no path = no movement, always RED |
| 20,000 loco pilot vacancies | Human dependency | Fully automated — no human dispatcher needed |
| Slow Kavach rollout | Hardware dependency, costs ₹70L per loco | Software-only solution, no per-locomotive hardware |
| Circular deadlocks (shunting yards) | No algorithm to resolve cyclic blocking | S-07: BFS state space search, guaranteed resolution |

---

## Part 3 — What This System Cannot Resolve (Honest Limits)

```
1. Physical track defects
   A broken rail, misaligned point, or damaged axle counter
   cannot be detected by this software alone.
   Requires: integration with physical sensor data (V3+)

2. Communication failure between system and train
   If the system sets GREEN but the message does not reach
   the locomotive, the train does not know.
   Requires: reliable communication layer (Kavach-style radio)

3. Sensor failures
   If axle counters or track circuits malfunction,
   this system believes incorrect occupancy data.
   Requires: dual/triple redundant sensor verification (V3+)

4. Software bugs in this codebase
   No software is bug-free. This system must have:
   - Unit tests for every ConflictDetector decision
   - Integration tests for every signal state transition
   - Regression tests after every code change
   - Independent safety audit before production deployment

5. Network partitions
   If the central system loses connectivity mid-operation,
   all signals must immediately revert to RED (S-10).
   Partial connectivity is more dangerous than no connectivity.

6. Trains already in motion when conflict is detected
   Predictive scheduling (IntervalBuilder) catches conflicts
   before trains depart. But if a train is already moving
   and a new conflict arises, braking distance matters.
   This system does not model braking physics in V1.
```

---

## Part 4 — The Circular Deadlock (Shunting Problem)

### What It Is
```
Three trains on a single-track line with one gap:

  [T1] -[gap]- [T2] -[T3]

  T1 needs to reach T3's position
  T2 needs to reach T1's position
  T3 needs to reach T2's position

  T1 blocked by T2
  T2 blocked by T3
  T3 blocked by T1 (wraps around via gap)

Topological Sort returns: "cycle detected, no solution."
DFS returns: "deadlock."
Neither gives a sequence of moves to resolve it.
```

### Why It Is Solvable
```
The gap (empty block) is the escape.
Any train adjacent to the gap can move into it.
This shifts which trains are adjacent to the gap.
By choosing moves carefully, the gap rotates around
until all trains reach their destinations.

This is identical to the 15-puzzle problem.
BFS on state space is guaranteed to find the
minimum move sequence.

State = tuple of all train positions + gap position
Move = one train slides into the adjacent gap
Goal = all trains at their destination positions
```

### Algorithm — BFS State Space Search
```java
// ShuntingResolver.java — concept

class State {
    Map<String, String> trainPositions;  // trainId → trackId
    String              gapTrackId;
    List<String>        moveSequence;    // how we got here
}

public List<String> resolve(List<Train> trains, GraphBuilder graph) {
    State initial = buildInitialState(trains);
    State goal    = buildGoalState(trains);

    Queue<State>     queue   = new LinkedList<>();
    Set<String>      visited = new HashSet<>();  // state hash

    queue.offer(initial);
    visited.add(hash(initial));

    while (!queue.isEmpty()) {
        State current = queue.poll();

        if (current.equals(goal)) {
            return current.moveSequence;  // optimal sequence found
        }

        // Generate all valid next states — trains adjacent to gap
        for (String move : validMoves(current, graph)) {
            State next = applyMove(current, move);
            String h   = hash(next);

            if (!visited.contains(h)) {
                visited.add(h);
                queue.offer(next);
            }
        }
    }

    return Collections.emptyList();  // no solution (impossible layout)
}
```

### Safety Rule for Shunting Resolver
```
The resolver must compute the FULL sequence before any train moves.
Not move-by-move. Not optimistically.

Full sequence computed → verified → Dispatcher executes in exact order.

If resolver returns empty list:
  All trains halt immediately
  All signals → RED
  System logs: UNRESOLVABLE DEADLOCK, human intervention required
  This should never happen in a correctly designed network
  but the system must handle it safely if it does
```

---

## Part 5 — Code Review Checklist

Before any code is merged, verify:

```
[ ] All signals initialized to RED in constructor
[ ] No signal.setState() called outside SignalController
[ ] ConflictDetector.check() called before every track entry
[ ] All four conditions checked — not short-circuited
[ ] IntervalBuilder uses Math.ceil and minSpeedLimit
[ ] track.reserve() validates occupiedDirection before setting
[ ] track.release() sets occupiedDirection = null when usedBy empty
[ ] DeadlockDetector runs before Dispatcher pops first train
[ ] ShuntingResolver returns full sequence before any move executes
[ ] Every state change has a log entry
[ ] PathFinder returning empty list throws exception — never ignored
[ ] System restart sets all signals RED before any operation
[ ] No hardcoded speed limits — all from track.minSpeedLimit
[ ] allowedTypes filter applied in PathFinder.getNeighbor()
[ ] No train dispatched without a valid pre-computed path
```

---

## Part 6 — Final Principle

> There can never be zero risk.
> This system's goal is to reduce risk to a level where
> no preventable accident can occur due to software logic.
>
> Hardware failures, sensor errors, and physical defects
> are outside this system's scope for V1.
> They must be addressed in V2 and V3 with sensor integration.
>
> Every line of code in this system should be written as if
> a life depends on it. Because eventually, it might.

---

## Author

[**Abishek Ganesh B S**](https://github.com/notAbishek)