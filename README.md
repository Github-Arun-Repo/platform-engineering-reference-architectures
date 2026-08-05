# Platform Engineering Reference Architectures

A growing collection of practical infrastructure patterns, reference implementations, and production-oriented platform engineering examples.

## About This Repository

I am **Arunasalam Govindasamy**, an AWS Cloud Architect with extensive experience designing and building cloud infrastructure, Kubernetes platforms, Infrastructure as Code, GitOps, CI/CD, networking, and distributed systems.

I created this repository to share infrastructure patterns that engineers and architects can inspect, deploy, test, and learn from.

The implementations are not documentation-only examples. They have been built and validated through hands-on deployments, operational testing, failure scenarios, recovery exercises, and architecture analysis.

The repository contains practical and reusable infrastructure implementations designed to help engineers understand, deploy, and operate modern platforms.

## Reference Architectures

| Area | What It Covers | Documentation |
|---|---|---|
| **GitOps and Argo CD** | Argo CD Applications, App of Apps, ApplicationSets, automated sync, self-healing, drift detection, rollback, and recovery scenarios. | [Explore Argo CD architectures](./argocd-reference-architectures/README.md) |
| **CI/CD and Continuous Deployment** | Container image build pipelines, supply chain security, artifact versioning, vulnerability scanning, and deployment automation. This architecture pattern can be implemented with Jenkins, GitHub Actions, GitLab CI, Tekton, or Azure DevOps. This repository currently uses Jenkins; GitHub Actions is also a modern approach for GitHub-native teams. | [Explore CI/CD patterns](./supply-chain-security-ref/README.md) |
| **Kubernetes Reference Architectures** | Multi-tenant Kubernetes platform patterns, namespace isolation, quotas, RBAC boundaries, network segmentation, and operational failure testing runbooks. | [Explore Kubernetes patterns](./kubernetes-reference-architectures/README.md) |
| **Terraform Infrastructure** | Modular Terraform, multi-AZ AWS networking, secure S3 patterns, remote state, state locking, reusable modules, and architecture decisions. | [Explore Terraform patterns](./terraform/README.md) |

## Kubernetes Pattern Reference

This section lists Kubernetes patterns that engineers can refer to when designing and operating production platforms.

| Pattern | Why It Was Created | What It Teaches | Links |
|---|---|---|---|
| **Pattern 1: Shared Cluster Multi-Tenancy** | Multiple teams sharing one cluster without isolation cause resource exhaustion, cross-team security gaps, and operational chaos | Namespace boundaries, ResourceQuotas, LimitRanges, RBAC, NetworkPolicy, and Secrets with failure-focused validation | [README](./kubernetes-reference-architectures/multi-cluster-strategy/README.md) · [Runbook](./kubernetes-reference-architectures/multi-cluster-strategy/multi-cluster-runbook.md) |
| **Pattern 2: Autoscaling (HPA and VPA)** | Static resource allocation either over-provisions (wasted cost) or under-provisions (failures at peak) | HPA replica scaling and VPA right-sizing behavior, including conflict and boundary considerations | [README](./kubernetes-reference-architectures/autoscaling-reference-patterns/README.md) · [Runbook](./kubernetes-reference-architectures/autoscaling-reference-patterns/autoscaling-runbook.md) |
| **Pattern 3: Zero-Downtime Deployment Strategies** | Every team eventually ships a bad release; recovery speed and blast radius are critical | Rolling Update, Blue/Green, Canary, PodDisruptionBudget usage, and rollback workflows | [README](./kubernetes-reference-architectures/zero-downtime-deployment-strategies/README.md) · [Runbook](./kubernetes-reference-architectures/zero-downtime-deployment-strategies/zero-downtime-runbook.md) |
| **Pattern 4: Pod Health Probes** | Startup, liveness, and readiness probes look similar in YAML but trigger different runtime outcomes | Correct probe design, anti-patterns, and service-availability impact during failures | [README](./kubernetes-reference-architectures/pod-health-probes/README.md) · [Runbook](./kubernetes-reference-architectures/pod-health-probes/probes-runbook.md) |
| **Pattern 5: Kyverno Cosign Image Verification** | CI signing alone does not stop untrusted images at runtime without admission controls | Kyverno verifyImages Audit and Enforce policy model using the same Cosign trust identity from CI | [README](./kubernetes-reference-architectures/kyverno-image-verification/README.md) · [Runbook](./kubernetes-reference-architectures/kyverno-image-verification/kyverno-runbook.md) |
| **Pattern 6: Karpenter vs Cluster Autoscaler** | Platform teams need a clear decision framework for node autoscaling approaches | Theory-focused architecture comparison, trade-offs, scenario guidance, and manual command set for future validation | [README](./kubernetes-reference-architectures/karpenter-vs-cluster-autoscaler/README.md) · Runbook not available (theory-only) |

For full Kubernetes navigation and prerequisites, use the dedicated index: [Kubernetes Reference Architectures](./kubernetes-reference-architectures/README.md).

## Supply Chain Security Tools

The CI/CD reference architecture includes a tools section for engineers who want to compare security controls before choosing an implementation.

| Tool area | Reference |
|---|---|
| Full supply chain tools index | [Supply Chain Security Tools Reference](./supply-chain-security-ref/tools/README.md) |
| SAST and code quality | [SonarQube SAST Reference](./supply-chain-security-ref/tools/sonarqube-sast.md) |
| SBOM intelligence and publishing | [Dependency-Track Reference](./supply-chain-security-ref/tools/dependency-track.md) |
| Image signing and attestations | [Cosign Signing Reference](./supply-chain-security-ref/tools/cosign-signing.md) |

## Who This Is For

This repository is intended for:

- Cloud and platform architects
- DevOps and infrastructure engineers
- Kubernetes and GitOps engineers
- Engineers moving towards production-level infrastructure design

## Topics Being Added

The repository will continue to expand with:

- Kubernetes and Amazon EKS architecture
- Kubernetes reference architecture patterns
- CI/CD and deployment strategies
- Observability and monitoring
- Infrastructure security
- Kafka and event-driven systems
- Platform automation
- AI infrastructure on Kubernetes
- GPU workload scheduling, model hosting, and MLOps

## About Me

I specialise in AWS cloud architecture, Kubernetes, Amazon EKS, Terraform, GitOps, CI/CD, cloud networking, scalable infrastructure, and distributed systems.

I am currently extending my platform architecture expertise into AI infrastructure, including GPU-enabled Kubernetes platforms, AI workload hosting, autoscaling, observability, and MLOps.

> These implementations are provided as reference architectures. Review security, cost, availability, compliance, and organisational requirements before adapting them for production.
