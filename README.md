# Railway Automatic Interlocking
> An algorithm-driven railway traffic management system that automatically dispatches trains,
> detects conflicts, prevents deadlocks, and controls signals — without human intervention.

---

## Architecture

```
                        ARCHITECTURE
                        ===============

  ┌──────────────────────────────────────────────────────────────┐
  │                        INPUT LAYER                            │
  │                                                               │
  │   Train(id, name, type, priority, speed,                      │
  │         direction, startNode, endNode, departureTime)         │
  │   Track(id, name, startNode, endNode, distance)               │
  └──────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────────────────────────────┐
  │                     SCHEDULER (Pre-Dispatch)                  │
  │                                                               │
  │   Runs BEFORE any train departs.                              │
  │   Computes all TrackIntervals using PathFinder.               │
  │   Detects time-window conflicts (Meet and Pass).              │
  │   Delays lower priority train at source — not mid-route.      │
  └──────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────────────────────────────┐
  │                      CORE ENGINE                              │
  │                                                               │
  │   ┌──────────────┐      ┌───────────────┐                    │
  │   │  Dispatcher  │      │  PathFinder   │                    │
  │   │              │─────▶│               │                    │
  │   │ PriorityQueue│      │  Dijkstra     │                    │
  │   │              │      │  (EXPRESS /   │                    │
  │   │ Sort rule:   │      │   PASSENGER)  │                    │
  │   │ 1. dep time  │      │               │                    │
  │   │ 2. priority  │      │  Bellman-Ford │                    │
  │   │    (tiebreak │      │  (GOODS /     │                    │
  │   │    only)     │      │   LOCAL)      │                    │
  │   └──────────────┘      └──────┬────────┘                    │
  │                                ▼                             │
  │                   ┌─────────────────────┐                    │
  │                   │  ConflictDetector   │                    │
  │                   │                     │                    │
  │                   │  Track.usedBy list  │                    │
  │                   │  + Cycle Detection  │                    │
  │                   │  + Direction Check  │                    │
  │                   │  + Time Window      │                    │
  │                   └──────────┬──────────┘                    │
  └──────────────────────────────┼───────────────────────────────┘
                                 │
                                 ▼
  ┌──────────────────────────────────────────────────────────────┐
  │                    SIGNAL CONTROLLER                          │
  │                                                               │
  │   Default state = RED (fail-safe)                             │
  │   States: RED → YELLOW → DOUBLE_YELLOW → GREEN               │
  │                                                               │
  │   Set GREEN only when:                                        │
  │     1. Track.inUse == false                                   │
  │     2. Track.usedBy list is empty                             │
  │     3. No cycle in reservation graph                          │
  │     4. Opposite direction track is clear                      │
  └──────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────────────────────────────┐
  │                    DATABASE LAYER                             │
  │                    (PostgreSQL — 3NF)                         │
  │                                                               │
  │   trains | tracks | nodes | reservations | signals            │
  └──────────────────────────────────────────────────────────────┘
```

---

## Class Hierarchy

```
Node (abstract)
│   id    : String
│   name  : String
│
├── SignalNode
│       state  : RED | YELLOW | DOUBLE_YELLOW | GREEN
│       facing : RIGHT | LEFT
│
├── JunctionNode
│       primaryNode   : Node ID   (connected when state = false)
│       secondaryNode : Node ID   (connected when state = true)
│       state         : false (default) | true
│       direction     : RIGHT | LEFT
│          RIGHT = two tracks merge into one  →
│          LEFT  = one track splits into two  ←
│
└── StationNode
        stationCode    : String   (e.g. "MAS")
        platformCount  : int


Track
│   id                  : String
│   name                : String
│   startNode           : Node ID
│   endNode             : Node ID
│   distance            : int (metres)
│   inUse               : boolean (default false)
│   usedBy              : List<String>  (Train IDs currently on this track)
│   occupiedDirection   : Direction     (which way current train is moving)


Train
    id              : String
    name            : String
    type            : EMU | ENGINE | GOODS | PASSENGER
    priority        : EXPRESS | PASSENGER_EXP | GOODS | LOCAL
    trackOnUse      : Track ID
    direction       : RIGHT | LEFT
    startNode       : Node ID
    endNode         : Node ID
    speed           : double (metres/second)
    departureTime   : int    (seconds from simulation start)


TrackInterval
    trainId     : String
    trackId     : String
    enterTime   : int     (seconds)
    exitTime    : int     (enterTime + distance / speed)
    direction   : Direction
```

---

## Dispatch Ordering — Departure Time First

```
Rule:
  Primary sort   → departureTime   (earliest departs first, always)
  Secondary sort → priority        (tiebreaker ONLY when times are equal)

Why:
  A LOCAL train departing at 08:00 must go before an EXPRESS
  departing at 20:00. Time governs, priority only breaks ties.

Example:
  Train A — LOCAL,   departs 08:00  → dispatched 1st
  Train B — EXPRESS, departs 08:00  → dispatched 2nd  (same time, priority wins)
  Train C — EXPRESS, departs 20:00  → dispatched 3rd

Wrong approach (priority first):
  Train B → Train C → Train A       ← LOCAL at 08:00 stuck behind 20:00 EXPRESS

Correct approach (time first):
  Train A → Train B → Train C       ← always correct

In code:
  PriorityQueue sorted by:
    1. train.departureTime  (ascending)
    2. train.priority.ordinal() (ascending, lower ordinal = higher priority)
```

---

## Junction Explained

```
Direction: RIGHT — two tracks merge into one

    Track A ──→ ╮
                 JCT ──→ continues
    Track B ──→ ╯

    state = false → train routes via primaryNode (Track A)
    state = true  → train routes via secondaryNode (Track B)


Direction: LEFT — one track splits into two

                 ╭──→ Track A
    continues ──→ JCT
                 ╰──→ Track B

    state = false → train exits via primaryNode (Track A)
    state = true  → train exits via secondaryNode (Track B)
```

---

## Signal States (Indian Railways Multi-Aspect)

```
RED           → Stop. Do not pass.
YELLOW        → Caution. Next signal is RED.
DOUBLE YELLOW → Attention. Next signal is YELLOW.
GREEN         → Proceed at full speed.
```

---

## Algorithm Stack

| Problem | Algorithm | Reference |
|---|---|---|
| Train dispatch order | Priority Queue (time first, priority tiebreak) | LC #253 Meeting Rooms II |
| Shortest path — EXPRESS / PASSENGER | Dijkstra (weight = distance / speed) | LC #743 Network Delay Time |
| Shortest path — GOODS / LOCAL | Bellman-Ford (junction limit K) | LC #787 Cheapest Flights |
| Pre-dispatch deadlock check | Cycle Detection (two HashSets) | LC #207 Course Schedule |
| Safe train dispatch order | Topological Sort | LC #210 Course Schedule II |
| Track reservation conflicts | Interval Overlap | LC #56 Merge Intervals |
| Meet and Pass scheduling | DP + Intervals | LC #1235 Max Profit Job Scheduling |

---

## Meet and Pass — Time Window Conflict

```
Scenario:
  Single track T002 shared by both directions.
  Train A (RIGHT): enters 08:10, exits 08:20
  Train B (LEFT):  enters 08:12, exits 08:22

Conflict window:
  start = max(08:10, 08:12) = 08:12
  end   = min(08:20, 08:22) = 08:20
  Train B must be delayed until 08:20.

Resolution:
  Train A = EXPRESS  → proceeds
  Train B = GOODS    → delayed 8 minutes at source
  SIG_B_OUT          → RED until 08:20
  After 08:20        → SIG_B_OUT → GREEN, Train B departs

This runs at scheduling time — before either train departs.
Not reactive. Predictive.
```

---

## Track Model

```java
Track {
    inUse  : false    // no train on this track
    usedBy : []       // empty list

    // When Train A enters:
    inUse  : true
    usedBy : ["TRAIN_A"]

    // When Train B also enters (conflict zone):
    usedBy : ["TRAIN_A", "TRAIN_B"]  ← triggers ConflictDetector

    // When Train A exits:
    usedBy : ["TRAIN_B"]
    inUse  : true

    // When Train B exits:
    inUse  : false
    usedBy : []
}
```

---

## Track Layout — 3 Stations

```
  STATION A             JCT W        JCT E          STATION B
     │                   │              │                │
  [SIG_A1]──[T001]──[SIG_JL]──[T003]──[SIG_JR]──[T004]──[SIG_B1]   → RIGHT
  [SIG_A2]──────────[SIG_JL2]─[T003]──[SIG_JR2]─[T004]──[SIG_B2]   ← LEFT
                         │
                      [T002]
                         │
                      [SIG_C1]
                         │
                    STATION C

  Nodes  : SignalNode x8, JunctionNode x2, StationNode x3
  Tracks : T001, T002, T003, T004 (bidirectional — direction from Train)
```

---

## Project Structure

```
interlocking/
├── src/
│   ├── model/
│   │   ├── Node.java                   ← abstract base: id, name
│   │   ├── SignalNode.java             ← extends Node: state, facing
│   │   ├── JunctionNode.java           ← extends Node: state, direction, primaryNode, secondaryNode
│   │   ├── StationNode.java            ← extends Node: stationCode, platformCount
│   │   ├── Track.java                  ← id, name, startNode, endNode, distance, inUse, usedBy, occupiedDirection
│   │   ├── Train.java                  ← id, name, type, direction, startNode, endNode, speed, priority, departureTime
│   │   └── TrackInterval.java          ← trainId, trackId, enterTime, exitTime, direction
│   │
│   ├── enums/
│   │   ├── SignalState.java            ← RED, YELLOW, DOUBLE_YELLOW, GREEN
│   │   ├── SignalFacing.java           ← RIGHT, LEFT
│   │   ├── Direction.java              ← RIGHT, LEFT
│   │   ├── JunctionState.java          ← PRIMARY (false), SECONDARY (true)
│   │   ├── TrainType.java              ← EMU, ENGINE, GOODS, PASSENGER
│   │   └── Priority.java              ← EXPRESS, PASSENGER_EXP, GOODS, LOCAL
│   │
│   ├── core/
│   │   ├── GraphBuilder.java           ← HashMap<String, List<Track>> adjacency list
│   │   ├── PathFinder.java             ← Dijkstra (EXPRESS/PASSENGER_EXP) + Bellman-Ford (GOODS/LOCAL)
│   │   ├── Dispatcher.java             ← PriorityQueue: sort by departureTime, priority as tiebreaker
│   │   ├── ConflictDetector.java       ← orchestrates all conflict detection
│   │   │
│   │   └── conflict/
│   │       ├── TimeWindowConflict.java ← hasTimeConflict(), getConflictWindow()
│   │       ├── HeadOnConflict.java     ← opposite direction on same track
│   │       ├── FollowingConflict.java  ← same direction, one train behind another
│   │       └── DeadlockDetector.java   ← cycle detection (currentPath + visited HashSets)
│   │
│   ├── signal/
│   │   ├── SignalController.java       ← sets RED/YELLOW/DOUBLE_YELLOW/GREEN
│   │   └── SignalRule.java             ← fail-safe rules: default RED, GREEN conditions
│   │
│   ├── scheduler/
│   │   ├── TrainScheduler.java         ← scheduleAndResolve(): runs before any train departs
│   │   ├── IntervalBuilder.java        ← computeIntervals(): builds TrackInterval list per train
│   │   └── MeetAndPassResolver.java    ← delays lower priority train, resolves time window conflicts
│   │
│   ├── db/
│   │   ├── DatabaseLayer.java          ← PostgreSQL CRUD
│   │   └── schema.sql                  ← 3NF schema
│   │
│   └── Main.java
│
├── test/
│   ├── GraphBuilderTest.java
│   ├── PathFinderTest.java
│   ├── ConflictDetectorTest.java
│   └── SchedulerTest.java
│
├── README.md
└── LICENSE
```

---

## How It Works

```
1. Trains loaded into PriorityQueue
      sorted by: departureTime first, priority second (tiebreaker only)

2. Scheduler runs BEFORE dispatch
      builds TrackIntervals for every train using PathFinder
      detects Meet-and-Pass conflicts using time window overlap
      delays lower priority train at source until conflict clears

3. Dispatcher pops next train (earliest departure time)

4. PathFinder finds route
      EXPRESS / PASSENGER_EXP → Dijkstra (weight = distance / speed)
      GOODS / LOCAL           → Bellman-Ford (junction limit applied)

5. ConflictDetector checks each track in path:
      a. track.inUse == false?
      b. track.usedBy list is empty?
      c. Opposite direction train on this track?
      d. Does adding this train create a cycle? (DFS)

6. If clear:
      track.inUse = true
      track.usedBy.add(train.id)
      signalController.advance(signal) → GREEN

7. If conflict:
      signalController.setRed(signal)
      dispatcher.requeue(train)

8. As train moves block by block:
      previous track: usedBy.remove(train.id)
      if usedBy.isEmpty() → inUse = false
      signal behind train → RED

9. Database updated at every state change
```

---

## Database Schema (3NF)

```sql
CREATE TABLE stations (
    station_id     VARCHAR(10) PRIMARY KEY,
    station_name   VARCHAR(100),
    station_code   VARCHAR(10),
    platform_count INT
);

CREATE TABLE nodes (
    node_id    VARCHAR(20)  PRIMARY KEY,
    node_name  VARCHAR(100),
    node_type  VARCHAR(10),
    station_id VARCHAR(10),
    FOREIGN KEY (station_id) REFERENCES stations(station_id)
);

CREATE TABLE signals (
    node_id      VARCHAR(20) PRIMARY KEY,
    signal_state VARCHAR(20) DEFAULT 'RED',
    facing       VARCHAR(10),
    FOREIGN KEY  (node_id) REFERENCES nodes(node_id)
);

CREATE TABLE junctions (
    node_id        VARCHAR(20) PRIMARY KEY,
    primary_node   VARCHAR(20),
    secondary_node VARCHAR(20),
    state          BOOLEAN     DEFAULT FALSE,
    direction      VARCHAR(10),
    FOREIGN KEY (node_id) REFERENCES nodes(node_id)
);

CREATE TABLE tracks (
    track_id    VARCHAR(20)  PRIMARY KEY,
    track_name  VARCHAR(100),
    start_node  VARCHAR(20),
    end_node    VARCHAR(20),
    distance    INT,
    in_use      BOOLEAN      DEFAULT FALSE,
    FOREIGN KEY (start_node) REFERENCES nodes(node_id),
    FOREIGN KEY (end_node)   REFERENCES nodes(node_id)
);

CREATE TABLE track_usage (
    track_id    VARCHAR(20),
    train_id    VARCHAR(20),
    entered_at  TIMESTAMP,
    direction   VARCHAR(10),
    PRIMARY KEY (track_id, train_id),
    FOREIGN KEY (track_id) REFERENCES tracks(track_id)
);

CREATE TABLE trains (
    train_id        VARCHAR(20)  PRIMARY KEY,
    train_name      VARCHAR(100),
    train_type      VARCHAR(20),
    priority        VARCHAR(20),
    direction       VARCHAR(10),
    start_node      VARCHAR(20),
    end_node        VARCHAR(20),
    speed           DECIMAL(6,2),
    departure_time  INT,
    track_on_use    VARCHAR(20),
    FOREIGN KEY (start_node)   REFERENCES nodes(node_id),
    FOREIGN KEY (end_node)     REFERENCES nodes(node_id),
    FOREIGN KEY (track_on_use) REFERENCES tracks(track_id)
);
```

---

## Signal Rules (Indian Railways — G&SR)

| Signal | Placement | Default State |
|---|---|---|
| Home Signal | At station entry | RED |
| Starter Signal | At station exit | RED |
| Junction Signal | Before every track switch | RED |
| Block Signal | Between stations on single track | RED |

> Fail-safe rule: All signals default to RED. GREEN is only set when
> all conditions are verified safe by ConflictDetector.

---

## Tech Stack

- **Language:** Java
- **Algorithms:** Dijkstra, Bellman-Ford, Topological Sort, Cycle Detection, Priority Queue
- **Database:** PostgreSQL
- **Visualization:** GraphStream (core), React + WebSocket (V3)
- **API:** Spring Boot (V2)

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

## Author

**Abishek Ganesh B S**
[GitHub](https://github.com/notAbishek) · Chennai, Tamil Nadu