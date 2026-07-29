#!/usr/bin/env bash
set -euo pipefail

echo "=== Cleaning up Kyverno image verification demo ==="

echo "Deleting demo deployment and namespace..."
kubectl delete deployment todo-app-signed -n kyverno-verify-demo --ignore-not-found
kubectl delete namespace kyverno-verify-demo --ignore-not-found

echo "Deleting Kyverno policies..."
kubectl delete clusterpolicy verify-cosign-signature-agovindasamy-arun-audit --ignore-not-found
kubectl delete clusterpolicy verify-cosign-signature-agovindasamy-arun-enforce --ignore-not-found

echo "Waiting for namespace deletion..."
kubectl wait --for=delete namespace/kyverno-verify-demo --timeout=90s 2>/dev/null || true

echo "Done."
