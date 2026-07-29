#!/usr/bin/env bash
set -euo pipefail

echo "=== Installing Kyverno ==="

kubectl create namespace kyverno --dry-run=client -o yaml | kubectl apply -f -
helm repo add kyverno https://kyverno.github.io/kyverno/ >/dev/null
helm repo update >/dev/null
helm upgrade --install kyverno kyverno/kyverno \
  -n kyverno \
  --set admissionController.replicas=1

kubectl rollout status deployment/kyverno-admission-controller -n kyverno --timeout=180s
kubectl get pods -n kyverno

echo "Kyverno installation complete."
