#!/usr/bin/env bash
set -euo pipefail

echo "=== Rolling back: Switching Service to Blue (v1) ==="
echo ""

kubectl patch svc demo-app -n blue-green-demo \
  --type='json' \
  -p='[{"op":"replace","path":"/spec/selector/slot","value":"blue"}]'

echo ""
echo "Service now pointing to:"
kubectl get svc demo-app -n blue-green-demo \
  -o jsonpath='  selector: {.spec.selector}{"\n"}'

echo ""
echo "Rollback complete. All traffic back on Blue (v1)."
