#!/usr/bin/env bash
set -euo pipefail

echo "=== Inspecting Kyverno policy events and reports ==="

echo "Kyverno admission/controller logs (tail):"
kubectl logs -n kyverno deployment/kyverno-admission-controller --tail=50 || true

echo "\nPolicy reports in demo namespace:"
kubectl get polr -n kyverno-verify-demo 2>/dev/null || echo "No namespaced policy reports found (version/setup dependent)."

echo "\nCluster policy reports:"
kubectl get cpolr 2>/dev/null || echo "No cluster policy reports found (version/setup dependent)."

echo "\nCurrent demo pods:"
kubectl get pods -n kyverno-verify-demo -o wide || true
