# Zero-Downtime Deployment Strategies

You have a new version of your application and need to get it into production without your users noticing. Kubernetes gives you three distinct approaches to this problem, and they make genuinely different trade-offs around speed, safety, resource cost, and rollback complexity.

This pattern covers all three: rolling update, blue/green, and canary. One sample application, three deployment paths, fully executable from a single runbook.

---

## Quick Start — Choose Your Path

**I want the full timed runbook:**
→ [Run the zero-downtime runbook](./zero-downtime-runbook.md)

**I want to understand rolling updates first:**
→ [Rolling Update](#rolling-update)

**I want to understand blue/green deployment:**
→ [Blue/Green Deployment](#bluegreen-deployment)

**I want to understand canary releases:**
→ [Canary Deployment](#canary-deployment)

**I want to pick the right strategy for my situation:**
→ [Decision Framework](#decision-framework)

---

## Why This Pattern Exists

The default Kubernetes strategy (`Recreate`) terminates all existing pods before creating new ones. For a few seconds or longer, nothing is serving your application. That's fine in development. It's not acceptable when real users are involved.

Even teams using `RollingUpdate` often don't think through what happens when the rollout breaks halfway, how quickly they can roll back, whether their application handles two versions running simultaneously, or what happens to in-flight requests during the switchover.

Rolling update, blue/green, and canary each sit at a different point on the risk-vs-speed-vs-cost spectrum. The goal of this pattern is to make those trade-offs explicit, show the mechanism behind each one, and give you runnable examples so you can verify the behavior yourself rather than assume it.

---

## The Sample Application

All three strategies use nginx served with a ConfigMap-injected page that clearly identifies which version and which deployment track is responding. This makes it possible to confirm by curling the service exactly which version is live during and after each deployment step.

```text
v1: nginx:1.24-alpine — page shows "Version: v1"
v2: nginx:1.25-alpine — page shows "Version: v2"
```

No custom image builds. No registry required.

---

## Rolling Update

The default Kubernetes Deployment strategy. Kubernetes replaces old pods with new ones gradually — it will not remove an old pod until a replacement has passed its readiness probe.

**How it works in this pattern:**
- 4 replicas running v1
- `maxUnavailable: 0` — no pod is terminated until its replacement is ready
- `maxSurge: 1` — one extra pod is created during the transition (briefly 5 pods, then 4)
- A PodDisruptionBudget keeps at least 3 pods available during node maintenance

**The transition:**
```text
Start:      [v1][v1][v1][v1]          4 pods
Surge:      [v1][v1][v1][v1][v2]     5 pods — v2 passes readiness
Remove one: [v1][v1][v1][v2]          4 pods
...repeat until:
End:        [v2][v2][v2][v2]          4 pods — done
```

At no point during this transition does the number of ready pods drop below 4.

**When to use rolling update:**
- Most stateless web services and APIs
- Your application tolerates a window where v1 and v2 are both serving requests simultaneously
- You don't need instant rollback — `kubectl rollout undo` is reliable, it just takes time
- You don't have the budget to run a full duplicate environment

**When NOT to use rolling update:**
- Database migrations that break the old schema: v1 pods crash when v2 applies a destructive change
- API version changes where v1 and v2 return incompatible response shapes and clients can't handle mixed responses
- You need a hard, auditable cutover point

**Rollback:**
```bash
kubectl rollout undo deployment/demo-app -n rolling-demo
```

---

## Blue/Green Deployment

Two identical environments run simultaneously. Blue is the current live version. Green is the new version. A single patch to the service selector flips all traffic from blue to green — atomically, from the perspective of new connections.

**How it works in this pattern:**
- Blue: 3 replicas of v1, label `slot: blue`
- Green: 3 replicas of v2, label `slot: green`
- Service selector starts at `slot: blue`
- Cutover: patch the selector to `slot: green`
- Rollback: patch it back to `slot: blue`

```text
Service → [v1][v1][v1]    [v2][v2][v2]
          Blue (live)      Green (idle, fully ready)

Patch selector →

Service → [v1][v1][v1]    [v2][v2][v2]
          Blue (idle)      Green (live)
```

The switch is one `kubectl patch` command. Blue keeps running the entire time — rollback is the same command in reverse.

**When to use blue/green:**
- You need instant, verified cutover with instant rollback
- Database migrations that cannot be applied to two versions simultaneously — prepare the schema on green before switching
- Release events where you want green fully validated before a single user hits it
- You have the resources to run two full environments at the same time

**When NOT to use blue/green:**
- Resource-constrained clusters — you're paying for double the pods constantly
- Stateful applications with sticky sessions or in-flight requests that cannot be abandoned on cutover
- Very frequent deployments — maintaining two full environments on every push gets expensive

**Rollback:**
```bash
kubectl patch svc demo-app -n blue-green-demo \
  --type='json' \
  -p='[{"op":"replace","path":"/spec/selector/slot","value":"blue"}]'
```

Rollback is instantaneous. Blue never stopped running.

---

## Canary Deployment

A small fraction of live traffic goes to the new version. Watch it, measure it, and increase the percentage if it looks good. If it doesn't, scale the canary to zero.

**How it works in this pattern:**
- Stable: 4 replicas of v1, label `track: stable`
- Canary: 1 replica of v2, label `track: canary`
- Service selector: `app: demo` — matches both deployments
- Traffic is split proportionally to replica count: 4+1=5 pods → ~80% stable, ~20% canary

This is the native Kubernetes canary approach. It does not require a service mesh or Ingress weight annotations. The limitation: traffic percentage is controlled by replica ratio, not by an explicit weight. You can't route 1% without running 99 stable replicas.

**Promotion path:**
```text
Start:    stable=4, canary=1   (~80% / ~20%)
Step up:  stable=4, canary=4   (~50% / ~50%)
Commit:   stable=0, canary=4   (0% / 100%)
```

**Rollback:**
```bash
kubectl scale deployment demo-app-canary -n canary-demo --replicas=0
# stable stays at 4, absorbs all traffic immediately
```

**When to use canary:**
- High-stakes releases where real-traffic validation before full rollout is worth the overhead
- Features with quality signals that synthetic tests miss — search ranking, recommendation accuracy, latency under real load patterns
- Teams with observability in place to actually detect regressions in the canary slice

**When NOT to use canary:**
- Low-traffic applications where 20% canary gets too few requests to be meaningful
- API changes where v1 and v2 are incompatible — you can't serve two protocol versions from one service endpoint safely
- Teams without the monitoring to catch regressions: it becomes theater, not safety

---

## Decision Framework

| If your situation is... | Use this |
|---|---|
| Standard stateless app, no protocol change | Rolling Update |
| Application can handle v1 and v2 serving simultaneously | Rolling Update |
| Need instant cutover and instant rollback | Blue/Green |
| Destructive schema migration | Blue/Green |
| Release event — want full validation before any traffic | Blue/Green |
| Resource-constrained cluster | Rolling Update |
| High-stakes release, need real-traffic signal | Canary |
| Frequent deployments (daily or more) | Rolling Update |
| Large e-commerce release on peak traffic day | Blue/Green |
| No service mesh, but want gradual traffic shift | Canary (replica ratio) |

---

## Architecture Overview

```text
Rolling Update
──────────────
        Service (selector: app=demo-app)
              │
    ┌─────────┴──────────┐
  [v1][v1][v1][v1]      [v2]      ← surge: v2 passes readiness
  [v1][v1][v1]          [v2]      ← one v1 removed
  ...
  [v2][v2][v2][v2]               ← complete


Blue/Green
──────────
        Service ──────► slot: blue ──► [v1][v1][v1]
                                        [v2][v2][v2]  ← idle, fully ready

        patch selector →

        Service ──────► slot: green ──► [v2][v2][v2]
                         [v1][v1][v1]                 ← idle, rollback ready


Canary
──────
        Service (selector: app=demo)
              │
    ┌─────────┴──────────────────────────┐
  [v1-stable][v1-stable][v1-stable][v1-stable][v2-canary]
  ←─────────── ~80% stable ────────────── ~20% canary ──►
```

---

## Folder Layout

```text
zero-downtime-deployment-strategies/
├── README.md                              ← this file
├── zero-downtime-runbook.md               ← timed runbook, directly executable
├── rolling-update/
│   └── k8s/
│       ├── namespace.yaml                 ← rolling-demo namespace
│       ├── configmap-v1.yaml              ← v1 page content
│       ├── configmap-v2.yaml              ← v2 page content
│       ├── deployment-v1.yaml             ← 4 replicas, maxUnavailable=0, maxSurge=1
│       ├── deployment-v2.yaml             ← same params, nginx:1.25-alpine
│       ├── service.yaml                   ← ClusterIP, selector: app=demo-app
│       └── pdb.yaml                       ← minAvailable: 3
├── blue-green/
│   └── k8s/
│       ├── namespace.yaml                 ← blue-green-demo namespace
│       ├── configmap-blue.yaml            ← blue environment page content
│       ├── configmap-green.yaml           ← green environment page content
│       ├── deployment-blue.yaml           ← 3 replicas, slot: blue
│       ├── deployment-green.yaml          ← 3 replicas, slot: green
│       └── service.yaml                   ← selector starts at slot: blue
├── canary/
│   └── k8s/
│       ├── namespace.yaml                 ← canary-demo namespace
│       ├── configmap-stable.yaml          ← stable track page content
│       ├── configmap-canary.yaml          ← canary track page content
│       ├── deployment-stable.yaml         ← 4 replicas, track: stable
│       ├── deployment-canary.yaml         ← 1 replica, track: canary
│       └── service.yaml                   ← selector: app=demo (matches both)
└── scripts/
    ├── 00-cleanup.sh                      ← removes all three demo namespaces
    ├── 01-deploy-rolling.sh               ← deploy v1 baseline
    ├── 02-rolling-update.sh               ← apply v2 configmap + deployment
    ├── 03-watch-rolling.sh                ← watch rollout in a loop
    ├── 04-deploy-blue-green.sh            ← deploy blue and green together
    ├── 05-switch-to-green.sh              ← patch service selector to green
    ├── 06-rollback-to-blue.sh             ← patch service selector back to blue
    ├── 07-deploy-canary.sh                ← deploy stable + canary
    ├── 08-promote-canary.sh               ← scale canary to 4, stable to 0
    └── 09-rollback-canary.sh              ← scale canary to 0, stable to 4
```

---

## Learning Path

1. Read this README to understand why the three strategies exist and what trade-offs each makes.
2. Execute [zero-downtime-runbook.md](./zero-downtime-runbook.md) from top to bottom.
3. Run the failure scenarios in Part 4 of the runbook — especially the broken readiness probe test.
4. Run cleanup and verify the cluster is clean.

---

## Navigation

- [Back to Kubernetes Reference Architectures](../README.md)
- [Pattern 1: Shared Cluster Multi-Tenancy](../multi-cluster-strategy/README.md)
- [Pattern 2: Autoscaling (HPA and VPA)](../autoscaling-reference-patterns/README.md)
