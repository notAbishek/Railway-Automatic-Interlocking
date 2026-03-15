# SAFETY.md — Interlocking System Safety Principles

This document defines non-negotiable safety behavior for the interlocking simulation. The system is intentionally fail-safe first, throughput second, and every movement authorization must be explainable from code-level checks.

---

## Why This Document Exists

On June 2, 2023, the Coromandel Express disaster demonstrated how an interlocking state mismatch can convert a valid signal into a fatal route condition. The core lesson is that route state, signal state, and movement authorization must remain bound to the same safety chain. This project codifies that chain in software so no movement is authorized without explicit verification.

---

## Part 1 — Fundamental Safety Principles

### S-01 — Fail-Safe Default
Every signal starts in RED and should return to RED whenever the system cannot prove a safe proceed condition. This mirrors railway fail-safe philosophy where danger is the resting state and proceed is temporary authority. A restart or control uncertainty must therefore bias to stop.
Violation permits movement under unknown conditions.
Enforced by: SignalNode constructor, SignalController.setRed().

### S-02 — No Signal Changes Without Verification
Signal clearing must only happen after conflict and route checks have passed for the exact movement under consideration. The code path that requests GREEN must be the same path that performed safety validation, so authorization cannot be detached from verification context. Direct signal manipulation outside that chain is unsafe by design.
Violation enables bypass of interlocking protections.
Enforced by: ConflictDetector.check(), SignalController.requestGreen(), Main dispatch loop sequence.

### S-03 — Exclusive Track Occupancy Semantics
A track section under active use cannot be simultaneously granted for a conflicting movement direction. Occupancy state and direction state must be consistent across entry and exit events so collision checks are deterministic. Reservation and release operations therefore form the runtime lock model for track usage.
Violation allows head-on or same-block movement overlap.
Enforced by: Track.reserve(), Track.release(), HeadOnConflict, FollowingConflict.

### S-04 — Multi-Condition Gate Before Proceed
Signal clearance is a conjunction of safety predicates, not a single flag, and includes occupancy, direction conflict, overlap, junction availability, and deadlock checks. If any predicate fails, movement authority must be denied and the signal must remain or return to RED. This ensures no partial-check state can accidentally authorize a train.
Violation clears signals on incomplete route safety evidence.
Enforced by: SignalRule.canSetGreen(), ConflictDetector.check().

### S-05 — Conservative Interval Modeling
Occupancy duration should be pessimistic rather than optimistic so route release does not happen before a train can reasonably clear a section. Conservative interval arithmetic reduces unsafe headway assumptions in pre-dispatch planning. This is essential when scheduling and runtime checks are layered together.
Violation underestimates occupancy and may schedule conflicting use.
Enforced by: IntervalBuilder.computeIntervals().

### S-06 — Speed Limit Safety Envelope
Infrastructure speed limits are safety constraints and must be treated as hard boundaries in runtime validation. Effective speed must account for temporary restrictions and signal logic must be able to force or keep restrictive aspects when speed safety is violated. Operational scheduling values must never weaken these runtime limits.
Violation can permit derailment-risk or braking-risk movement.
Enforced by: Track.getEffectiveMaxSpeed(), SignalController.validateSpeed().

### S-07 — Deadlock Detection Before Dispatch Commitment
Dependency cycles between trains must be detected before movement commits to avoid locked network states during operation. If a cycle is found, the resolver must produce a deterministic conflict-resolution sequence or hold traffic safely. Deadlock handling is therefore part of safety, not merely performance.
Violation can produce irreversible blocking with unsafe manual overrides.
Enforced by: DeadlockDetector, DependencyResolver, ShuntingResolver.

### S-08 — No Movement Without a Valid Path
A train without a valid computed path has no movement authority and must not enter dispatch execution. Empty or broken path results must be treated as safety failures and surfaced immediately. Dispatch should continue only for trains whose route plan is fully available.
Violation allows undefined routing behavior at runtime.
Enforced by: IntervalBuilder.buildPaths(), Dispatcher usage in Main.

### S-09 — Safety-Critical State Logging
Signal transitions, conflict detections, and reservation events must be logged so post-incident analysis can reconstruct the exact decision path. Logging is not only observability; it is part of safety assurance and verification evidence. Missing logs degrade trust in both automated and manual review.
Violation prevents accountability and root-cause reconstruction.
Enforced by: ConflictDetector.log(), SignalController.log(), runtime console traces.

### S-10 — Safe Recovery on Restart
After restart, the system must not assume prior proceed authority remains valid without re-evaluation. Runtime state should be reconstructed from safe defaults and fresh checks before train movement resumes. This prevents stale-memory or partial-state hazards during recovery.
Violation reintroduces obsolete authority into active operations.
Enforced by: signal default RED construction plus pre-dispatch recomputation flow in Main and scheduler pipeline.

---

## Part 2 — What This System Resolves

The architecture addresses major classes of preventable signaling failures by forcing all movement through a common verification chain. `ConflictDetector` evaluates occupancy, direction conflicts, overlap, junction availability, and deadlock before a route segment can proceed, while `SignalController` centralizes aspect changes so RED remains the fallback state when checks fail. `DependencyResolver` and `MeetAndPassResolver` reduce unsafe dispatch ordering before runtime, and `ShuntingResolver` provides a controlled response for cyclic blocking states rather than ad hoc movement decisions.

---

## Part 3 — Honest Limits

This V1 system is a software interlocking simulator and does not yet include hardware-grade proving of track vacancy through axle counters or track circuits. Fouling point geometry is represented conceptually but not fully computed against physical turnout geometry and measured clearances. Automatic block SPAD wait-and-proceed behavior, strict hold-until-last-vehicle-proved release, and explicit route locking/holding/release lifecycle state machines are only partially represented in current flow and should be treated as V2 enhancements. Shunting speed law enforcement at 15 km/h is documented in domain references but is not yet implemented as a strict runtime governor across all shunting code paths.

---

## Part 4 — Circular Deadlock

A circular deadlock occurs when each train in a set is blocked by another train in the same set, so no local move appears possible. Simple dependency ordering cannot resolve this because there is no first train that can legally move under current occupancy assumptions. The resolver therefore treats the yard state as a search problem: each legal move shifts one train into available space, generating a new state, and breadth-first exploration finds a valid sequence that reaches the goal arrangement with minimal steps. If no valid state transition chain is found, the safe action is to hold movements and keep restrictive signaling until manual intervention.

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

[Abishek Ganesh B S](https://github.com/notAbishek)
