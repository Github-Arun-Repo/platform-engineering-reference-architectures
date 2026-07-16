# CI/CD and Continuous Deployment: Reference Architectures

Comprehensive, production-validated patterns for automating code builds, tests, image creation, and deployments to Kubernetes.

[![Security Reports](https://img.shields.io/badge/Security%20Reports-View%20Dashboard-blue?logo=github)](https://htmlpreview.github.io/?https://github.com/Github-Arun-Repo/platform-engineering-reference-architectures/blob/main/docs/security-reports/index.html)

> **📊 Live Security Reports:** Every Jenkins pipeline run scans the container image with Trivy and publishes results to the [Security Reports Dashboard](https://htmlpreview.github.io/?https://github.com/Github-Arun-Repo/platform-engineering-reference-architectures/blob/main/docs/security-reports/index.html). CVE vulnerabilities, SBOM, and future scan types are accessible there without needing Jenkins access.

---

## What Is This?

This section demonstrates battle-tested **continuous integration and continuous delivery** patterns across multiple tools and platforms. Unlike tutorials focused on individual commands, this documentation teaches the **architectural reasoning**: why each tool exists, when to use it, what trade-offs it makes, and how to operate it in production.

Each pattern is implemented with a **reference application** (a simple Spring Boot TODO API) and a **live demo runbook**, so you can understand concepts by hands-on experience.

---

## Quick Start — Choose Your Path

**I want to understand CI/CD fundamentals first:**
→ [Read CI/CD Principles below](#cicd-principles)

**I want to see a working image build pipeline:**
→ [Phase 1: Image Build Pipeline](#phase-1-image-build-pipeline)

**I want the Jenkins implementation:**
→ [Phase 1 — Jenkins](./phase-1-image-build-jenkins/)

**I want the GitHub Actions implementation:**
→ [Phase 1 — GitHub Actions](./phase-1-image-build-github-actions/)

**I want to understand the sample application:**
→ [Sample Application](./sample-application/)

**I want to understand tools and when to use each:**
→ [Available CI Tools](#available-ci-tools)

---

## Prerequisites

This reference architecture assumes:

- **Kubernetes cluster** running and accessible (1.19+)
- **kubectl** configured with cluster access
- **Docker** installed locally for testing images
- **Git** repository (GitHub, GitLab, or self-hosted)
- Understanding of Kubernetes Deployments and Services
- Familiarity with containerization concepts

Recommended:
- Kubernetes cluster on AWS EKS, Google GKE, or Azure AKS
- Argo CD installed (for Phase 2 integration)
- A private container registry (Docker Hub, ECR, GCR, or Harbor)

---

## CI/CD Principles

Before exploring patterns, understand the core **continuous integration and delivery** philosophy:

**Continuous Integration (CI)**
Code changes are integrated into a shared main branch multiple times per day. Each integration is automatically tested and built.

**Build Automation**
Every commit triggers an automated build. Code is compiled, tests execute, quality checks run — all without human intervention.

**Fast Feedback**
Developers learn of broken builds within minutes, not hours. Fast feedback enables rapid fixes.

**Automated Testing**
Unit tests, integration tests, and smoke tests run automatically. Coverage gates prevent untested code.

**Artifact Generation**
Each successful build produces an artifact: a JAR file, a Docker image, or a binary. Artifacts are versioned and stored.

**Continuous Delivery (CD)**
Artifacts are continuously released to production (or pre-production) environments, ready for deployment at any time.

**Container-Centric**
Modern CI/CD centers on container images: build once, test once, deploy to any environment.

**Supply Chain Security**
Every artifact is scanned for vulnerabilities, signed, and audited. The build pipeline is part of your security posture.

---

## Understanding CI/CD Fundamentals

### The Build Pipeline

A **CI pipeline** progresses through distinct stages:

```
Code Commit → Checkout → Build → Test → Quality Check → Artifact
   (Git)      (VCS)     (Maven)  (JUnit) (SonarQube)  (Image)
```

Each stage must succeed before the next runs. **Fail fast** on the left (code issues caught first).

### Artifact Versioning

Every build produces a versioned artifact:

| Scheme | Format | Example | Use Case |
|--------|--------|---------|----------|
| **Build Number** | Build #42 | `todo-app:42` | Every build during development |
| **Git SHA** | Commit hash | `todo-app:abc1234` | Trace to exact commit |
| **Semantic Version** | X.Y.Z | `todo-app:1.2.3` | Production releases |
| **Latest** | Tag | `todo-app:latest` | Convenience tag |

Use **build number + Git SHA** during development. Use **semantic version** for production releases.

### Sync Policy: Manual vs Automated

| Policy | Behavior | Trade-off |
|--------|----------|-----------|
| **Manual** | Image built, needs approval to deploy | Safe, slower (hours to deploy) |
| **Automated** | Image built, automatically deployed | Fast (minutes), more risk |
| **Gated** | Auto-deploy to dev, manual to prod | Balanced: fast feedback + safety |

**Best practice:** Automated to dev/staging, manual to production.

### Container Image Layers

Docker images are built in **layers**. Each `RUN`, `COPY`, or `ADD` creates a layer:

```dockerfile
FROM base              # Layer 1: base OS
RUN apt-get update    # Layer 2: updates
COPY app.jar          # Layer 3: app binary
RUN chmod +x app.jar  # Layer 4: permissions
```

**Build cache** reuses unchanged layers (saves 20-30 seconds per build). **Multi-stage builds** discard build tools, reducing final image size.

### Supply Chain Security

The CI/CD pipeline is part of your **security posture**:

1. **Code scanning** — detect bugs before build
2. **Dependency scanning** — find known CVEs in libraries
3. **Image scanning** — find vulnerabilities in OS and app layers
4. **Image signing** — verify image wasn't tampered with
5. **SBOM (Software Bill of Materials)** — track all components
6. **Audit trail** — every build logged and traceable

**2026 best practice:** Every image scanned, SBOM generated, artifacts signed.

---

## Available CI Tools

Different tools, different trade-offs. Choose based on your infrastructure and team.

### Jenkins

**What it is:** On-premises or cloud-hosted orchestrator for arbitrary jobs. The industry standard.

| Aspect | Detail |
|--------|--------|
| **Infrastructure** | Self-hosted or cloud SaaS |
| **Setup Time** | Hours to days (VMs, networking, Jenkins config) |
| **Cost** | Free software, but hosting/maintenance costs |
| **Customization** | Highly customizable via plugins |
| **Declarative** | Jenkinsfile in Git (2.0+) |
| **Scaling** | Agents across multiple machines |
| **Use Case** | Large teams, complex workflows, on-prem requirement |

**Phase 1 Implementation:** [Jenkins Image Build Pipeline](./phase-1-image-build-jenkins/)

### GitHub Actions

**What it is:** Cloud-native, serverless CI/CD integrated into GitHub.

| Aspect | Detail |
|--------|--------|
| **Infrastructure** | Zero — fully managed by GitHub |
| **Setup Time** | Minutes (YAML file, push to repo) |
| **Cost** | Free (2000 min/month), $0.008/minute after |
| **Customization** | Actions marketplace, community-driven |
| **Declarative** | Workflow YAML files |
| **Scaling** | Automatic via GitHub infrastructure |
| **Use Case** | GitHub-hosted projects, small-medium teams, rapid setup |

**Phase 1 Implementation:** [GitHub Actions Image Build Pipeline](./phase-1-image-build-github-actions/)

### GitLab CI

**What it is:** Built-in CI/CD within GitLab. Excellent for GitLab users.

| Aspect | Detail |
|--------|--------|
| **Infrastructure** | SaaS or self-hosted GitLab |
| **Setup Time** | Minutes (`.gitlab-ci.yml`) |
| **Cost** | Free (SaaS), included in self-hosted |
| **Customization** | Native YAML, extensive |
| **Declarative** | `.gitlab-ci.yml` in repo |
| **Scaling** | Runners on any machine |
| **Use Case** | GitLab users, comprehensive DevOps platform |

*Future phase: GitLab CI implementation*

### Tekton

**What it is:** Kubernetes-native CI/CD. Runs jobs as Kubernetes resources.

| Aspect | Detail |
|--------|--------|
| **Infrastructure** | Kubernetes cluster required |
| **Setup Time** | Hours (CRD learning curve) |
| **Cost** | Free, uses cluster resources |
| **Customization** | YAML-based, cloud-native |
| **Declarative** | Tekton Tasks, Pipelines (YAML) |
| **Scaling** | Automatic via Kubernetes |
| **Use Case** | Kubernetes-first teams, cloud-native workflows |

*Future phase: Tekton implementation*

---

## Decision Framework: Which CI Tool?

**Do you use GitHub for source control?**
- Yes → **GitHub Actions** (zero setup, free)
- No → Continue

**Do you need on-premises/air-gapped capability?**
- Yes → **Jenkins**
- No → Continue

**Do you use GitLab?**
- Yes → **GitLab CI**
- No → Continue

**Do you prefer Kubernetes-native?**
- Yes → **Tekton**
- No → **Jenkins** (most flexible)

---

## Phase 1: Image Build Pipeline

**Focus:** Build, test, scan, and push container images.

### What's Included

Each implementation (Jenkins and GitHub Actions) includes:

1. **Complete build pipeline** → Checkout → Build → Test → Quality → Image → Scan → Push
2. **Best practices** → Multi-stage Dockerfile, non-root user, health checks, CVE scanning
3. **Artifact versioning** → Build number, Git SHA, semantic version, `latest`
4. **Supply chain security** → Trivy scanning, SBOM generation, vulnerability reporting
5. **Production ready** → Timeouts, error handling, notifications, cleanup

### Available Implementations

- [Jenkins Implementation](./phase-1-image-build-jenkins/) — Enterprise-standard, on-premises capable
- [GitHub Actions Implementation](./phase-1-image-build-github-actions/) — Serverless, GitHub-native

### Sample Application

Both implementations use the same **reference application**:

- **Language:** Java 21 / Spring Boot 3.2
- **Framework:** Spring Web + Data JPA
- **Database:** H2 (in-memory, no external DB needed)
- **API:** Simple CRUD for TODO items
- **Purpose:** Demonstrate containerization and CI/CD; the app itself is intentionally simple

[View Sample Application](./sample-application/)

---

## Real-World Considerations

### Artifact Management

Store images in a **private registry**:
- **Docker Hub** — public or private repositories
- **AWS ECR** — tight AWS integration
- **Google GCR** — tight GCP integration
- **Harbor** — self-hosted, enterprise-grade
- **Artifactory** — universal artifact repository

**Policy:** Store images in private registry, pull only authenticated. Use image pull secrets in Kubernetes.

### Build Caching

Cache dependency downloads to save build time:
- **Maven:** `.m2` directory
- **Docker:** Layer cache
- **GitHub Actions:** Built-in `actions/cache`
- **Jenkins:** Workspace persistence or volume mounts

**2026 practice:** Expect 30-50% faster builds with caching enabled.

### Parallelization

Run independent steps in parallel:
- Unit tests, integration tests, code quality checks in parallel
- Multi-stage Docker builds (compile in one stage, run in another)
- Matrix jobs (test against multiple Java versions simultaneously)

### Notifications

Alert teams on build status:
- **Slack** — integration in both Jenkins and GitHub Actions
- **Email** — for build failures
- **PagerDuty** — for critical failures
- **VCS** — commit status checks on pull requests

### Secrets Management

Never hardcode secrets. Use platform-provided secret stores:
- **GitHub Secrets** — repository secrets, organization secrets
- **Jenkins Credentials** — encrypted credential store
- **Vault** — external secret management
- **AWS Secrets Manager** — for AWS-based deployments

### Audit and Compliance

Log and audit all builds:
- Every build is logged and traceable
- Link artifacts to commits (image tag includes commit SHA)
- Track who triggered builds
- Store artifacts with retention policy

---

## Learning Path

1. **Understand the principles** — Read sections above
2. **Choose a tool** — Jenkins or GitHub Actions (or both)
3. **Review the implementation** — Read phase-1 README
4. **Explore the code** — Examine Jenkinsfile or workflow YAML
5. **Inspect the Dockerfile** — Understand multi-stage build
6. **Run locally** — Build the sample app locally with Docker
7. **Configure your tool** — Set up Jenkins or GitHub Actions in your environment
8. **Execute the pipeline** — Trigger a build and watch stages execute
9. **Inspect the image** — Run `docker inspect` on the built image
10. **Understand the output** — Review build logs, scan reports, artifact registry

---

## Production Readiness Checklist

Before deploying pipelines to your team:

- [ ] Container registry is private and access-controlled
- [ ] Image scanning (Trivy or similar) is mandatory, not optional
- [ ] Build logs are retained (audit trail)
- [ ] Failed builds alert the team (Slack, email)
- [ ] Artifacts are versioned with commit SHA for traceability
- [ ] Secrets are stored in vault, not in code or logs
- [ ] Registry credentials are rotated regularly
- [ ] Image pull secrets are created in Kubernetes
- [ ] Docker daemon limits are set (memory, CPU)
- [ ] Build timeouts prevent runaway jobs
- [ ] SBOM is generated for every image
- [ ] Backup strategy exists for artifact registry

---

## Key Takeaways

1. **CI/CD automates the path from code to running container** — commit → build → test → scan → push → deploy

2. **Container images are the unit of deployment** — not source code, not configuration

3. **Fail fast** — expensive operations (pushing to registry) come last; cheap operations (running tests) come first

4. **Supply chain security is non-negotiable** — scan for CVEs, generate SBOM, verify artifacts

5. **Different tools for different contexts** — GitHub Actions for GitHub users, Jenkins for complex workflows, Tekton for Kubernetes-first

6. **Build once, deploy many times** — the same image artifact runs in dev, staging, and production

7. **Traceability matters** — every artifact linked to a commit, every deployment logged

---

## Related Documentation

- [ArgoCD and GitOps Reference Architecture](../argocd-reference-architectures/) — Deployment patterns
- [Terraform Infrastructure](../terraform/) — Infrastructure as code
- [Main Repository README](../README.md)

---

## Phases Overview

| Phase | Focus | Status |
|-------|-------|--------|
| **Phase 1** | Image Build & Push | ✅ Complete (Jenkins + GitHub Actions) |
| **Phase 2** | ArgoCD Integration | 🔄 Planned |
| **Phase 3** | Multi-Environment | 🔄 Planned |
| **Phase 4** | Blue-Green Deployments | 🔄 Planned |
| **Phase 5** | Canary & Traffic Management | 🔄 Planned |

More phases to be added as the reference architecture grows.

---

## Contributing & Questions

These patterns are designed to be **reference implementations**, not copy-paste templates. Adapt them to your organization's needs, constraints, and preferences.

Questions? Explore the phase-specific READMEs for detailed guidance.
