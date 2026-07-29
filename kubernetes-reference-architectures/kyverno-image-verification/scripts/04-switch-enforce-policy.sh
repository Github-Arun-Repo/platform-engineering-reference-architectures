#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== Switching from audit to enforce policy ==="

kubectl delete clusterpolicy verify-cosign-signature-agovindasamy-arun-audit --ignore-not-found
kubectl apply -f "$BASE_DIR/k8s/policy-cosign-verify-enforce.yaml"

kubectl get clusterpolicy verify-cosign-signature-agovindasamy-arun-enforce -o yaml | grep -E "name:|validationFailureAction:" -A1

echo "Enforce policy is active."
