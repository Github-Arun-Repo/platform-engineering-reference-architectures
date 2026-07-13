# Kubernetes Reference Architectures

Production-oriented Kubernetes architecture patterns designed for platform teams that want reusable, tested blueprints rather than one-off manifests.

---

## Why This Section Exists

This repository already teaches GitOps and CI/CD patterns. The Kubernetes section exists to complete the platform story:

- CI/CD builds immutable artifacts
- GitOps reconciles desired state
- Kubernetes architecture patterns define safe runtime boundaries for teams and workloads

We created this section to teach engineers how to design Kubernetes platforms that are secure, multi-tenant, and operationally predictable.

---

## Quick Start - Choose Your Path

**I want to understand why these patterns matter:**
-> [Read Kubernetes architecture principles](#kubernetes-architecture-principles)

**I want to run a live demo now:**
-> [Open the Kubernetes demo runbook](./kubernetes-demo-runbook.md)

**I want to verify pre-requisites and tooling first:**
-> [Open installation and prerequisites](./installation-kubernetes-prerequisites.md)

**I want to start with multi-cluster strategy patterns:**
-> [Start with Multi-Cluster Strategy (Pattern 1)](./multi-cluster-strategy/README.md)

---

## Kubernetes Architecture Principles

Before applying any pattern, align on foundational platform principles:

**Isolation by Default**
Workloads from different teams should be isolated by namespace, policy, and identity boundaries.

**Least Privilege Access**
Service accounts and users should get only the permissions they need in their own scope.

**Resource Governance**
Every tenant namespace should enforce quotas and default limits to prevent noisy-neighbor incidents.

**Network Segmentation**
Cross-namespace communication should be explicit and allow-listed, not open by default.

**Repeatable Operations**
Patterns must be reproducible via declarative manifests and runbooks, with failure tests included.

---

## Pattern Catalog

| Pattern | What It Demonstrates | Documentation |
|---|---|---|
| **Pattern 1: Multi-Cluster Strategy (Shared Cluster Multi-Tenancy Baseline)** | Multiple teams safely sharing one Kubernetes cluster using namespace isolation, quotas, limits, RBAC, network policies, service accounts, and secret boundaries. | [Explore pattern](./multi-cluster-strategy/README.md) |

---

## Learning Path

1. Read the pattern README for architecture and design trade-offs.
2. Apply manifests to a Kubernetes cluster.
3. Execute failure tests from the runbook.
4. Observe policy enforcement and platform guardrails.
5. Adapt quotas, RBAC, and network policy rules for your organization.

---

## Production Readiness Checklist

- Namespace-per-team model implemented
- ResourceQuota and LimitRange enforced per namespace
- Team-specific service accounts and RBAC bindings
- Default-deny network policy with explicit allow rules
- Secret scope limited to namespace boundaries
- Failure tests run and documented

---

## Next Patterns Planned

- True multi-cluster fleet strategy (cluster-per-environment and cluster-per-business-domain)
- Multi-cluster traffic management and failover
- Cluster policy enforcement with admission controls
- Workload identity and secret externalization patterns
