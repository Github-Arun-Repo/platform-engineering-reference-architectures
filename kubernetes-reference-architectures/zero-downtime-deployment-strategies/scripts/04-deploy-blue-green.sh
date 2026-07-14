#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== Deploying Blue/Green environments ==="
echo ""

kubectl apply -f "$BASE_DIR/blue-green/k8s/namespace.yaml"
kubectl apply -f "$BASE_DIR/blue-green/k8s/configmap-blue.yaml"
kubectl apply -f "$BASE_DIR/blue-green/k8s/configmap-green.yaml"
kubectl apply -f "$BASE_DIR/blue-green/k8s/deployment-blue.yaml"
kubectl apply -f "$BASE_DIR/blue-green/k8s/deployment-green.yaml"
kubectl apply -f "$BASE_DIR/blue-green/k8s/service.yaml"

echo ""
echo "Waiting for both deployments to be ready..."
kubectl rollout status deployment/demo-app-blue -n blue-green-demo --timeout=120s
kubectl rollout status deployment/demo-app-green -n blue-green-demo --timeout=120s

echo ""
kubectl get pods -n blue-green-demo -o wide
echo ""
echo "--- Service selector (should be: slot=blue) ---"
kubectl get svc demo-app -n blue-green-demo -o jsonpath='selector: {.spec.selector}{"\n"}'
echo ""
echo "Both environments are running. Service is currently pointing to Blue (v1)."
