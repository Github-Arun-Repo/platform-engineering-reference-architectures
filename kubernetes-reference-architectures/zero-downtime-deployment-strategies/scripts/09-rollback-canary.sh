#!/usr/bin/env bash
set -euo pipefail

echo "=== Rolling back: Removing Canary from traffic ==="
echo ""

echo "Step 1: Restoring stable to 4 replicas..."
kubectl scale deployment demo-app-stable -n canary-demo --replicas=4
kubectl rollout status deployment/demo-app-stable -n canary-demo --timeout=120s

echo ""
echo "Step 2: Scaling canary to 0..."
kubectl scale deployment demo-app-canary -n canary-demo --replicas=0

echo ""
kubectl get pods -n canary-demo -o wide
echo ""
echo "Rollback complete. All traffic back on Stable (v1)."
echo "Canary deployment remains at 0 replicas — delete it or keep for re-promotion."
