#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== Deploying v1 for Rolling Update demo ==="
echo ""

kubectl apply -f "$BASE_DIR/rolling-update/k8s/namespace.yaml"
kubectl apply -f "$BASE_DIR/rolling-update/k8s/configmap-v1.yaml"
kubectl apply -f "$BASE_DIR/rolling-update/k8s/deployment-v1.yaml"
kubectl apply -f "$BASE_DIR/rolling-update/k8s/service.yaml"
kubectl apply -f "$BASE_DIR/rolling-update/k8s/pdb.yaml"

echo ""
echo "Waiting for pods to be ready..."
kubectl rollout status deployment/demo-app -n rolling-demo --timeout=120s

echo ""
kubectl get pods -n rolling-demo -o wide
kubectl get svc -n rolling-demo
kubectl get pdb -n rolling-demo
echo ""
echo "v1 is live. 4 pods running nginx:1.24-alpine."
