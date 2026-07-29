#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== Applying namespace and audit policy ==="

kubectl apply -f "$BASE_DIR/k8s/namespace.yaml"
kubectl apply -f "$BASE_DIR/k8s/policy-cosign-verify-audit.yaml"

kubectl get clusterpolicy verify-cosign-signature-agovindasamy-arun-audit -o yaml | grep -E "name:|validationFailureAction:" -A1

echo "Audit policy applied."
