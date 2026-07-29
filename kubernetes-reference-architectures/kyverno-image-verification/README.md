# Kyverno Image Signature Verification (Cosign)

This pattern shows how to enforce image signature verification in Kubernetes using Kyverno and Cosign public keys, using the same signed image identity and Cosign public key exported by the Jenkins CI/CD pipeline.

- Registry: `docker.io`
- Image repository: `agovindasamy/arun`
- CI/CD repository: `https://github.com/Github-Arun-Repo/platform-engineering-reference-architectures.git`
- Cosign public key source: `security-reports/cosign.pub`

---

## Quick Start — Choose Your Path

**I want the full timed runbook:**
→ [Run the Kyverno runbook](./kyverno-runbook.md)

**I want audit mode first (recommended):**
→ [Audit Mode](#audit-mode)

**I want to move to enforce mode:**
→ [Enforce Mode](#enforce-mode)

**I want to understand test flow:**
→ [Testing Flow](#testing-flow)

---

## Why This Pattern Exists

Cosign signing in CI proves the image was signed. But runtime policy enforcement is what prevents unsigned or untrusted images from entering cluster workloads.

This pattern links CI and Kubernetes controls:

1. Jenkins signs image digests with Cosign.
2. Jenkins exports `cosign.pub` as public verification material.
3. Kyverno verifies image signatures at admission time.
4. Teams start in `Audit` mode, then switch to `Enforce` when ready.

---

## Audit Mode

Audit mode records policy violations without blocking deployments.

Use this to:
- validate policy wiring safely
- baseline current workloads
- identify rollout risks before strict enforcement

Policy file:
- `k8s/policy-cosign-verify-audit.yaml`

---

## Enforce Mode

Enforce mode blocks non-compliant admissions.

Use this after audit-mode validation is stable.

Policy file:
- `k8s/policy-cosign-verify-enforce.yaml`

---

## Testing Flow

1. Install Kyverno.
2. Apply namespace + audit policy.
3. Deploy the same image used by Jenkins (`docker.io/agovindasamy/arun:latest`).
4. Observe policy events/logs.
5. Switch to enforce mode.
6. Re-apply workloads and validate admission behavior.

Runbook covers this end to end:
- [kyverno-runbook.md](./kyverno-runbook.md)

---

## Folder Layout

```text
kyverno-image-verification/
├── README.md                               ← this file
├── kyverno-runbook.md                      ← timed runbook, directly executable
├── k8s/
│   ├── namespace.yaml                      ← kyverno-verify-demo namespace
│   ├── deployment-signed-image.yaml        ← same image family as Jenkins pipeline
│   ├── policy-cosign-verify-audit.yaml     ← validationFailureAction: Audit
│   └── policy-cosign-verify-enforce.yaml   ← validationFailureAction: Enforce
└── scripts/
    ├── 00-cleanup.sh
    ├── 01-install-kyverno.sh
    ├── 02-apply-audit-policy.sh
    ├── 03-deploy-signed-image.sh
    ├── 04-switch-enforce-policy.sh
    └── 05-test-policy-events.sh
```

---

## Navigation

- [Back to Kubernetes Reference Architectures](../README.md)
- [Kubernetes runbook](../kubernetes-runbook.md)
- [CI/CD reference architecture](../../cicd-reference-architectures/README.md)
