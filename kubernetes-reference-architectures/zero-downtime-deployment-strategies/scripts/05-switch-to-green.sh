#!/usr/bin/env bash
set -euo pipefail

echo "=== Switching Service to Green (v2) ==="
echo ""
echo "Before:"
kubectl get svc demo-app -n blue-green-demo \
  -o jsonpath='  selector: {.spec.selector}{"\n"}'

kubectl patch svc demo-app -n blue-green-demo \
  --type='json' \
  -p='[{"op":"replace","path":"/spec/selector/slot","value":"green"}]'

echo ""
echo "After:"
kubectl get svc demo-app -n blue-green-demo \
  -o jsonpath='  selector: {.spec.selector}{"\n"}'

echo ""
echo "Traffic is now routed to Green (v2)."
echo "Blue is still running — rollback is one command: ./06-rollback-to-blue.sh"
