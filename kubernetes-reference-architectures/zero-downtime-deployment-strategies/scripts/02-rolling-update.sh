#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== Triggering Rolling Update to v2 ==="
echo ""
echo "Applying v2 ConfigMap..."
kubectl apply -f "$BASE_DIR/rolling-update/k8s/configmap-v2.yaml"

echo ""
echo "Applying v2 Deployment (image: nginx:1.25-alpine)..."
kubectl apply -f "$BASE_DIR/rolling-update/k8s/deployment-v2.yaml"

echo ""
echo "Rolling update in progress."
echo "Run 03-watch-rolling.sh in another terminal to observe the pod-by-pod replacement."
