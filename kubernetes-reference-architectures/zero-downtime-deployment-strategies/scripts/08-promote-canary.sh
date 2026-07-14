#!/usr/bin/env bash
set -euo pipefail

echo "=== Promoting Canary to 100% ==="
echo ""

echo "Step 1: Scaling canary to 4 replicas (50/50 split)..."
kubectl scale deployment demo-app-canary -n canary-demo --replicas=4
kubectl rollout status deployment/demo-app-canary -n canary-demo --timeout=120s

echo ""
echo "Step 2: Scaling stable to 0 (draining v1)..."
kubectl scale deployment demo-app-stable -n canary-demo --replicas=0

echo ""
kubectl get pods -n canary-demo -o wide
echo ""
echo "Promotion complete. All traffic on v2 (canary)."
echo "Stable deployment still exists at 0 replicas for emergency restore."
