# Zero-Downtime Deployment Strategies — Runbook

**Repo:** `https://github.com/Github-Arun-Repo/platform-engineering-reference-architectures.git`
**Base folder:** `kubernetes-reference-architectures/zero-downtime-deployment-strategies/` (all commands run from here)
**Cluster:** Standalone Kubernetes on EC2
**Presenter:** Arunasalam Govindasamy

---

## Repo Layout

```text
zero-downtime-deployment-strategies/
├── zero-downtime-runbook.md          ← this file
├── rolling-update/k8s/
│   ├── namespace.yaml                # rolling-demo namespace
│   ├── configmap-v1.yaml             # v1 HTML content
│   ├── configmap-v2.yaml             # v2 HTML content
│   ├── deployment-v1.yaml            # 4 replicas, maxUnavailable=0, maxSurge=1
│   ├── deployment-v2.yaml            # same params, nginx:1.25-alpine
│   ├── service.yaml                  # ClusterIP
│   └── pdb.yaml                      # minAvailable: 3
├── blue-green/k8s/
│   ├── namespace.yaml                # blue-green-demo namespace
│   ├── configmap-blue.yaml
│   ├── configmap-green.yaml
│   ├── deployment-blue.yaml          # 3 replicas, slot: blue
│   ├── deployment-green.yaml         # 3 replicas, slot: green
│   └── service.yaml                  # selector: slot=blue initially
├── canary/k8s/
│   ├── namespace.yaml                # canary-demo namespace
│   ├── configmap-stable.yaml
│   ├── configmap-canary.yaml
│   ├── deployment-stable.yaml        # 4 replicas, track: stable
│   ├── deployment-canary.yaml        # 1 replica, track: canary
│   └── service.yaml                  # selector: app=demo (matches both)
└── scripts/
    ├── 00-cleanup.sh
    ├── 01-deploy-rolling.sh
    ├── 02-rolling-update.sh
    ├── 03-watch-rolling.sh           # watch loop — run in a second terminal
    ├── 04-deploy-blue-green.sh
    ├── 05-switch-to-green.sh
    ├── 06-rollback-to-blue.sh
    ├── 07-deploy-canary.sh
    ├── 08-promote-canary.sh
    └── 09-rollback-canary.sh
```

---

## Timing Plan

- Pre-flight: ≈ 5 min
- Part 1 — Rolling Update: ≈ 20 min
- Part 2 — Blue/Green: ≈ 15 min
- Part 3 — Canary: ≈ 20 min
- Part 4 — Failure scenarios: ≈ 15 min
- Cleanup and reset: ≈ 5 min
- **Total: ≈ 75 min**

---

## 0. Pre-Flight

```bash
# Pull latest
cd ~/projects/platform-engineering-reference-architectures && git pull

# Move into the working directory — all commands below run from here
cd kubernetes-reference-architectures/zero-downtime-deployment-strategies

# Make scripts executable
chmod +x scripts/*.sh

# Verify cluster access
kubectl cluster-info
kubectl get nodes

# Clean slate — remove any previous run of this demo
./scripts/00-cleanup.sh

echo "Ready."
```

---
---

# PART 1 — Rolling Update (≈20 min)

> **Goal:** Deploy v1, confirm it's serving, trigger a rolling update to v2, observe Kubernetes replace pods one by one without dropping below 4 ready pods, then verify rollback.

---

## 1.1 — Deploy v1

```bash
./scripts/01-deploy-rolling.sh
```

👉 4 pods running `nginx:1.24-alpine`. Service is live. PDB is in place.

Verify:
```bash
kubectl get pods -n rolling-demo -o wide
kubectl get deployment demo-app -n rolling-demo
kubectl get pdb demo-app-pdb -n rolling-demo
```

Check what v1 is actually serving:
```bash
kubectl run curl-test --image=curlimages/curl:8.10.1 --restart=Never \
  -n rolling-demo --command -- sleep 600
kubectl wait --for=condition=Ready pod/curl-test -n rolling-demo --timeout=60s
kubectl exec curl-test -n rolling-demo -- curl -s http://demo-app
```

👉 Expected: response shows `Version: v1`

---

## 1.2 — Review the Rolling Update Strategy

Before triggering the update, understand the parameters:

```bash
kubectl get deployment demo-app -n rolling-demo \
  -o jsonpath='{.spec.strategy}' | python3 -m json.tool
```

Expected:
```json
{
    "type": "RollingUpdate",
    "rollingUpdate": {
        "maxUnavailable": 0,
        "maxSurge": 1
    }
}
```

👉 `maxUnavailable: 0` — the number of ready pods never drops below 4 during the update.
👉 `maxSurge: 1` — Kubernetes creates one extra pod first, waits for readiness, then removes one old pod. Repeat.

---

## 1.3 — Trigger Rolling Update to v2

Open a **second terminal** and start the watcher before applying the update:

```bash
# In second terminal — run from the same base folder:
./scripts/03-watch-rolling.sh
```

In the **main terminal**, apply the update:

```bash
./scripts/02-rolling-update.sh
```

Watch the second terminal. The sequence unfolds over 1-2 minutes:

```text
--- Pods ---
demo-app-xxxx-aaaa   1/1   Running   nginx:1.24-alpine
demo-app-xxxx-bbbb   1/1   Running   nginx:1.24-alpine
demo-app-xxxx-cccc   1/1   Running   nginx:1.24-alpine
demo-app-xxxx-dddd   1/1   Running   nginx:1.24-alpine
demo-app-yyyy-eeee   0/1   ContainerCreating   nginx:1.25-alpine   ← new pod coming up
```

Then:
```text
demo-app-xxxx-aaaa   1/1   Terminating         nginx:1.24-alpine   ← old pod leaving
demo-app-yyyy-eeee   1/1   Running             nginx:1.25-alpine   ← new pod ready
```

👉 At every point in this output, at least 4 pods are ready. The old pod goes down only after the new one passes readiness.

---

## 1.4 — Verify the Update Completed

```bash
kubectl rollout status deployment/demo-app -n rolling-demo
# Expected: deployment "demo-app" successfully rolled out

kubectl get pods -n rolling-demo -o wide
# Expected: all 4 pods on nginx:1.25-alpine

kubectl exec curl-test -n rolling-demo -- curl -s http://demo-app
# Expected: Version: v2
```

Check the rollout history:
```bash
kubectl rollout history deployment/demo-app -n rolling-demo
```

👉 Revision 1 is v1, revision 2 is v2.

---

## 1.5 — Rollback to v1

```bash
kubectl rollout undo deployment/demo-app -n rolling-demo
kubectl rollout status deployment/demo-app -n rolling-demo
```

```bash
kubectl get pods -n rolling-demo -o wide
kubectl exec curl-test -n rolling-demo -- curl -s http://demo-app
```

👉 Pods back on `nginx:1.24-alpine`. Response shows `Version: v1`. The rollback is itself a rolling update — same controlled pod-by-pod replacement, same readiness gate.

---

## 1.6 — Validate the PodDisruptionBudget

The PDB guarantees at least 3 pods survive voluntary eviction operations. Confirm it's in place:

```bash
kubectl describe pdb demo-app-pdb -n rolling-demo
```

Expected output includes:
```
Min available:   3
Allowed disruptions: 1
Current:         4
```

👉 `Allowed disruptions: 1` means at most one pod can be evicted voluntarily at a time. During a node drain with 4 pods, the eviction controller will drain them one at a time — never taking the deployment below 3.

Without this PDB, a node drain could evict all pods on that node simultaneously. With a 4-replica deployment spread across fewer nodes, that could mean all 4 going down at once.

---
---

# PART 2 — Blue/Green Deployment (≈15 min)

> **Goal:** Deploy both blue (v1) and green (v2) simultaneously, confirm only blue is receiving traffic, switch to green in one command, verify instantaneous cutover, then roll back.

---

## 2.1 — Deploy Both Environments

```bash
./scripts/04-deploy-blue-green.sh
```

👉 6 pods total — 3 blue (v1) + 3 green (v2). Both are fully ready. Service currently points to blue.

Verify:
```bash
kubectl get pods -n blue-green-demo -o wide
kubectl get svc demo-app -n blue-green-demo -o jsonpath='selector: {.spec.selector}{"\n"}'
```

Expected selector: `selector: map[app:demo slot:blue]`

Test that only blue is serving:
```bash
kubectl run curl-test --image=curlimages/curl:8.10.1 --restart=Never \
  -n blue-green-demo --command -- sleep 600
kubectl wait --for=condition=Ready pod/curl-test -n blue-green-demo --timeout=60s

for i in $(seq 1 5); do
  kubectl exec curl-test -n blue-green-demo -- curl -s http://demo-app | grep -E "Environment|Version"
  echo "---"
done
```

👉 All 5 responses: `Environment: Blue | Version: v1`

---

## 2.2 — Switch to Green

```bash
./scripts/05-switch-to-green.sh
```

Test immediately:
```bash
for i in $(seq 1 5); do
  kubectl exec curl-test -n blue-green-demo -- curl -s http://demo-app | grep -E "Environment|Version"
  echo "---"
done
```

👉 All 5 responses: `Environment: Green | Version: v2`. The cutover was instantaneous.

Confirm the selector changed:
```bash
kubectl get svc demo-app -n blue-green-demo -o jsonpath='selector: {.spec.selector}{"\n"}'
# Expected: selector: map[app:demo slot:green]
```

👉 Blue is still running 3 healthy pods. The service is the only thing that changed. Rollback is one command away.

---

## 2.3 — Rollback to Blue

```bash
./scripts/06-rollback-to-blue.sh
```

```bash
for i in $(seq 1 5); do
  kubectl exec curl-test -n blue-green-demo -- curl -s http://demo-app | grep -E "Environment|Version"
  echo "---"
done
```

👉 All responses back on `Environment: Blue | Version: v1`. Time from trigger to complete rollback: under 2 seconds.

---
---

# PART 3 — Canary Deployment (≈20 min)

> **Goal:** Deploy v1 at full capacity, introduce v2 as a 20% canary, verify the traffic split across real requests, promote in steps, then demonstrate rollback.

---

## 3.1 — Deploy Stable + Canary

```bash
./scripts/07-deploy-canary.sh
```

👉 5 pods total: 4 stable (v1) + 1 canary (v2). Both deployments feed the same service.

Verify:
```bash
kubectl get pods -n canary-demo -o wide
kubectl get svc demo-app -n canary-demo -o jsonpath='selector: {.spec.selector}{"\n"}'
```

Expected selector: `selector: map[app:demo]` — matches all 5 pods regardless of track.

---

## 3.2 — Observe the Traffic Split

Run 20 requests and count by track:
```bash
kubectl run curl-test --image=curlimages/curl:8.10.1 --restart=Never \
  -n canary-demo --command -- sleep 600
kubectl wait --for=condition=Ready pod/curl-test -n canary-demo --timeout=60s

for i in $(seq 1 20); do
  kubectl exec curl-test -n canary-demo -- curl -s http://demo-app | grep "Track:"
done
```

👉 Expected: roughly 16 `Track: Stable` and 4 `Track: Canary` out of 20. The split follows the replica ratio (4:1). It won't be perfectly 80/20 on every run — kube-proxy distributes across endpoints and small sample sizes show variance.

To get a cleaner picture, run 100 requests and count:
```bash
for i in $(seq 1 100); do
  kubectl exec curl-test -n canary-demo -- curl -s http://demo-app | grep "Track:"
done | sort | uniq -c
```

👉 Should converge near 80 Stable / 20 Canary.

---

## 3.3 — Promote Canary in Steps

Satisfied with canary results? Increase the canary slice:

```bash
# Step to 50/50 first
kubectl scale deployment demo-app-canary -n canary-demo --replicas=4
kubectl rollout status deployment/demo-app-canary -n canary-demo

kubectl get pods -n canary-demo -o wide
```

👉 Now 4 stable + 4 canary = 8 pods. Run the curl loop again — should split roughly 50/50.

Commit to 100% canary:
```bash
./scripts/08-promote-canary.sh
```

```bash
kubectl get pods -n canary-demo -o wide
for i in $(seq 1 5); do
  kubectl exec curl-test -n canary-demo -- curl -s http://demo-app | grep "Track:"
done
```

👉 All responses: `Track: Canary`. Stable is at 0 replicas — still exists, recoverable.

---

## 3.4 — Rollback Canary

```bash
./scripts/09-rollback-canary.sh
```

```bash
kubectl get pods -n canary-demo -o wide
for i in $(seq 1 5); do
  kubectl exec curl-test -n canary-demo -- curl -s http://demo-app | grep "Track:"
done
```

👉 All responses back on `Track: Stable`. Canary at 0 replicas, stable at 4.

---
---

# PART 4 — Failure Scenarios (≈15 min)

> **Goal:** Verify that the guardrails actually work — rolling update pauses on a bad readiness probe, blue/green instant rollback survives a green failure, and canary rollback is immediate when issues appear.

---

## 4.1 — Rolling Update: Deploy a Broken Version

Simulate a deployment with a readiness probe pointing at a path that doesn't exist. The rollout should stall — new pods can't pass readiness, old pods are NOT removed.

```bash
# Restore v1 first if needed
kubectl apply -f rolling-update/k8s/configmap-v1.yaml
kubectl apply -f rolling-update/k8s/deployment-v1.yaml
kubectl rollout status deployment/demo-app -n rolling-demo

# Now trigger a bad rollout: new image + broken readiness path
kubectl patch deployment demo-app -n rolling-demo \
  --type='json' \
  -p='[
    {"op":"replace","path":"/spec/template/spec/containers/0/image","value":"nginx:1.25-alpine"},
    {"op":"replace","path":"/spec/template/spec/containers/0/readinessProbe/httpGet/path","value":"/this-does-not-exist"}
  ]'
```

Watch what happens:
```bash
kubectl rollout status deployment/demo-app -n rolling-demo --timeout=60s
```

👉 Expected: rollout stalls. New pods start but stay in `0/1 Running` (readiness failing, 404 from nginx on the unknown path).

```bash
kubectl get pods -n rolling-demo -o wide
```

👉 Old pods still `1/1 Running`. New pod stuck at `0/1`. The service continues sending traffic to the old running pods.

```bash
kubectl exec curl-test -n rolling-demo -- curl -s http://demo-app
```

👉 Still serving v1. The broken pod never entered the service endpoint list.

Recover with rollback:
```bash
kubectl rollout undo deployment/demo-app -n rolling-demo
kubectl rollout status deployment/demo-app -n rolling-demo
kubectl get pods -n rolling-demo -o wide
```

👉 Back to 4 healthy v1 pods.

---

## 4.2 — Blue/Green: Delete the Active Environment

Demonstrates why you should never delete the inactive environment before confirming the active one is healthy, and why you should never delete the active environment without switching first.

```bash
# Make sure service is on blue
./scripts/06-rollback-to-blue.sh

# Confirm blue is serving
kubectl exec curl-test -n blue-green-demo -- curl -s http://demo-app | grep "Environment"

# Delete blue (simulating premature cleanup)
kubectl delete deployment demo-app-blue -n blue-green-demo
```

```bash
# Service still has selector slot: blue — but there are no blue pods
kubectl get endpoints demo-app -n blue-green-demo
```

👉 Expected: endpoints show `<none>`. The service exists but has no pods to route to.

```bash
kubectl exec curl-test -n blue-green-demo -- curl -s --max-time 5 http://demo-app
```

👉 Expected: connection refused or timeout. No endpoints = no response.

Fix — switch to green (which is still running) then re-deploy blue:
```bash
./scripts/05-switch-to-green.sh

# Traffic is now on green
kubectl exec curl-test -n blue-green-demo -- curl -s http://demo-app | grep "Environment"

# Re-deploy blue for future rollback capability
kubectl apply -f blue-green/k8s/configmap-blue.yaml
kubectl apply -f blue-green/k8s/deployment-blue.yaml
kubectl rollout status deployment/demo-app-blue -n blue-green-demo --timeout=90s
```

👉 Lesson: in blue/green, keep the inactive environment running until the new one is fully confirmed. The resource cost is the price of instant rollback.

---

## 4.3 — Canary: Emergency Rollback on a Bad Canary

```bash
# Ensure canary and stable are both running
kubectl scale deployment demo-app-stable -n canary-demo --replicas=4
kubectl scale deployment demo-app-canary -n canary-demo --replicas=1
kubectl rollout status deployment/demo-app-stable -n canary-demo
kubectl rollout status deployment/demo-app-canary -n canary-demo

# Simulate catching a problem in canary (e.g. error rate spiking)
# Emergency rollback: canary to 0 replicas
kubectl scale deployment demo-app-canary -n canary-demo --replicas=0
```

```bash
kubectl get pods -n canary-demo -o wide
for i in $(seq 1 5); do
  kubectl exec curl-test -n canary-demo -- curl -s http://demo-app | grep "Track:"
done
```

👉 All responses back on `Track: Stable`. Canary is removed from the endpoint list as soon as its pod terminates (~5-10 seconds with graceful termination).

The canary deployment remains at 0 replicas — available for re-promotion or clean deletion.

---

## Final Cleanup

```bash
# Remove curl-test pods
kubectl delete pod curl-test -n rolling-demo --ignore-not-found
kubectl delete pod curl-test -n blue-green-demo --ignore-not-found
kubectl delete pod curl-test -n canary-demo --ignore-not-found

# Remove all demo namespaces
./scripts/00-cleanup.sh

# Verify
kubectl get ns | grep -E 'rolling|blue-green|canary'
# Expected: no output
```

---

## Navigation

- [README — Zero-Downtime Deployment Strategies](./README.md)
- [Back to Kubernetes Reference Architectures](../README.md)
