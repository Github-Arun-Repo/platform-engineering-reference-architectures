#!/usr/bin/env bash

echo "=== Watching Rolling Update Progress — Ctrl+C to stop ==="
echo ""

while true; do
  clear
  echo "--- Rollout Status ---"
  kubectl rollout status deployment/demo-app -n rolling-demo --timeout=3s 2>&1 || true
  echo ""
  echo "--- Pods ---"
  kubectl get pods -n rolling-demo -o wide
  echo ""
  echo "--- Active Image ---"
  kubectl get deployment demo-app -n rolling-demo \
    -o jsonpath='Image: {.spec.template.spec.containers[0].image}{"\n"}'
  echo ""
  echo "--- Rollout History ---"
  kubectl rollout history deployment/demo-app -n rolling-demo 2>/dev/null || true
  echo ""
  sleep 4
done
