# Kyverno Image Signature Verification — Runbook

**Repo:** `https://github.com/Github-Arun-Repo/platform-engineering-reference-architectures.git`
**Base folder:** `kubernetes-reference-architectures/kyverno-image-verification/` (all commands run from here)
**Cluster:** Standalone Kubernetes on EC2
**Presenter:** Arunasalam Govindasamy

---

## Repo Layout

```text
kyverno-image-verification/
├── kyverno-runbook.md
├── k8s/
│   ├── namespace.yaml
│   ├── deployment-signed-image.yaml
│   ├── policy-cosign-verify-audit.yaml
│   └── policy-cosign-verify-enforce.yaml
└── scripts/
    ├── 00-cleanup.sh
    ├── 01-install-kyverno.sh
    ├── 02-apply-audit-policy.sh
    ├── 03-deploy-signed-image.sh
    ├── 04-switch-enforce-policy.sh
    └── 05-test-policy-events.sh
```

---

## Timing Plan

- Pre-flight: ≈ 5 min
- Part 1 — Kyverno install and baseline: ≈ 10 min
- Part 2 — Audit policy verification: ≈ 10 min
- Part 3 — Enforce-mode switch: ≈ 10 min
- Part 4 — Validation and evidence checks: ≈ 10 min
- Cleanup: ≈ 5 min
- **Total: ≈ 50 min**

---

## 0. Pre-Flight

```bash
# Pull latest
cd ~/projects/platform-engineering-reference-architectures && git pull

# Move into working directory — all commands run from here
cd kubernetes-reference-architectures/kyverno-image-verification

# Make scripts executable
chmod +x scripts/*.sh

# Verify cluster access
kubectl cluster-info
kubectl get nodes

# Clean slate
./scripts/00-cleanup.sh

echo "Ready."
```

---
---

# PART 1 — Install Kyverno (≈10 min)

> Goal: Install Kyverno admission control and verify readiness.

```bash
./scripts/01-install-kyverno.sh
```

Verification:
```bash
kubectl get pods -n kyverno
kubectl get crd | grep kyverno
```

Expected:
- Kyverno admission controller pod is Running
- Kyverno CRDs are present

---
---

# PART 2 — Audit Policy Flow (≈10 min)

> Goal: Apply non-blocking policy and deploy the same image identity used by Jenkins pipeline.

## 2.1 Apply namespace and audit policy

```bash
./scripts/02-apply-audit-policy.sh
```

## 2.2 Deploy signed image workload

```bash
./scripts/03-deploy-signed-image.sh
```

This deployment uses:
- `docker.io/agovindasamy/arun:latest`

which matches the Jenkins pipeline image repository.

## 2.3 Inspect policy/evidence signals

```bash
./scripts/05-test-policy-events.sh
```

Additional checks:
```bash
kubectl get clusterpolicy
kubectl describe clusterpolicy verify-cosign-signature-agovindasamy-arun-audit
```

---
---

# PART 3 — Switch to Enforce Mode (≈10 min)

> Goal: Transition from observe-only to admission-enforced mode.

```bash
./scripts/04-switch-enforce-policy.sh
```

Re-apply deployment to force fresh admission review:

```bash
kubectl rollout restart deployment/todo-app-signed -n kyverno-verify-demo
kubectl rollout status deployment/todo-app-signed -n kyverno-verify-demo --timeout=180s || true
```

Inspect current policy mode:

```bash
kubectl get clusterpolicy verify-cosign-signature-agovindasamy-arun-enforce -o yaml | grep validationFailureAction
```

Expected:
- `validationFailureAction: Enforce`

---
---

# PART 4 — Validation Drill (≈10 min)

> Goal: Validate runtime controls and operational evidence.

1. Verify workloads in demo namespace:
```bash
kubectl get deploy,pods -n kyverno-verify-demo -o wide
```

2. Inspect Kyverno logs for verifyImages decisions:
```bash
kubectl logs -n kyverno deployment/kyverno-admission-controller --tail=200
```

3. Confirm policy inventory:
```bash
kubectl get clusterpolicy
```

4. (Optional) If your cluster version supports policy reports:
```bash
kubectl get polr -n kyverno-verify-demo
kubectl get cpolr
```

---
---

# Cleanup (≈5 min)

```bash
./scripts/00-cleanup.sh
```

---

## Notes

- This pattern intentionally uses the same repository and image coordinates defined in Jenkins pipeline (`docker.io/agovindasamy/arun`).
- The embedded Cosign public key in policies is the same key material exported to `security-reports/cosign.pub` during Jenkins signing.
- Start with `Audit`, then move to `Enforce` after your team validates expected behavior in your cluster.

---

## Navigation

- [Back to pattern README](./README.md)
- [Back to Kubernetes Reference Architectures](../README.md)
- [Back to Kubernetes runbook](../kubernetes-runbook.md)
