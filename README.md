# Railway Automatic Interlocking (Under Devlopment)
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
  │         direction, startNode, endNode)                        │
  │   Track(id, name, startNode, endNode, distance)               │
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
  │   │ (by priority │      │  on Graph     │                    │
  │   │  + dep time) │      └──────┬────────┘                    │
  │   └──────────────┘             │                             │
  │                                ▼                             │
  │                   ┌─────────────────────┐                    │
  │                   │  ConflictDetector   │                    │
  │                   │                     │                    │
  │                   │  Track.usedBy list  │                    │
  │                   │  + Cycle Detection  │                    │
  │                   │  + Direction Check  │                    │
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
│       state : RED | YELLOW | DOUBLE_YELLOW | GREEN
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
│   id          : String
│   name        : String
│   startNode   : Node ID
│   endNode     : Node ID
│   distance    : int (metres)
│   inUse       : boolean (default false)
│   usedBy      : List<String>  (Train IDs currently on this track)


Train
    id          : String
    name        : String
    type        : EMU | ENGINE | GOODS | PASSENGER
    priority    : EXPRESS | PASSENGER_EXP | GOODS | LOCAL
    trackOnUse  : Track ID
    direction   : RIGHT | LEFT
    startNode   : Node ID
    endNode     : Node ID
    speed       : double (metres/second)
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

## Train Priority (Dispatch Order)

```
EXPRESS        → Highest priority (Rajdhani, Shatabdi)
PASSENGER_EXP  → Medium-high (Mail, Express)
GOODS          → Medium-low
LOCAL          → Lowest priority (suburban, EMU)
```

---

## Track Model

```java
Track {
    inUse  : false    // no train on this track
    usedBy : []       // empty list — no trains

    // When Train A enters:
    inUse  : true
    usedBy : ["TRAIN_ID"]

    // When Train B also enters same track (conflict zone):
    inUse  : true
    usedBy : ["TRAIN_ID", "TRAIN_ID"]  ← triggers ConflictDetector

    // When Train A exits:
    usedBy : ["TRAIN_ID"]
    inUse  : true  (still occupied)

    // When Train B exits:
    inUse  : false
    usedBy : []    ← track fully free
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
  Tracks : T001, T002, T003, T004 (each has FWD + BWD)
```

---

## Project Structure

```
interlocking/
├── src/
│   ├── model/
│   │   ├── Node.java                 # abstract base
│   │   ├── SignalNode.java            # RED|YELLOW|DOUBLE_YELLOW|GREEN
│   │   ├── JunctionNode.java          # state + direction + primary/secondary
│   │   ├── StationNode.java           # stationCode + platformCount
│   │   ├── Track.java                 # startNode, endNode, usedBy List
│   │   ├── Train.java                 # type, priority, direction, speed
│   │   ├── SignalState.java           # enum
│   │   ├── TrainType.java             # enum
│   │   ├── TrainPriority.java         # enum
│   │   ├── JunctionDirection.java     # enum
│   │   └── Direction.java             # RIGHT | LEFT
│   ├── core/
│   │   ├── GraphBuilder.java          # adjacency list
│   │   ├── PathFinder.java            # Dijkstra
│   │   ├── Dispatcher.java            # PriorityQueue
│   │   └── ConflictDetector.java      # usedBy + cycle detection
│   ├── signal/
│   │   └── SignalController.java      # manages signal state transitions
│   ├── db/
│   │   └── DatabaseLayer.java         # PostgreSQL CRUD
│   └── Main.java
├── schema.sql
├── LICENSE
└── README.md
```

---

## Algorithm Stack

| Problem | Algorithm | Reference |
|---|---|---|
| Train dispatch order | Priority Queue | LC #253 Meeting Rooms II |
| Shortest path on tracks | Dijkstra | LC #743 Network Delay Time |
| Pre-dispatch deadlock check | Cycle Detection | LC #207 Course Schedule |
| Safe train dispatch order | Topological Sort | LC #210 Course Schedule II |
| Track reservation conflicts | Interval Overlap | LC #56 Merge Intervals |
| Limited track switching | Bellman-Ford | LC #787 Cheapest Flights |
| Priority scheduling | DP + Intervals | LC #1235 Max Profit Job Scheduling |

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
    node_type  VARCHAR(10),           -- SIGNAL | JUNCTION | STATION
    station_id VARCHAR(10),
    FOREIGN KEY (station_id) REFERENCES stations(station_id)
);

CREATE TABLE signals (
    node_id      VARCHAR(20) PRIMARY KEY,
    signal_state VARCHAR(20) DEFAULT 'RED',  -- RED|YELLOW|DOUBLE_YELLOW|GREEN
    FOREIGN KEY  (node_id) REFERENCES nodes(node_id)
);

CREATE TABLE junctions (
    node_id        VARCHAR(20) PRIMARY KEY,
    primary_node   VARCHAR(20),
    secondary_node VARCHAR(20),
    state          BOOLEAN     DEFAULT FALSE,
    direction      VARCHAR(10),              -- RIGHT | LEFT
    FOREIGN KEY (node_id) REFERENCES nodes(node_id)
);

CREATE TABLE tracks (
    track_id    VARCHAR(20)  PRIMARY KEY,
    track_name  VARCHAR(100),
    start_node  VARCHAR(20),
    end_node    VARCHAR(20),
    distance    INT,                         -- metres
    in_use      BOOLEAN      DEFAULT FALSE,
    FOREIGN KEY (start_node) REFERENCES nodes(node_id),
    FOREIGN KEY (end_node)   REFERENCES nodes(node_id)
);

CREATE TABLE track_usage (
    track_id    VARCHAR(20),
    train_id    VARCHAR(20),
    entered_at  TIMESTAMP,
    PRIMARY KEY (track_id, train_id),
    FOREIGN KEY (track_id) REFERENCES tracks(track_id)
);

CREATE TABLE trains (
    train_id      VARCHAR(20)  PRIMARY KEY,
    train_name    VARCHAR(100),
    train_type    VARCHAR(20),    -- EMU|ENGINE|GOODS|PASSENGER
    priority      VARCHAR(20),    -- EXPRESS|PASSENGER_EXP|GOODS|LOCAL
    direction     VARCHAR(10),    -- RIGHT | LEFT
    start_node    VARCHAR(20),
    end_node      VARCHAR(20),
    speed         DECIMAL(6,2),   -- metres/second
    track_on_use  VARCHAR(20),
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

## How It Works

```
1. Trains loaded into PriorityQueue
      sorted by: Departure time first, TrainPriority second

2. Dispatcher pops highest priority train

3. PathFinder runs Dijkstra
      weight = track.distance / train.speed
      respects JunctionNode.state and direction

4. ConflictDetector checks each track in path:
      a. track.inUse == false?
      b. track.usedBy list is empty?
      c. Opposite direction track usedBy is empty?
      d. Does adding this train create a cycle? (DFS)

5. If clear:
      track.inUse = true
      track.usedBy.add(train.id)
      signalController.advance(signal)  → GREEN

6. If conflict:
      signalController.setRed(signal)
      dispatcher.requeue(train)

7. As train moves block by block:
      previous track: usedBy.remove(train.id)
      if usedBy.isEmpty() → inUse = false

8. Database updated at every state change
```

---

## Tech Stack

- **Language:** Java
- **Algorithms:** Dijkstra, Topological Sort, Cycle Detection, Priority Queue
- **Database:** PostgreSQL
- **Visualization:** GraphStream, React + WebSocket
- **API:** Spring Boot

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

## Author

**Abishek Ganesh B S**
[GitHub](https://github.com/notAbishek)