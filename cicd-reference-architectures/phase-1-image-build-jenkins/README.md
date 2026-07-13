# Phase 1: Image Build Pipeline — Jenkins Implementation

Build, test, scan, and push Docker images using Jenkins — a traditional, battle-tested CI/CD orchestrator.

---

## Quick Start

**Prerequisites:**
- Jenkins instance running (2.387+)
- Docker installed on Jenkins agents
- Git access to repository
- Docker registry credentials configured in Jenkins

**Setup:**

1. Create a new Pipeline job in Jenkins
2. Point to this repository: `https://github.com/Github-Arun-Repo/platform-engineering-reference-architectures.git`
3. Configure pipeline script path: `cicd-reference-architectures/phase-1-image-build-jenkins/Jenkinsfile`
4. Add credentials for Docker registry (`docker-credentials`)
5. Configure SonarQube server (optional, labeled `SonarQube` in Jenkins)
6. Trigger build manually or configure webhooks

**Expected output:**
```
Docker image built and pushed: arunrepo/todo-app:42
```

---

## What This Pipeline Does

The Jenkins pipeline implements a **complete container image build workflow** following 2026 best practices.

### Stage 1: Checkout
- Clones the repository from GitHub
- Targets the `main` branch

### Stage 2: Build & Test
- Runs Maven to compile and execute unit tests
- Catches test failures early, fails fast

### Stage 3: Code Quality Analysis
- Integrates with SonarQube for static code analysis
- Detects code smells, bugs, vulnerabilities before containerization

### Stage 4: Build Application
- Packages the Spring Boot application as a JAR
- Creates an executable artifact ready for Docker

### Stage 5: Build Docker Image
- Executes multi-stage Dockerfile (in `sample-application/`)
- Tags image with build number and `latest`
- Optimizes layers for faster builds and smaller final image

### Stage 6: Scan Docker Image
- Uses **Trivy** (2026 best practice) to scan image for CVE vulnerabilities
- Fails on CRITICAL or HIGH severity issues
- Prevents vulnerable images from reaching the registry

### Stage 7: Push to Registry
- Authenticates to Docker registry (Docker Hub, ECR, GCR, etc.)
- Pushes image with build-specific tag and `latest` tag
- Securely handles registry credentials

### Stage 8: Update Deployment Manifests
- *Placeholder for next phase*
- Will integrate with ArgoCD to trigger deployments
- Updates Kubernetes manifests with new image tag

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                          Jenkins Pipeline                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  GitHub Repo → [Checkout] → [Build & Test] → [Code Quality]    │
│                                ↓                                 │
│                        [Build Application] → JAR artifact        │
│                                ↓                                 │
│         [Build Docker Image] → Docker image (multi-stage)        │
│                ↓                                                  │
│  [Scan Image] ← Trivy (vulnerability scan)                      │
│         ↓                                                         │
│  [Push to Registry] → Docker Hub / ECR / GCR                    │
│         ↓                                                         │
│  [Update Deployment] → ArgoCD repo (triggers reconciliation)    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Key Design Decisions

**Why Jenkins?**
- Declarative Pipeline syntax (Jenkinsfile) version-controlled in Git
- Extensive plugin ecosystem (Docker, SonarQube, Slack, etc.)
- Enterprise-standard; high maturity and community support
- Familiar to organizations with legacy CI/CD systems
- Runs on-premises or cloud-hosted

**Why this stage order?**
1. **Checkout → Test**: Fail fast on code issues
2. **Test → Build**: Only build working code
3. **Build → Quality**: Analyze before containerization
4. **Container → Scan**: Detect supply-chain vulnerabilities
5. **Scan → Push**: Never push vulnerable images
6. **Push → Reconciliation**: Trigger GitOps immediately

**Security practices:**
- Non-root user in Docker image
- Multi-stage builds minimize attack surface
- Health checks for container orchestration
- CVE scanning with Trivy (2026 standard)
- Credentials injected at runtime, not baked into image

**Best practices:**
- Build logs retained for 15 builds (configurable)
- 30-minute timeout prevents hung builds
- Timestamps on all logs for troubleshooting
- Clean workspace post-build
- Graceful notification on success/failure

---

## Configuration

### Jenkins Credentials

Add Docker registry credentials:
- **Credentials ID:** `docker-credentials`
- **Username:** Your Docker Hub username
- **Password:** Docker Hub personal access token (not password)

Add SonarQube server:
- **Jenkins URL:** Manage Jenkins → Configure System → SonarQube Servers
- **Server URL:** `http://sonarqube:9000` (or your SonarQube instance)
- **Authentication token:** SonarQube token

### Dockerfile Registry

Edit the `REGISTRY` environment variable in the Jenkinsfile:

```groovy
REGISTRY = 'gcr.io'  // For Google Cloud
REGISTRY = '123456789.dkr.ecr.us-east-1.amazonaws.com'  // For AWS ECR
```

Update the image name accordingly:
```groovy
IMAGE_NAME = 'your-org/todo-app'
```

---

## Operational Patterns

### Manual Trigger

Click "Build Now" in Jenkins UI.

### Scheduled Trigger (Nightly Builds)

Add to Jenkins job:
```groovy
triggers {
    cron('H 2 * * *')  // 2 AM daily
}
```

### GitHub Webhook (Push-to-Build)

1. Configure webhook in GitHub: Settings → Webhooks
2. Payload URL: `https://your-jenkins.com/github-webhook/`
3. Trigger on push

Jenkins will automatically start a build on every push to `main`.

### Artifact Retention

The pipeline keeps the last 15 builds' artifacts. To extend or reduce:

```groovy
buildDiscarder(logRotator(numToKeepStr: '30'))  // Keep 30 builds
```

---

## Troubleshooting

**Build fails at "Build Docker Image"**
- Ensure Docker daemon is running on the Jenkins agent
- Check Dockerfile syntax: `docker build --no-cache .`
- Verify Java 21 is available in the builder stage

**Image scan fails at Trivy stage**
- Trivy not installed: Install with `apt-get install trivy` or skip stage
- Adjust severity levels if needed: `--severity LOW,MEDIUM,HIGH,CRITICAL`

**Push to registry fails**
- Verify Docker credentials are configured correctly
- Check network connectivity to registry
- Ensure image name matches registry namespace

**SonarQube analysis fails**
- Verify SonarQube server is reachable
- Check authentication token expiration
- Review `sonar-project.properties` in the sample app

---

## Next Steps

1. **Phase 2:** Update Argo CD git repo on successful push (GitOps reconciliation)
2. **Phase 3:** Multi-environment promotion (dev → staging → prod)
3. **Phase 4:** Blue-green deployments with traffic management
4. **Phase 5:** Rollback and incident response patterns

---

## Related Documentation

- [Main CI/CD Reference Architecture](../README.md)
- [GitHub Actions Implementation](../phase-1-image-build-github-actions/)
- [Sample Application](../sample-application/)
- [Jenkins Best Practices](https://www.jenkins.io/doc/book/using-jenkins-safely/)
