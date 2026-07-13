# Multi-Cluster Strategy - Pattern 1

## Shared Cluster Multi-Tenancy Baseline

This first strategy starts from a practical assumption: Kubernetes is already installed, and multiple teams need to share one cluster safely.

Although this is under "multi-cluster strategy", this baseline pattern is intentionally single-cluster. It establishes the tenant guardrails that must exist before scaling into true multi-cluster fleet patterns.

---

## What This Pattern Teaches

How multiple teams or applications can safely share one Kubernetes cluster using:

- Namespace isolation
- Resource quotas
- Default CPU and memory limits
- RBAC per team
- Network isolation
- Service accounts
- Secrets separation

---

## Architecture

```text
Shared Kubernetes cluster
        |
        |-- Team A namespace
        |    |-- ResourceQuota
        |    |-- LimitRange
        |    |-- RBAC
        |    `-- NetworkPolicy
        |
        `-- Team B namespace
             |-- ResourceQuota
             |-- LimitRange
             |-- RBAC
             `-- NetworkPolicy
```

---

## Folder Layout

```text
multi-cluster-strategy/
├── README.md
├── multi-cluster-demo-runbook.md
└── k8s/
    └── shared-cluster/
        ├── kustomization.yaml
        ├── team-a/
        │   ├── kustomization.yaml
        │   ├── namespace.yaml
        │   ├── resourcequota.yaml
        │   ├── limitrange.yaml
        │   ├── serviceaccount.yaml
        │   ├── rbac.yaml
        │   ├── networkpolicy.yaml
        │   ├── secret.yaml
        │   ├── deployment.yaml
        │   └── service.yaml
        └── team-b/
            ├── kustomization.yaml
            ├── namespace.yaml
            ├── resourcequota.yaml
            ├── limitrange.yaml
            ├── serviceaccount.yaml
            ├── rbac.yaml
            ├── networkpolicy.yaml
            ├── secret.yaml
            ├── deployment.yaml
            └── service.yaml
```

---

## When To Use

- Platform teams operating a shared Kubernetes cluster
- Early-stage multi-tenant platform onboarding
- Internal environments where teams share control-plane and worker nodes
- Learning and validating namespace-level guardrails before multi-cluster expansion

## When Not To Use

- Strict regulatory boundaries requiring hard physical or account separation
- Tenant workloads that require dedicated cluster-level controls
- High-risk workloads with conflicting runtime dependencies and security profiles

---

## Operational Outcomes

By applying this pattern:

- Team A cannot manage Team B resources
- One namespace cannot consume all cluster CPU or memory
- Default limits are enforced for pods that omit resources
- Cross-namespace traffic is blocked by default
- Secrets remain namespace-scoped

---

## Run It

Follow the complete step-by-step execution and failure tests here:

- [multi-cluster-demo-runbook.md](./multi-cluster-demo-runbook.md)
