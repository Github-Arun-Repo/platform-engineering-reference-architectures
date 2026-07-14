#!/usr/bin/env bash
set -euo pipefail

echo "=== Cleaning up all zero-downtime demo namespaces ==="
echo ""

kubectl delete namespace rolling-demo --ignore-not-found
kubectl delete namespace blue-green-demo --ignore-not-found
kubectl delete namespace canary-demo --ignore-not-found

echo ""
echo "Waiting for namespaces to terminate..."
kubectl wait --for=delete namespace/rolling-demo --timeout=60s 2>/dev/null || true
kubectl wait --for=delete namespace/blue-green-demo --timeout=60s 2>/dev/null || true
kubectl wait --for=delete namespace/canary-demo --timeout=60s 2>/dev/null || true

echo ""
echo "Done. All demo namespaces removed."
