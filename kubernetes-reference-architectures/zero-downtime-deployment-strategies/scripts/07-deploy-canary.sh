#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== Deploying Canary release ==="
echo "Stable (v1): 4 replicas  →  ~80% of traffic"
echo "Canary  (v2): 1 replica   →  ~20% of traffic"
echo ""

kubectl apply -f "$BASE_DIR/canary/k8s/namespace.yaml"
kubectl apply -f "$BASE_DIR/canary/k8s/configmap-stable.yaml"
kubectl apply -f "$BASE_DIR/canary/k8s/configmap-canary.yaml"
kubectl apply -f "$BASE_DIR/canary/k8s/deployment-stable.yaml"
kubectl apply -f "$BASE_DIR/canary/k8s/deployment-canary.yaml"
kubectl apply -f "$BASE_DIR/canary/k8s/service.yaml"

echo ""
echo "Waiting for deployments to be ready..."
kubectl rollout status deployment/demo-app-stable -n canary-demo --timeout=120s
kubectl rollout status deployment/demo-app-canary -n canary-demo --timeout=120s

echo ""
kubectl get pods -n canary-demo -o wide
echo ""
echo "Canary deployed. One service (app: demo) routes to all 5 pods."
