# Jenkins Image Build Pipeline — Demo Runbook

**Repo:** `https://github.com/Github-Arun-Repo/platform-engineering-reference-architectures.git`
**Base folder:** `cicd-reference-architectures/` (run commands from this folder)
**Cluster:** Standalone K8s on EC2 · **Jenkins UI:** `http://<EC2-IP>:30080`
**Presenter:** Arun

---

## Repo Layout (all paths below match this)

```
cicd-reference-architectures/
├── sample-application/
│   ├── src/main/java/com/example/app/
│   │   ├── TodoApplication.java
│   │   ├── Todo.java
│   │   ├── TodoController.java
│   │   └── TodoRepository.java
│   ├── Dockerfile                    # multi-stage build
│   └── pom.xml                       # Spring Boot 3.2 / Java 21
└── phase-1-image-build-jenkins/
    ├── Jenkinsfile                   # 8-stage pipeline
    ├── README.md
    ├── installation-jenkins.md
    └── jenkins-demo-runbook.md       # ← this file
```

---

## Installation: Deploy Jenkins

Follow the complete installation guide: **[installation-jenkins.md](./installation-jenkins.md)**

That document covers:
- Helm installation on Kubernetes
- Plugin configuration
- Docker access for builds
- Docker registry credentials
- Pipeline job creation

After completing installation, return here and continue with the pre-flight section.

---

## 0. PRE-FLIGHT

Run these before starting the demo. Jenkins must already be installed.

```bash
# local clone current?
cd ~/platform-engineering-reference-architectures && git pull

# Jenkins pod running?
kubectl get pods -n jenkins

# Jenkins accessible?
curl -s -o /dev/null -w "%{http_code}" http://<EC2-IP>:30080/login
# Expect: 200

# Docker working on the node?
docker version
docker info | grep "Server Version"

# Credentials configured?
# Check Jenkins UI → Manage Jenkins → Credentials → docker-credentials exists
```

**Timing plan:**
- Installation (one-time): ≈ 15 min
- Pre-flight: ≈ 3 min
- Part 1 — Pipeline stages walkthrough: ≈ 20 min
- Part 2 — Failure and recovery: ≈ 15 min
- Part 3 — Operational patterns: ≈ 10 min
- Q&A: ≈ 5 min
- **Total demo time (without installation): ≈ 53 min**

---
---

# PART 1 — Pipeline Execution (≈20 min)

> Goal: run the full pipeline end-to-end and understand what each stage does.

## 1.1 — Inspect the Jenkinsfile Before Running

**Say:** "Always read the pipeline before running it. The Jenkinsfile is code."

```bash
cat phase-1-image-build-jenkins/Jenkinsfile
```

Point out:
- `environment {}` block — centralized config, change once to affect everything
- `options {}` block — build retention, timeout, timestamps
- Stage order — tests before image build, scan before push
- `post {}` block — success, failure, and cleanup handlers

## 1.2 — Trigger the First Build

In the Jenkins UI:
1. Open `todo-app-image-build` job
2. Click **Build Now**
3. Watch the **Stage View** update in real time

While building, talk through each stage:

```
[Checkout] Running...
  → Cloning the repository, checking out main branch

[Build & Test] Running...
  → mvn clean test — compiles and runs unit tests
  → Fails here if any test fails (fail fast principle)

[Code Quality] Running...
  → SonarQube static analysis (or skip message if not configured)

[Build Application] Running...
  → mvn clean package -DskipTests
  → Produces: target/todo-app-0.0.1-SNAPSHOT.jar

[Build Docker Image] Running...
  → docker build (multi-stage)
  → Two tags: todo-app:1 and todo-app:latest

[Scan Docker Image] Running...
  → trivy image todo-app:1
  → Reports CVEs; pipeline continues (warn-only in demo mode)

[Push to Registry] Running...
  → docker login (credentials injected, never visible)
  → docker push todo-app:1
  → docker push todo-app:latest
  → docker logout

[Update Deployment Manifests] Running...
  → Placeholder for Phase 2 ArgoCD integration
```

```bash
# In the console output, look for:
# "Building Docker image: arunrepo/todo-app:1"
# "Successfully built ..."
# "Image built and pushed: arunrepo/todo-app:1"
```

👉 Green pipeline. Every stage passed. Build number 1 is now in the registry.

## 1.3 — Inspect the Resulting Image

```bash
# What image layers were built?
docker images | grep todo-app

# Inspect the image metadata
docker inspect arunrepo/todo-app:1 | jq '.[0].Config'

# Verify non-root user
docker run --rm arunrepo/todo-app:1 whoami
# Expect: appuser (not root)

# Check health check config
docker inspect arunrepo/todo-app:1 | jq '.[0].Config.Healthcheck'

# Check image size (multi-stage = smaller)
docker images arunrepo/todo-app:1 --format "Size: {{.Size}}"
```

👉 Image runs as `appuser`. Health check configured. Size ~180MB — compare to `eclipse-temurin:21-jdk-alpine` at ~330MB — build tools excluded.

## 1.4 — Run the Image Locally

```bash
docker run -d -p 8080:8080 --name todo-demo arunrepo/todo-app:1

# Wait ~5 seconds for startup
sleep 5
curl -s http://localhost:8080/api/todos/health
# Expect: "TODO App is healthy"

# Create a todo
curl -s -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title": "Learn Jenkins", "description": "Pipeline demo", "completed": false}' | jq .

# List todos
curl -s http://localhost:8080/api/todos | jq .

# Clean up
docker stop todo-demo && docker rm todo-demo
```

👉 Proves the image is functional — not just built and pushed.

## 1.5 — Inspect Build Artifacts and History

In the Jenkins UI:
- Open the build → **Console Output** (full log, every command, every line)
- Back to job → **Stage View** (visual pipeline with per-stage timing)
- **Build Artifacts** section — JAR file archived from the post block

```bash
# From CLI — Jenkins REST API
curl -s "http://<EC2-IP>:30080/job/todo-app-image-build/1/api/json" | jq '.result,.duration'
# Expect: "SUCCESS" and duration in milliseconds
```

---
---

# PART 2 — Failure and Recovery Scenarios (≈15 min)

> Goal: understand how the pipeline fails, why, and how to recover.

## 2.1 — Failing Test (Stage 2 Fail Fast)

**Say:** "The most common pipeline failure: a developer pushes broken code. Let's see what happens."

```bash
# Break a test — edit the controller to break compilation
cd ~/platform-engineering-reference-architectures
sed -i 's/return ResponseEntity.ok(todos);/return ResponseEntity.ok(null);/' \
  cicd-reference-architectures/sample-application/src/main/java/com/example/app/TodoController.java

git add -A && git commit -m "break: return null from controller (demo)" && git push
```

Trigger a build or wait for webhook. In the Stage View:

```
[Checkout]        ✅
[Build & Test]    ❌ FAILED
```

👉 Pipeline stops at stage 2. Stages 3–8 (Docker build, scan, push) never execute. Cost: 30–60 seconds wasted, not 5 minutes.

```bash
# In console output, look for:
# "BUILD FAILURE"
# "Tests run: X, Failures: Y"

# Recover immediately
git revert --no-edit HEAD && git push
```

Trigger another build — back to green.

**Key point:** "Fail fast protects your registry from broken images. A broken image in production is worse than a failed build."

## 2.2 — Build Succeeds, Deployment Breaks (Sync vs Health)

**Say:** "Build passes, image is in the registry — but does it actually run?"

```bash
# Inject a bad image reference into the Dockerfile ENTRYPOINT to simulate a runtime failure
# (This would result in a build that succeeds but the container crashes immediately)
# Instead demonstrate with a bad environment variable approach:

docker run --rm -e JAVA_TOOL_OPTIONS="-Xmx1m" arunrepo/todo-app:1
# Container starts but JVM crashes immediately with OOM

# Verify health check would catch it
docker run -d --name todo-broken \
  -e JAVA_TOOL_OPTIONS="-Xmx1m" \
  -p 8081:8080 arunrepo/todo-app:1

sleep 15
docker inspect todo-broken | jq '.[0].State.Health.Status'
# Expect: "unhealthy" — the health check fires and marks the container unhealthy

docker stop todo-broken && docker rm todo-broken
```

👉 **The pipeline produced a valid image** (build passed). But the container is unhealthy at runtime. This is the distinction: **pipeline success ≠ application success**. This is exactly the gap that Kubernetes liveness and readiness probes + ArgoCD health checks address (Phase 2).

## 2.3 — Trivy Finds a Vulnerability (Scan Failure)

**Say:** "Let's see what happens when the CVE scanner flags an issue."

```bash
# Check what Trivy finds in the current image
docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy image --severity HIGH,CRITICAL arunrepo/todo-app:1

# If vulnerabilities found, output shows:
# ┌──────────────┬────────────────┬──────────┬──────────────────┐
# │   Library    │ Vulnerability  │ Severity │ Fixed Version    │
# └──────────────┴────────────────┴──────────┴──────────────────┘
```

**Say:** "If the Jenkinsfile were configured to `exit 1` on scan failure:"

```groovy
// In Jenkinsfile, to make scan fail the build:
sh '''
    trivy image --severity HIGH,CRITICAL --exit-code 1 ${IMAGE_NAME}:${IMAGE_TAG}
'''
```

👉 "The pipeline would stop before push. A vulnerable image never reaches the registry. This is supply chain security in practice."

## 2.4 — Registry Push Failure (Wrong Credentials)

**Say:** "What if the Docker credentials are wrong or expired?"

In Jenkins UI:
1. Go to **Manage Jenkins → Credentials**
2. Temporarily corrupt the Docker password
3. Trigger a build

```
[Checkout]             ✅
[Build & Test]         ✅
[Code Quality]         ✅
[Build Application]    ✅
[Build Docker Image]   ✅
[Scan Docker Image]    ✅
[Push to Registry]     ❌ FAILED — "unauthorized: authentication required"
```

👉 All stages before push succeeded (image was built, tested, scanned). Only push failed. Restore correct credentials, re-trigger — push succeeds without rebuilding the image.

**Key point:** "The pipeline is idempotent in failures. Fix the credential, retry — no need to rebuild from scratch."

---
---

# PART 3 — Operational Patterns (≈10 min)

## 3.1 — Triggering a Second Build (Increment)

**Say:** "Every commit produces a new versioned artifact."

```bash
# Change the application version in pom.xml
sed -i 's/version>0.0.1-SNAPSHOT/version>0.0.2-SNAPSHOT/' \
  cicd-reference-architectures/sample-application/pom.xml

git add -A && git commit -m "bump version to 0.0.2-SNAPSHOT" && git push
```

After build completes:

```bash
# Two versions now in Docker
docker images | grep todo-app
# todo-app:1   (old build)
# todo-app:2   (new build)
# todo-app:latest   (points to build 2)
```

👉 "Build 2 is a new immutable artifact. Build 1 still exists. You can always roll back to build 1 by deploying that image."

## 3.2 — Build History and Traceability

```bash
# Jenkins REST API — list recent builds
curl -s "http://<EC2-IP>:30080/job/todo-app-image-build/api/json?tree=builds[number,result,timestamp]" \
  | jq '.builds[] | {build: .number, result: .result}'

# Correlate a build number to a Git commit
curl -s "http://<EC2-IP>:30080/job/todo-app-image-build/2/api/json" \
  | jq '.actions[] | select(._class | contains("RevisionParameterAction")) | .parameters'
```

👉 "Build number → Jenkins URL → Git commit. Every deployed image is traceable to the exact commit that produced it."

## 3.3 — Webhook-Triggered Build (Automatic CI)

In GitHub:
1. Go to Repository → Settings → Webhooks
2. Add webhook: `http://<EC2-IP>:30080/github-webhook/`
3. Content type: `application/json`
4. Events: Push

```bash
# Test it — make a small change
echo "# demo webhook" >> cicd-reference-architectures/sample-application/README.md
git add -A && git commit -m "test webhook trigger" && git push
```

👉 In the Jenkins UI, build starts within seconds of the push — no polling, no manual click.

**Say:** "This is the CI in CI/CD. Every push to `main` automatically tests, builds, scans, and publishes an image. The developer's work ends at `git push`."

---
---

## RESET (after demo / to rehearse again)

```bash
# Remove local demo images
docker rmi arunrepo/todo-app:1 arunrepo/todo-app:2 arunrepo/todo-app:latest 2>/dev/null || true

# Revert any pom.xml or code changes made during demo
cd ~/platform-engineering-reference-architectures
git checkout cicd-reference-architectures/sample-application/pom.xml
git checkout cicd-reference-architectures/sample-application/src/

# Optionally delete the Jenkins job build history via UI:
# Jenkins → todo-app-image-build → Delete Build History
```

---

## Command Cheat-Sheet (keep on screen during Q&A)

| Intent | Command |
|---|---|
| Check Jenkins pod | `kubectl get pods -n jenkins` |
| Get admin password | `kubectl exec -n jenkins <pod> -- cat /run/secrets/additional/chart-admin-password` |
| List builds (API) | `curl http://<IP>:30080/job/<job>/api/json` |
| Trigger build (API) | `curl -X POST http://<IP>:30080/job/<job>/build --user admin:<token>` |
| Check image locally | `docker run --rm -p 8080:8080 <image>:<tag>` |
| Scan image locally | `trivy image --severity HIGH,CRITICAL <image>:<tag>` |
| Inspect image | `docker inspect <image>:<tag> | jq '.[0].Config'` |
| View image layers | `docker history <image>:<tag>` |
| Check non-root user | `docker run --rm <image>:<tag> whoami` |
| Jenkins logs | `kubectl logs -n jenkins <pod>` |

**Things to reinforce throughout demo:**
1. The Jenkinsfile is code — version control it
2. Fail fast — cheap checks first, expensive operations last
3. Credentials never in Git or visible in logs
4. Multi-stage Docker = smaller images, fewer CVEs
5. Build number + registry = rollback is just redeploy of an older tag
