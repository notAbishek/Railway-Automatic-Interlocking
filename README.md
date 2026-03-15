# Railway Automatic Interlocking
> An algorithm-driven railway traffic management system that automatically dispatches trains,
> detects conflicts, prevents deadlocks, and controls signals — without human intervention.

---

## Compiling and Executing

```bash
javac -d out -sourcepath src src\Main.java
java -cp out Main
```

---

## Architecture

![Architecture](assets/architecture.svg)

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
├── StationNode
│       stationCode    : String   (e.g. "MAS")
│       platformCount  : int
│
└── OriginNode
        virtual spawn node — gives train a valid position before
        it enters the first real signal node.
        connected to first signal via a spawn track (distance = 0)


Track
    id                  : String
    name                : String
    startNode           : Node
    endNode             : Node
    distance            : int    (metres)
    minSpeedLimit       : double (metres/second — slowest allowed, used for interval calculation)
    maxSpeedLimit       : double (metres/second — fastest allowed, safety ceiling)
    inUse               : boolean (default false)
    usedBy              : List<String>  (Train IDs on this track)
    occupiedDirection   : Direction     (set on reserve, null when free)

    reserve(trainId, direction) → sets inUse, adds to usedBy
    release(trainId)            → removes from usedBy, clears when empty


Train
    id                  : String
    name                : String
    type                : EMU | ENGINE | GOODS | PASSENGER
    priority            : EXPRESS | PASSENGER_EXP | GOODS | LOCAL
    trackOnUse          : Track ID  (null when not moving)
    direction           : RIGHT | LEFT
    startNode           : Node ID
    endNode             : Node ID
    speed               : double  (current speed, 0 at station)
    departureTime       : LocalDateTime  (format: dd-MM-yyyy HH:mm:ss)
    arrivalTime         : LocalDateTime  (planned — set by station planner)
    actualArrivalTime   : LocalDateTime  (calculated by IntervalBuilder)
    delayHours          : double  (0 if on time, positive if late)

    setTrackOnUse(trackId)     → called by ConflictDetector on entry
    clearTrackOnUse()          → called by ConflictDetector on exit
    setActualArrivalTime(time) → called by IntervalBuilder, computes delayHours


TrackInterval
    trainId     : String
    trackId     : String
    enterTime   : LocalDateTime
    exitTime    : LocalDateTime
    direction   : Direction

    conflictsWith(other)     → opposite direction + time overlap = true
    getConflictWindow(other) → [conflictStart, conflictEnd]
    delay(Duration)          → pushes enterTime and exitTime forward
    getOccupancyDuration()   → Duration between enter and exit
```

---

## Dispatch Ordering — Departure Time First

```
Rule:
  1. departureTime  → earliest departs first (always)
  2. priority       → tiebreaker when times are equal
  3. trainType      → tiebreaker when priority is also equal
  4. id             → final tiebreaker (alphabetical)

Why time is first, not priority:
  A LOCAL train at 08:00 must go before an EXPRESS at 20:00.
  Priority only matters when two trains compete for the same slot.

Example:
  Train A — LOCAL,   08:00 → dispatched 1st  (earliest time)
  Train B — EXPRESS, 08:00 → dispatched 2nd  (same time, priority wins)
  Train C — EXPRESS, 20:00 → dispatched 3rd  (later time, goes last)

Wrong (priority first):
  B → C → A   ← LOCAL at 08:00 stuck behind 20:00 EXPRESS

Correct (time first):
  A → B → C
```

---

## Signal States (Indian Railways Multi-Aspect)

```
RED           → Stop. Do not pass.
YELLOW        → Caution. Next signal is RED.
DOUBLE YELLOW → Attention. Next signal is YELLOW.
GREEN         → Proceed. Speed must stay within [minSpeedLimit, maxSpeedLimit].
```

Signal facing:
```
SignalNode.facing = RIGHT → controls trains moving RIGHT (→)
SignalNode.facing = LEFT  → controls trains moving LEFT  (←)
A signal only applies to a train whose direction matches its facing.
```

---

## Junction Explained

![Junction Diagram](assets/junction.svg)

```
state = false → routes via primaryNode
state = true  → routes via secondaryNode
No tertiary node needed — outgoing connection is a Track in the adjacency list.
```

---

## Origin Node and Spawn Track

```
Every train starts on a virtual spawn track before entering the graph.

  [ORIGIN] ──[T0, distance=0]── S1 ──[T1]── S2 ── ...

  OriginNode  → virtual, type ORIGIN, one per train
  Spawn Track → distance=0, speedLimit=any, gives valid trackOnUse
  PathFinder  → starts from train.startNode (S1), ignores ORIGIN
  T0 interval → enterTime = exitTime = departureTime (no travel time)
```

---

## Track Speed Limits vs Train Speed

```
Two fixed limits on every Track — set by infrastructure, never change:

  minSpeedLimit → slowest a train is allowed to move on this track
                  enforced by operations (don't block the line)
                  used by IntervalBuilder for worst-case occupancy:
                    travelSeconds = Math.ceil(distance / minSpeedLimit)

  maxSpeedLimit → fastest a train is allowed on this track
                  enforced by physics (curve radius, track grade, quality)
                  used by SignalController as a hard safety ceiling

One dynamic value on every Train:

  train.speed   → current speed, changes as train accelerates or brakes
                  0 at station (waiting for signal)
                  must reach minSpeedLimit after entering a block
                  must never exceed maxSpeedLimit

Valid operating range on any track = [minSpeedLimit, maxSpeedLimit]

SignalController validates:
  train.speed >= track.minSpeedLimit  → train is moving fast enough
  train.speed <= track.maxSpeedLimit  → train is within safe limit
  violation of maxSpeedLimit          → emergency, signal forced RED

Why minSpeedLimit for intervals (not maxSpeedLimit):
  Worst case occupancy = train moves at the slowest allowed speed.
  Conservative overestimate → track held slightly longer than reality.
  Safe: next train waits. Underestimate would risk collision model.

Why Math.ceil:
  distance / minSpeedLimit = 4.9 → ceil = 5, not 4.
  Never release a track 0.1 seconds early in the model.
```

---

## Arrival Time vs Actual Arrival

```
arrivalTime       → planned, set in constructor by station planner
                    the promise made to passengers

actualArrivalTime → calculated by IntervalBuilder from path + trackSpeedLimit
                    what the system predicts will actually happen

delayHours        → actualArrivalTime - arrivalTime
                    0 if on time, positive if late
                    logged automatically by IntervalBuilder
```

---

## Bidirectional Graph — Direction Filter

```
Every track stored under BOTH nodes:
  T1 (startNode=S1, endNode=S2):
    adjacencyList["S1"] → [T1]
    adjacencyList["S2"] → [T1]   ← same object, two references

PathFinder filters at query time:

  RIGHT train at S2:
    T1.startNode == S2? NO  → SKIP  (prevents going back to S1)
    T2.startNode == S2? YES → go to S3

  LEFT train at S2:
    T1.endNode == S2? YES → go to S1
    T2.endNode == S2? NO  → SKIP  (prevents going forward to S3)

Track has no direction. Direction belongs to the Train.
```

---

## Meet and Pass — Opposite Direction Conflict

```
Single track shared by both directions:
  Train A (RIGHT, EXPRESS): enters 08:10, exits 08:20
  Train B (LEFT,  GOODS):   enters 08:12, exits 08:22

Conflict window:
  start = max(08:10, 08:12) = 08:12
  end   = min(08:20, 08:22) = 08:20

Resolution (pre-dispatch, before either train moves):
  Train A = EXPRESS → no change
  Train B = GOODS   → delayed by 8 minutes at source
  SIG_B_OUT → RED until Train A clears the track
```

---

## Blocking Precedence — Same Direction Conflict

```
Single track, same direction:
  [T0: Train1 HIGH] -[T1]- [T2: Train2 LOW] -[T3]- ...

  Train1 wants T1 → T2 → T3
  Train2 physically sitting on T2 — in the way

PriorityQueue says dispatch Train1 first.
But Train2 must move before Train1 can enter T2.

DependencyResolver:
  Train1 depends on Train2

Topological Sort result:
  [Train2, Train1]  ← physical position overrides priority
```

---

## Algorithm Stack

| Problem | Algorithm | LC Reference |
|---|---|---|
| Train dispatch order | PriorityQueue (time → priority → type → id) | #253 Meeting Rooms II |
| Shortest path — EXPRESS / PASSENGER_EXP | Dijkstra (weight = distance / minSpeedLimit) | #743 Network Delay Time |
| Shortest path — GOODS / LOCAL | Bellman-Ford (K junction limit, temp snapshot, weight = distance / minSpeedLimit) | #787 Cheapest Flights |
| Pre-dispatch deadlock check | Cycle Detection (two HashSets) | #207 Course Schedule |
| Safe dispatch order with blocking | Topological Sort | #210 Course Schedule II |
| Track reservation conflicts | Interval Overlap | #56 Merge Intervals |
| Meet and Pass scheduling | DP + Intervals | #1235 Max Profit Job Scheduling |

---

## Track Layout — 3 Stations

![Track Layout](assets/track-layout.svg)

```
Nodes  : SignalNode x8, JunctionNode x2, StationNode x3
Tracks : T001, T002, T003, T004 (bidirectional — direction from Train)
```

---

## Project Structure

```
interlocking/
│
├── src/
│   │
│   ├── model/
│   │   ├── Node.java               ✓  abstract: id, name
│   │   ├── SignalNode.java         ✓  state, type, repeater link
│   │   ├── JunctionNode.java       ✓  state, direction, primaryNode, secondaryNode
│   │   ├── OriginNode.java         ✓  virtual spawn node
│   │   ├── Track.java              ✓  id, name, startNode, endNode, distance,
│   │   │                               minSpeedLimit, maxSpeedLimit,
│   │   │                               inUse, usedBy, occupiedDirection,
│   │   │                               reserve(), release(), TSR fields
│   │   │                               (TrackGeometry inner enum: STRAIGHT, CURVE)
│   │   ├── Train.java              ✓  id, name, type, priority, direction,
│   │   │                               startNode, endNode, speed, trackOnUse,
│   │   │                               departureTime, arrivalTime,
│   │   │                               actualArrivalTime, delayHours, LV fields
│   │   ├── TrackInterval.java      ✓  trainId, trackId, enterTime, exitTime,
│   │   │                               direction, conflictsWith(), delay(Duration),
│   │   │                               getConflictWindow(), getOccupancyDuration()
│   │   ├── TrackTraversal.java     ✓  track + direction wrapper
│   │   ├── SignalState.java        ✓  RED, YELLOW, DOUBLE_YELLOW, GREEN
│   │   └── TrainPriority.java      ✓  EXPRESS, PASSENGER_EXP, GOODS, LOCAL
│   │
│   ├── enums/
│   │   ├── Direction.java          ✓  RIGHT, LEFT
│   │   ├── JunctionDirection.java  ✓  RIGHT, LEFT
│   │   ├── JunctionState.java      ✓  PRIMARY, SECONDARY
│   │   ├── Priority.java           ✓  HIGH, MEDIUM, LOW
│   │   ├── TrainType.java          ✓  EMU, ENGINE, GOODS, PASSENGER
│   │   └── SignalType.java         ✓  GENERIC, HOME, STARTER, SHUNT, etc.
│   │
│   ├── core/
│   │   ├── GraphBuilder.java       ✓  HashMap<String, List<Track>>
│   │   │                               addTrack() — registers both nodes
│   │   │                               getTracksFrom(nodeId)
│   │   │                               getAdjacencyList()
│   │   ├── PathFinder.java         ✓  dijkstra() — EXPRESS / PASSENGER_EXP
│   │   │                               bellmanFord() — GOODS / LOCAL
│   │   │                               getNeighbor() — direction filter
│   │   │                               reconstructPath() — end to start, reversed
│   │   ├── Dispatcher.java         ✓  PriorityQueue<Train>
│   │   │                               sort: time → priority → type → id
│   │   │                               addTrain() — replaces past departure time
│   │   └── ConflictDetector.java   ✓  orchestrates all conflict checks
│   │
│   ├── conflict/
│   │   ├── TimeWindowConflict.java ✓  opposite direction time overlap
│   │   ├── HeadOnConflict.java     ✓  opposite direction runtime check
│   │   ├── FollowingConflict.java  ✓  same direction runtime check
│   │   ├── DeadlockDetector.java   ✓  cycle detection, two HashSets
│   │   └── ShuntingResolver.java   ✓  BFS state space, circular deadlock
│   │
│   ├── signal/
│   │   ├── SignalController.java   ✓  only class that calls setState()
│   │   └── SignalRule.java         ✓  5-condition GREEN check
│   │
│   ├── scheduler/
│   │   ├── TrainScheduler.java     ✓  orchestrates all pre-dispatch steps
│   │   ├── IntervalBuilder.java    ✓  buildPaths() — PathFinder for all trains
│   │   │                               buildIntervals() — TrackInterval per track
│   │   │                               computeIntervals() — LocalDateTime based
│   │   │                               sets actualArrivalTime + delayHours
│   │   ├── DependencyResolver.java ✓  blocking detection, topo sort
│   │   └── MeetAndPassResolver.java ✓  opposite direction delay resolution
│   │
│   ├── db/
│   │   ├── DatabaseLayer.java      ✓  PostgreSQL layer scaffold (V2)
│   │   └── schema.sql              ✓  3NF schema
│   │
│   └── Main.java                   ✓  scheduler + simulation loop
│
├── README.md
└── LICENSE
```

---

## Execution Order

```
1. GraphBuilder         nodes and tracks registered

2. IntervalBuilder      PathFinder runs for every train upfront
                        TrackIntervals built using LocalDateTime
                        actualArrivalTime and delayHours set per Train

3. DependencyResolver   same direction blocking detected
                        Topological Sort rewrites dispatch order

4. MeetAndPassResolver  opposite direction time window conflicts
                        lower priority train departure delayed at source

5. Dispatcher           trains popped — time first, priority tiebreak

6. ConflictDetector     real-time check before each track entry
                        last line of defence

7. SignalController     RED/GREEN set per ConflictDetector result

8. Train moves          block by block
                        track.reserve() on entry
                        track.release() on exit

Repeat 6,7,8 per track.
Repeat 5,6,7,8 per train.
```

---

## How It Works

```
1. Trains added with departureTime and planned arrivalTime

2. Scheduler runs pre-dispatch (steps 2-4)
     all paths computed upfront — no surprises mid-dispatch

3. Dispatcher pops next train

4. ConflictDetector checks each track in path:
     a. track.inUse == false?
     b. track.usedBy is empty?
     c. Opposite direction train on this track?
     d. Adding this train creates a cycle?

5. If clear:
     track.reserve(trainId, direction)
     train.setTrackOnUse(trackId)
     signalController → GREEN

6. If conflict (edge case scheduler missed):
     signalController → RED
     dispatcher.requeue(train)

7. Train moves block by block:
     track.release() on previous block
     signal behind → RED, signal ahead → checked fresh

8. Database updated at every state change (V2)
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
    node_id    VARCHAR(20) PRIMARY KEY,
    node_name  VARCHAR(100),
    node_type  VARCHAR(10),
    station_id VARCHAR(10),
    FOREIGN KEY (station_id) REFERENCES stations(station_id)
);

CREATE TABLE signals (
    node_id      VARCHAR(20) PRIMARY KEY,
    signal_state VARCHAR(20) DEFAULT 'RED',
    facing       VARCHAR(10),
    FOREIGN KEY (node_id) REFERENCES nodes(node_id)
);

CREATE TABLE junctions (
    node_id        VARCHAR(20) PRIMARY KEY,
    primary_node   VARCHAR(20),
    secondary_node VARCHAR(20),
    state          BOOLEAN DEFAULT FALSE,
    direction      VARCHAR(10),
    FOREIGN KEY (node_id) REFERENCES nodes(node_id)
);

CREATE TABLE tracks (
    track_id          VARCHAR(20) PRIMARY KEY,
    track_name        VARCHAR(100),
    start_node        VARCHAR(20),
    end_node          VARCHAR(20),
    distance          INT,
    min_speed_limit   DECIMAL(6,2),
    max_speed_limit   DECIMAL(6,2),
    in_use            BOOLEAN DEFAULT FALSE,
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
    train_id          VARCHAR(20) PRIMARY KEY,
    train_name        VARCHAR(100),
    train_type        VARCHAR(20),
    priority          VARCHAR(20),
    direction         VARCHAR(10),
    start_node        VARCHAR(20),
    end_node          VARCHAR(20),
    speed             DECIMAL(6,2),
    departure_time    TIMESTAMP,
    arrival_time      TIMESTAMP,
    track_on_use      VARCHAR(20),
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

> Fail-safe: All signals default RED. GREEN set only when all four
> conditions verified safe by ConflictDetector.

---

## Build Order

```
Done  ✓   All model files
Done  ✓   All enums
Done  ✓   GraphBuilder, PathFinder, Dispatcher
Done  ✓   IntervalBuilder
Done  ✓   All conflict/ files
Done  ✓   DependencyResolver, MeetAndPassResolver, TrainScheduler
Done  ✓   SignalController, SignalRule
Done  ✓   ConflictDetector
Done  ✓   Main.java simulation loop

V2    →   DatabaseLayer (PostgreSQL)
V2    →   Automatic block pass protocol (1-2 min wait timer)
V2    →   Gradient safety for shunting
V2    →   Spring Boot REST API
V3    →   GraphStream visualization
V3    →   React + WebSocket live map
```

---

## Tech Stack

- **Language:** Java
- **Algorithms:** Dijkstra, Bellman-Ford, Topological Sort, Cycle Detection, Priority Queue
- **Database:** PostgreSQL (V2)
- **Visualization:** GraphStream (core), React + WebSocket (V3)
- **API:** Spring Boot (V2)

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

## Author

[**Abishek Ganesh B S**](https://github.com/notAbishek)