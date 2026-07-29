#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== Deploying signed image used by Jenkins pipeline ==="

kubectl apply -f "$BASE_DIR/k8s/deployment-signed-image.yaml"
kubectl rollout status deployment/todo-app-signed -n kyverno-verify-demo --timeout=180s || true
kubectl get pods -n kyverno-verify-demo -o wide

echo "Deployment command complete."
