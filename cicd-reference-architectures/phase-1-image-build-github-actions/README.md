# Phase 1: Image Build Pipeline — GitHub Actions Implementation

Build, test, scan, and push Docker images using GitHub Actions — a native, fully-managed CI/CD platform integrated into your repository.

---

## Quick Start

**Prerequisites:**
- Repository hosted on GitHub (github.com)
- Container registry (GitHub Container Registry, Docker Hub, ECR, or GCR)
- Kubernetes cluster available for deployment (prerequisite from project scope)

**Setup:**

1. The workflow is automatically available at `.github/workflows/build-docker-image.yml`
2. Push code to `main` branch to trigger builds
3. Monitor execution in **Actions** tab on GitHub
4. Images are pushed to **GitHub Container Registry (GHCR)** by default
5. Vulnerability reports appear in **Security → Code scanning alerts**

**Expected output:**
```
✅ Build Summary
- Registry: ghcr.io
- Image: ghcr.io/github-arun-repo/platform-engineering-reference-architectures/todo-app
- Tag: 42 (build number)
- Branch: main
```

---

## What This Pipeline Does

The GitHub Actions workflow implements a **complete container image build and push workflow** following 2026 best practices, with native GitHub integration.

It now uses a **shift-left security model**: an initial `security_precheck` job must pass before the `build` job starts.

### Event Triggers

Builds are triggered on:
- **Push to `main` branch** — full image build and push
- **Pull requests** — build only, no push (safe testing)
- **Path-based filtering** — only rebuild when relevant files change:
  - `cicd-reference-architectures/sample-application/**`
  - `.github/workflows/build-docker-image.yml`

### Stage 0: Security Precheck (Fail Fast)
- Runs before any build or image work
- Scans repository history and files with **Gitleaks**
- Runs OWASP dependency vulnerability check on Maven dependencies
- Fails pipeline immediately when secrets are found or dependency CVEs meet threshold (CVSS >= 7)

### Stage 1: Checkout
- Clones repository with full commit history
- Enables version detection from Git tags

### Stage 2: Setup Docker Buildx
- Enables advanced Docker builder with:
  - **Multi-stage build** support
  - **Build cache** export to GitHub Actions cache
  - **SBOM (Software Bill of Materials)** generation
  - **BuildKit** features for optimization

### Stage 3: Authentication
- Authenticates to GitHub Container Registry using GitHub token
- No additional secrets needed for GHCR
- Can be switched to Docker Hub, ECR, GCR with secrets

### Stage 4: JDK Setup
- Sets up Java 21 with Temurin distribution
- Caches Maven dependencies for faster builds

### Stage 5: Build & Test
- Runs `mvn clean test`
- Fails fast if tests fail
- No image is built if tests don't pass

### Stage 6: Code Quality (Optional)
- SonarQube integration available but disabled by default
- Enable by:
  1. Setting to `if: true`
  2. Adding `SONAR_TOKEN` secret to repository
  3. Configuring SonarCloud organization

### Stage 7: Package Application
- Creates JAR artifact
- Ready for Docker image

### Stage 8: Metadata Extraction
- Generates intelligent image tags:
  - Branch name (e.g., `main`)
  - Semantic version from Git tags
  - Commit SHA (for traceability)
  - Build number
  - `latest` tag for default branch

### Stage 9: Build and Push Image
- **Multi-stage Dockerfile** builds minimal image
- **Layer caching** via GitHub Actions cache (faster rebuilds)
- **Push only on main** branch push (not on pull requests)
- **SBOM generated** for supply chain security

### Stage 10: Trivy Vulnerability Scan
- Scans image for CVE vulnerabilities
- **SARIF report** uploaded to GitHub Security tab
- Results visible as **code scanning alerts**
- Serves as post-build image assurance (the fail-fast secret/dependency gate already runs first)

### Stage 11: Build Summary
- Posts summary to **GitHub Actions** run page
- Shows image registry, tag, branch, commit SHA
- Indicates whether image was pushed

---

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                    GitHub Actions Workflow                       │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  GitHub Push → [Security Precheck: Gitleaks + Dependency CVE]   │
│           ↓                                                       │
│        [Checkout] → [Setup Docker] → [Auth Registry]             │
│           ↓                                                       │
│  [Setup JDK] → [Build & Test] ✗ Fail                            │
│       ↓                                                           │
│  [Package App] → JAR artifact                                    │
│       ↓                                                           │
│  [Extract Metadata] → Multiple image tags                        │
│       ↓                                                           │
│  [Build & Push] → GitHub Container Registry                     │
│       ↓                                                           │
│  [Trivy Scan] → Vulnerability report                            │
│       ↓                                                           │
│  [Summary] → Action run summary + Security alerts               │
│       ↓                                                           │
│  [Deploy] → Trigger ArgoCD sync (future phase)                  │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Key Design Decisions

**Why GitHub Actions?**
- **Fully managed** — no infrastructure to maintain
- **Native GitHub integration** — built-in secrets, RBAC, auditing
- **Free tier** — 2,000 free minutes/month for public repos
- **Fast** — faster builds due to GitHub infrastructure
- **Modern** — cloud-native, serverless approach
- **Great DX** — YAML syntax, extensive marketplace

**Why this workflow design?**
1. **Path-based triggers** — only rebuild when code changes
2. **Maven caching** — dependencies cached across runs
3. **Docker cache export** — layer caching for faster rebuilds (2x speedup)
4. **Multi-tag strategy** — trace images back to branch, commit, version
5. **Pull request safety** — build without push (no credentials exposed)
6. **Integrated scanning** — Trivy results in GitHub Security tab
7. **Outputs for next job** — `image-tag` and `image-digest` passed to deploy job

**Security practices:**
- Non-root user in Docker image
- Health checks for container orchestration
- CVE scanning with Trivy (2026 standard)
- SARIF report upload for compliance
- Secrets never logged or exposed

**Performance optimizations:**
- Maven dependency cache (saves ~30 seconds)
- Docker layer cache via GHA cache (saves ~20 seconds per build)
- SBOM generation (supply chain security)
- Shallow clone for pull requests

---

## Configuration

### Switch to Different Registry

**Docker Hub:**
```yaml
env:
  REGISTRY: docker.io
  IMAGE_NAME: ${{ github.repository_owner }}/todo-app
```

Add secrets to repository:
- `DOCKER_USERNAME` — Docker Hub username
- `DOCKER_PASSWORD` — Docker Hub PAT (not password)

Update login step:
```yaml
- name: Log in to Docker Hub
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKER_USERNAME }}
    password: ${{ secrets.DOCKER_PASSWORD }}
```

**AWS ECR:**
```yaml
env:
  REGISTRY: ${{ secrets.AWS_ACCOUNT_ID }}.dkr.ecr.us-east-1.amazonaws.com
  IMAGE_NAME: todo-app
```

Add AWS credentials as secrets and use `aws-actions/configure-aws-credentials`.

**Google Cloud (GCR):**
```yaml
env:
  REGISTRY: gcr.io
  IMAGE_NAME: ${{ secrets.GCP_PROJECT_ID }}/todo-app
```

### Enable SonarQube Scanning

1. Uncomment the SonarQube step (set `if: true`)
2. Add `SONAR_TOKEN` secret to repository
3. Set project key in `sonar-project.properties`

### Modify Tag Strategy

Current tags:
- Branch name → `main`
- Semver from tags → `v1.2.3` → `1.2.3` / `1.2`
- Commit SHA → `main-abc1234`
- Build number → `42`
- Latest → `latest` (only main branch)

Customize in `docker/metadata-action`:
```yaml
tags: |
  type=semver,pattern={{version}}
  type=raw,value=stable,enable={{is_default_branch}}
```

---

## Operational Patterns

### Manual Trigger (Workflow Dispatch)

Add to workflow to enable manual trigger button in Actions tab:

```yaml
on:
  workflow_dispatch:
    inputs:
      push-to-registry:
        description: 'Push image to registry'
        required: true
        default: 'true'
```

### Scheduled Nightly Builds

Add cron trigger:
```yaml
on:
  schedule:
    - cron: '0 2 * * *'  # 2 AM UTC daily
```

### Release Builds (Semantic Versioning)

Trigger on Git tags:
```yaml
on:
  push:
    tags:
      - 'v*.*.*'
```

### Matrix Builds (Multi-Environment)

Build for multiple Java versions:
```yaml
strategy:
  matrix:
    java-version: [17, 21, 22]
```

---

## Troubleshooting

**Build fails at "Build and Test Application"**
- Check Maven output for test failures
- Verify Java 21 is compatible with application
- Review logs in GitHub Actions tab

**Image fails to push (permission error)**
- Verify GitHub token has `packages: write` permission
- Check `GITHUB_TOKEN` is not expired
- Ensure PAT has `write:packages` scope (if using custom token)

**Trivy scan reports false positives**
- Create `trivy.yaml` in `sample-application/` to ignore known CVEs
- Example:
  ```yaml
  severity:
    - HIGH
    - CRITICAL
  ```

**Slow builds (slow Maven download)**
- Maven cache might be stale
- Clear cache in Actions → Caches
- Docker layer cache may not be efficient

**Images not pushed on pull requests**
- Expected behavior! PR builds are test-only
- Only merge to `main` pushes to registry

---

## Comparison: GitHub Actions vs Jenkins

| Aspect | GitHub Actions | Jenkins |
|--------|----------------|---------|
| **Setup** | Zero — native to GitHub | Infrastructure required |
| **Cost** | Free (2000 min/month) | Self-hosted or hosted SaaS |
| **Maintenance** | GitHub manages | You manage |
| **Integration** | Native GitHub | Plugins needed |
| **Speed** | Very fast (GitHub infra) | Depends on agents |
| **Customization** | Limited | Highly customizable |
| **On-prem** | No | Yes |
| **Enterprise** | Available (GHES) | Yes |

**Use GitHub Actions if:**
- Repository is on GitHub
- Want zero infrastructure
- Team comfortable with YAML
- Need tight GitHub integration

**Use Jenkins if:**
- On-premises requirement
- Complex customization needed
- Existing Jenkins ecosystem
- Hybrid cloud setup

---

## Next Steps

1. **Phase 2:** Update Argo CD git repo on successful push
2. **Phase 3:** Multi-environment promotion (dev → staging → prod)
3. **Phase 4:** Blue-green deployments
4. **Phase 5:** Canary releases with traffic management

---

## Related Documentation

- [Main CI/CD Reference Architecture](../README.md)
- [Jenkins Implementation](../phase-1-image-build-jenkins/)
- [Sample Application](../sample-application/)
- [GitHub Actions Documentation](https://docs.github.com/actions)
- [Docker Build Action](https://github.com/docker/build-push-action)
