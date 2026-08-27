# Supply Chain Security Reference Architecture

This reference architecture shows how to design a secure container delivery pipeline from source code to registry publication, with security evidence generated at every important control point.

This architecture is build-tool agnostic. You can implement the same control flow with tools such as Jenkins, GitHub Actions, GitLab CI, Tekton, or Azure DevOps. In this repository, the implemented reference uses Jenkins. GitHub Actions is also a modern and widely used approach for teams that prefer GitHub-native CI.

[![Security Reports](https://img.shields.io/badge/Security%20Reports-View%20Dashboard-blue?logo=github)](https://github-arun-repo.github.io/platform-engineering-reference-architectures/)

## Start Here: Verified Report Evidence

Executed and validated by **Arunasalam Govindasamy** against the sample Spring Boot TODO application in this repository, with full report outputs published for public review.

- [Open Security Reports Dashboard](https://github-arun-repo.github.io/platform-engineering-reference-architectures/)
- Application under test: [sample-application](./sample-application/)
- Full security report files: [security-reports](./security-reports/)

What this implementation demonstrates with generated evidence:

- unit test execution and code coverage outputs
- explicit pre-image validation gates for secrets, tests, coverage, and dependency vulnerabilities
- SonarQube analysis with enforced `waitForQualityGate abortPipeline: true`
- Trivy-based CycloneDX SBOM generation
- Trivy severity gating before promotion (Critical fail, High configurable, Medium report-only)
- dependency intelligence publication to Dependency-Track after security gates
- secret scanning results from repository content
- registry push controls, signing, and attestation evidence
- report publication to Git so teams can review without Jenkins access

## Contents

1. [Architecture Intent](#architecture-intent)
2. [End-to-End Supply Chain](#end-to-end-supply-chain)
3. [Evidence Model](#evidence-model)
4. [Stage Navigator](#stage-navigator)
5. [1. Source and Checkout](#1-source-and-checkout)
6. [2. Scan Secrets](#2-scan-secrets)
7. [3. Secret Exposure Gate](#3-secret-exposure-gate)
9. [5. Unit Tests](#5-unit-tests)
10. [6. Unit Test Result Gate](#6-unit-test-result-gate)
11. [6.1 Software Composition Analysis (SCA)](#61-software-composition-analysis-sca)
12. [6.2 Dependency Security Gate (SCA)](#62-dependency-security-gate-sca)
13. [6.3 Static Application Security Testing (SAST)](#63-static-application-security-testing-sast)
14. [6.4 SonarQube Quality Gate (SAST)](#64-sonarqube-quality-gate-sast)
15. [7. Code Coverage](#7-code-coverage)
16. [8. Coverage Threshold Gate](#8-coverage-threshold-gate)
17. [9. Generate CycloneDX SBOM](#9-generate-cyclonedx-sbom)
18. [10. Scan Container Image](#10-scan-container-image)
19. [11. Evaluate Security Gates](#11-evaluate-security-gates)
20. [12. Publish SCA SBOM to Dependency-Track](#12-publish-sca-sbom-to-dependency-track)
21. [13. Archive Security Reports](#13-archive-security-reports)
22. [14. Publish Report Evidence](#14-publish-report-evidence)
23. [15. Push to Registry](#15-push-to-registry)
24. [16. Sign Image](#16-sign-image)
25. [17. Attest Image](#17-attest-image)
27. [19. Publish Cosign Evidence](#19-publish-cosign-evidence)
28. [Reference Implementations](#reference-implementations)
29. [Runbooks vs Reference Guides](#runbooks-vs-reference-guides)
30. [Roadmap](#roadmap)

## Architecture Intent

This reference is for engineers designing secure build pipelines for containerized workloads.

It provides a reusable pattern for answering four architectural questions:

1. **What checks must happen before an image is promoted?**
2. **What evidence should be generated for each build?**
3. **Which findings should block delivery, and which should be reported?**
4. **How can teams inspect supply chain security output without depending on Jenkins UI access?**

The sample application is intentionally small. The important part is the supply chain around it.

## End-to-End Supply Chain

The architecture is designed as a chain of evidence. Each step either validates the artifact, enriches it with metadata, or decides whether it is allowed to move forward.

```mermaid
flowchart LR
    commit[01<br/>Developer Commit]
    checkout[02<br/>Checkout]
    gitleaks[03<br/>Repository Secret Scan]
    secretGate[04<br/>Secret Exposure Gate]
    tests[06<br/>Unit Test Execution]
    testGate[07<br/>Unit Test Result Gate]
    coverage[08<br/>Coverage Evidence Generation]
    coverageGate[09<br/>Coverage Threshold Gate]
    depScan[10<br/>SCA Dependency Scan]
    depGate[11<br/>SCA Policy Gate]
    sonar[12<br/>SAST Code Analysis]
    sonarGate[13<br/>SAST Quality Gate]
    package[14<br/>Package App]
    image[15<br/>Build Image]
    sbom[16<br/>Trivy CycloneDX SBOM]
    trivyImage[17<br/>Container Image Vulnerability Scan]
    gates[18<br/>Container Security Policy Gate]
    dtrack[19<br/>Dependency-Track Upload]
    archive[20<br/>Archive Reports]
    reportCommit[21<br/>Publish Report Evidence]
    registry[22<br/>Registry Push]
    sign[23<br/>Cosign Sign Default On]
    attest[24<br/>Cosign Attest Default On]
    cosignCommit[26<br/>Publish Cosign Evidence]
    evidence[27<br/>Evidence Dashboard]

    commit --> checkout --> gitleaks --> secretGate --> tests --> testGate --> coverage --> coverageGate --> depScan --> depGate --> sonar --> sonarGate --> package --> image --> sbom --> trivyImage --> gates --> dtrack --> archive --> reportCommit --> registry --> sign --> attest --> cosignCommit --> evidence
    gitleaks --> gates
    secretGate --> gates
    testGate --> gates
    coverageGate --> gates
    depGate --> gates
    trivyImage --> gates
    gitleaks --> reportCommit
    trivyImage --> reportCommit
    reportCommit --> evidence
    cosignCommit --> evidence

    classDef source fill:#e7f0ff,stroke:#1f6feb,color:#0b1f44,stroke-width:1px;
    classDef quality fill:#ecfdf3,stroke:#1a7f37,color:#062b16,stroke-width:1px;
    classDef artifact fill:#fff8c5,stroke:#9a6700,color:#3b2300,stroke-width:1px;
    classDef security fill:#ffebe9,stroke:#cf222e,color:#4d1113,stroke-width:1px;
    classDef publish fill:#f6f8fa,stroke:#57606a,color:#24292f,stroke-width:1px;

    class commit,checkout source;
    class tests,testGate,coverage,coverageGate,sonar,sonarGate quality;
    class package,image,sbom,dtrack,archive artifact;
    class trivyImage,gitleaks,secretGate,depScan,depGate,gates,sign,attest security;
    class registry,reportCommit,cosignCommit,evidence publish;
```

This is the core chain: code enters, controls run one after another, evidence is produced, and only approved artifacts move forward.

## Evidence Model

```mermaid
flowchart LR
    build[Jenkins Build]
    reports[Generated Reports]
    git[Git Evidence Store]
    dashboard[Public Dashboard]
    engineers[Engineers and Reviewers]

    build --> reports
    reports --> git
    git --> dashboard
    dashboard --> engineers
```

Jenkins executes the pipeline. Git stores the published evidence. The dashboard provides the review surface.

Current dashboard:

- [Security Reports Dashboard](https://github-arun-repo.github.io/platform-engineering-reference-architectures/)

## Stage Navigator

Click any stage to inspect what it does, why it exists, and where it is useful.

| Order | Stage | Tool | Used For | Gate |
|---:|---|---|---|---|
| 1 | [Source and Checkout](#1-source-and-checkout) | Git + Jenkins SCM | traceable source input | yes, if checkout fails |
| 2 | [Scan Secrets](#2-scan-secrets) | Gitleaks | repository secret scan | reported to next gate |
| 3 | [Secret Exposure Gate](#3-secret-exposure-gate) | Jenkins gate logic | secret exposure gate | yes |
| 5 | [Unit Tests](#5-unit-tests) | Maven Surefire + JUnit | unit test execution | reported to next gate |
| 6 | [Unit Test Result Gate](#6-unit-test-result-gate) | Jenkins gate logic | unit test result gate | yes |
| 7 | [Code Coverage](#7-code-coverage) | JaCoCo | coverage evidence generation - JaCoCo for Java only | reported to next gate |
| 8 | [Coverage Threshold Gate](#8-coverage-threshold-gate) | Jenkins gate logic + JaCoCo XML | coverage threshold gate | yes |
| 9 | [Software Composition Analysis](#61-software-composition-analysis-sca) | OWASP Dependency-Check | software composition analysis pre-image | reported to next gate |
| 10 | [Dependency Security Gate](#62-dependency-security-gate-sca) | Jenkins gate logic + Dependency-Check JSON | dependency security gate (Critical/High) | yes |
| 11 | [Static Application Security Testing](#63-static-application-security-testing-sast) | SonarQube | static application security testing analysis | reported to next gate |
| 12 | [SonarQube Quality Gate](#64-sonarqube-quality-gate-sast) | SonarQube `waitForQualityGate` | sonar quality gate enforcement | yes |
| 13 | [Package Application](#7-package-application) | Maven | build JAR artifact | yes |
| 14 | [Build Container Image](#8-build-container-image) | Docker | immutable runtime artifact | yes |
| 15 | [Generate CycloneDX SBOM](#9-generate-cyclonedx-sbom) | Trivy | CycloneDX package inventory for reuse | reported |
| 16 | [Scan Container Image](#10-scan-container-image) | Trivy image | container image vulnerability scan | severity gated |
| 17 | [Evaluate Security Gates](#11-evaluate-security-gates) | Jenkins policy logic | container security policy gate before publication/push | yes |
| 18 | [Publish SCA SBOM to Dependency-Track](#12-publish-sca-sbom-to-dependency-track) | OWASP Dependency-Track | SCA SBOM publication after gates | best effort |
| 19 | [Archive Security Reports](#13-archive-security-reports) | Jenkins artifacts | audit and evidence retention | reported |
| 20 | [Publish Report Evidence](#14-publish-report-evidence) | Jenkins + Git + HTML | public report publication after gating and post-gate uploads | reported |
| 21 | [Push to Registry](#15-push-to-registry) | Docker | artifact promotion | yes |
| 22 | [Sign Image](#16-sign-image) | Cosign | digest integrity proof | yes (default on) |
| 23 | [Attest Image](#17-attest-image) | Cosign | SBOM and build evidence referrers | yes (default on) |
| 25 | [Publish Cosign Evidence](#19-publish-cosign-evidence) | Jenkins + Git + HTML | public signing and attestation evidence | yes (default on) |

## 1. Source and Checkout

**What happens**

Jenkins checks out the repository and pins the pipeline to a specific source revision.

**Why this matters**

Every downstream artifact must be traceable to source code. Without a clean source checkpoint, SBOMs, images, scan reports, and coverage output lose their audit value.

**Where this is useful**

- regulated build pipelines
- incident investigation
- artifact provenance
- rollback analysis

**Reference implementation**

- [Jenkinsfile](./supply-chain-security-pipeline/Jenkinsfile)

## 2. Scan Secrets

**What happens**

Gitleaks scans the repository immediately after checkout for hardcoded credentials, keys, and tokens.

**Why this matters**

Secrets in source control are high-risk findings and should fail the build before the pipeline spends time compiling, packaging, or creating images.

Current behavior in Jenkins: this stage records findings first, then the dedicated **Secret Exposure Gate** stage enforces failure when findings or scan errors are detected.

**Where this is useful**

- repository protection
- early fail-fast validation
- credential hygiene
- audit evidence for secret scanning

**Evidence produced**

- [Gitleaks Secret Report](https://github-arun-repo.github.io/platform-engineering-reference-architectures/gitleaks-report.html)

## 3. Secret Exposure Gate

**What happens**

Jenkins evaluates the Gitleaks exit status and fails immediately when findings or scan errors are detected.

**Why this matters**

Secret exposure is a high-impact issue, so the pipeline blocks before any packaging, image build, or promotion work continues.

**Where this is useful**

- fail-fast policy enforcement
- credential hygiene controls
- release risk reduction
- audit-ready evidence of enforcement

**Evidence produced**

- [Gitleaks Secret Report](https://github-arun-repo.github.io/platform-engineering-reference-architectures/gitleaks-report.html)

## 5. Unit Tests

**What happens**

Maven Surefire runs JUnit tests for the Spring Boot sample application.

**Why this matters**

Unit tests catch broken behavior before the pipeline spends time creating images, SBOMs, and registry artifacts. This is the first functional quality gate.

**Where this is useful**

- API validation
- regression protection
- pull request checks
- release candidate validation

**Evidence produced**

- Surefire XML test reports
- Jenkins JUnit test result view

## 7. Code Coverage

**What happens**

JaCoCo generates coverage evidence from the unit test run.

**Why this matters**

Coverage does not prove quality by itself, but it shows which code paths are exercised by tests. It is useful evidence when reviewing release readiness and test depth.

**Where this is useful**

- release reviews
- quality dashboards
- pull request standards
- future SonarQube quality gates

**Evidence produced**

- [JaCoCo Coverage Report](https://github-arun-repo.github.io/platform-engineering-reference-architectures/jacoco/)

## 6. Unit Test Result Gate

**What happens**

This phase explicitly separates SCA (dependency risk) from SAST (source code risk) before image build.

### 6.1 Software Composition Analysis (SCA)

**Tool**: OWASP Dependency-Check

- Runs mandatory SCA before image build.
- Scans third-party dependencies for known vulnerabilities and produces JSON/HTML reports.

### 6.2 Dependency Security Gate (SCA)

- Critical CVEs fail when `DEPENDENCY_GATE_FAIL_ON_CRITICAL=true`.
- High CVEs fail when `DEPENDENCY_GATE_FAIL_ON_HIGH=true`.

SCA reference:

- [Software Composition Analysis Reference](./tools/dependency-track.md)

### 6.3 Static Application Security Testing (SAST)

**Tool**: SonarQube

- Runs mandatory source code analysis.
- Imports JaCoCo coverage and evaluates source-level quality/security rules.

### 6.4 SonarQube Quality Gate (SAST)

- Enforced with `waitForQualityGate abortPipeline: true`.
- Blocks pipeline progression when SonarQube quality gate conditions are not met.

SAST reference:

- [SonarQube SAST](./tools/sonarqube-sast.md)

**Why this matters**

SAST belongs before package and image promotion because source-level vulnerabilities and maintainability issues should be reviewed before the pipeline creates deployable artifacts.
SCA also belongs in this phase so vulnerable third-party dependencies can be identified before promotion.

**Where this is useful**

- Java service quality gates
- SAST evidence before image promotion
- SCA dependency risk checks before image promotion
- code coverage governance
- security hotspot review
- future pull request checks

**Tool references**

- [Software Composition Analysis Reference](./tools/dependency-track.md)
- [SonarQube SAST](./tools/sonarqube-sast.md)

<a id="7-package-application"></a>
## 7. Code Coverage

**What happens**

Maven packages the Spring Boot application into an executable JAR.

**Why this matters**

The JAR is the application artifact copied into the container image. A packaging failure should stop the pipeline before container build.

**Where this is useful**

- Java service builds
- release artifact creation
- reproducible application packaging

<a id="8-build-container-image"></a>
## 8. Coverage Threshold Gate

**What happens**

Docker builds the final runtime image and tags it with the Jenkins build number and `latest`.

**Why this matters**

The image is the deployable unit. It must be immutable, traceable, and tested before promotion.

**Where this is useful**

- Kubernetes deployments
- GitOps release flows
- container registry promotion
- environment parity across dev, staging, and production

## 9. Generate CycloneDX SBOM

**What happens**

Trivy scans the built image and generates a CycloneDX SBOM.

**Formats and outputs used**

- CycloneDX JSON: primary reusable SBOM artifact (`security-reports/sbom.cyclonedx.json`)
- Trivy SBOM log: generation diagnostics (`security-reports/sbom.trivy.log`)
- HTML summary: quick review (`security-reports/sbom-report.html`)

**Why this matters**

An SBOM answers: "What is inside this artifact?" It creates the package inventory needed for vulnerability analysis, incident response, and compliance review.

**Where this is useful**

- vulnerability management
- vendor reviews
- compliance evidence
- incident response when a new CVE is announced

**Evidence produced**

- [SBOM Report](https://github-arun-repo.github.io/platform-engineering-reference-architectures/sbom-report.html)

## 10. Scan Container Image

**What happens**

Trivy scans the built container image and generates JSON and HTML vulnerability reports.

**Why this matters**

This validates the deployable image artifact against known CVEs across runtime package layers.

**Where this is useful**

- dependency risk analysis
- build gates before registry push
- re-scanning historical artifacts
- CVE response workflows

**Evidence produced**

- [Trivy Image Report](https://github-arun-repo.github.io/platform-engineering-reference-architectures/trivy-report.html)

## 11. Evaluate Security Gates

**What happens**

Jenkins evaluates security gate inputs before promotion and before Dependency-Track publication.

**Why this matters**

Security reports are useful, but gates decide whether the artifact is allowed to continue in the supply chain.

**Security gate policy (implemented)**

| Control | Report/status checked | Fail condition | Warning condition | Pass condition |
|---|---|---|---|---|
| Gitleaks secrets gate | `security-reports/gitleaks-exit-code.txt` and `security-reports/gitleaks-report.json` | exit code `1` (findings) or `>1` (scan/runtime error) | none | exit code `0` |
| Trivy image vulnerability gate | `security-reports/trivy-scan-exit-code.txt`, `security-reports/trivy-critical-count.txt`, `security-reports/trivy-high-count.txt`, `security-reports/trivy-medium-count.txt` | Critical `> 0` always fails; High `> 0` fails only when `TRIVY_FAIL_ON_HIGH=true` | High findings when `TRIVY_FAIL_ON_HIGH=false`; Medium findings are report-only | scan status `0`, Critical `0`, and High policy satisfied |

**Current threshold configuration set (Jenkins environment)**

- `TRIVY_FAIL_ON_HIGH=false`
- `TRIVY_BLOCK_ON_SCAN_ERROR=true`

With current settings:

- any Gitleaks finding fails in the secret exposure gate stage
- any Trivy critical vulnerability fails in the security gate stage
- Trivy high vulnerabilities warn and continue by default
- Trivy medium vulnerabilities are report-only
- Trivy scan/tool errors fail by default

## 12. Publish SCA SBOM to Dependency-Track

**What happens**

After security gates pass, Jenkins uploads the generated CycloneDX SBOM (SCA artifact) to Dependency-Track.

**Why this matters**

This keeps Dependency-Track publication aligned with promoted artifacts while preserving central SBOM history and vulnerability intelligence.

**Where this is useful**

- SBOM historical tracking
- long-lived vulnerability intelligence
- compliance evidence
- release review and audit trails

**Evidence produced**

- [Dependency-Track SBOM Publish Report](https://github-arun-repo.github.io/platform-engineering-reference-architectures/dependency-track-report.html)

## 13. Archive Security Reports

**What happens**

Jenkins archives all generated security JSON/HTML/TXT outputs as build artifacts.

**Why this matters**

This gives durable, build-bound security evidence for audits, automation, and release reviews.

**Where this is useful**

- release evidence retention
- compliance checks
- post-build automation
- incident response traceability

**Evidence produced**

- Jenkins build artifacts under `security-reports/*`

## 14. Publish Report Evidence

**What happens**

After security gating and post-gate SBOM publication, Jenkins commits the latest report artifacts back into the repository and updates the public dashboard content.

**Why this matters**

This makes the security evidence visible in Git and dashboard form so reviewers can inspect exact build outputs without requiring Jenkins access.

**Where this is useful**

- release review preparation
- audit-friendly evidence publication
- engineering demos and walkthroughs
- failure analysis when promotion is blocked

## 15. Push to Registry

**What happens**

The image is pushed to Docker Hub after required gates pass.

**Why this matters**

The registry should receive only artifacts that have passed the required quality and security checks.

**Where this is useful**

- Kubernetes deployments
- GitOps image promotion
- release candidate storage
- rollback to known-good images

**Follow-up controls**

After the image is pushed, the strongest next steps are to sign the immutable digest and attach attestations to it.

- [Cosign Signing](./tools/cosign-signing.md)

## 16. Sign Image

**What happens**

When Cosign is enabled, Jenkins signs the immutable pushed image digest after registry publication.

**Why this matters**

Signing proves integrity and gives downstream consumers a way to verify that the digest they are pulling is the one the pipeline intended to publish.

**Where this is useful**

- clear supply chain demonstrations
- cluster admission verification
- release provenance and trust establishment

**Tool reference**

- [Cosign Signing](./tools/cosign-signing.md)

## 17. Attest Image

**What happens**

When Cosign is enabled, Jenkins creates attestations for the CycloneDX SBOM and build metadata predicate.

**Why this matters**

Attestations carry evidence about the artifact, not just a proof that the artifact was signed. This is where package inventory and build context become verifiable OCI referrers.

**Where this is useful**

- SBOM-linked release evidence
- policy engines consuming predicates
- downstream supply chain verification

**Tool reference**

- [Cosign Signing](./tools/cosign-signing.md)

## 19. Publish Cosign Evidence

**What happens**

When Cosign is enabled, Jenkins publishes signing, verification, attestation, and referrer evidence into `supply-chain-security-ref/docs/security-reports/` and exposes it through the same public dashboard.

**Why this matters**

Signing only helps if others can inspect and verify it. Publishing the evidence makes the signature flow visible and reviewable for other engineers.

**Where this is useful**

- architecture reviews
- release reviews
- audit preparation
- team enablement
- security exception discussions

**Evidence entry point**

- [Security Reports Dashboard](https://github-arun-repo.github.io/platform-engineering-reference-architectures/)

## Reference Implementations

### Jenkins (Chosen in this Repository)

Use this reference when you need:

- self-hosted execution
- Kubernetes-based build agents
- Jenkins credentials integration
- detailed stage-level operational visibility
- custom report publication back into Git

Files:

- [Jenkins reference](./supply-chain-security-pipeline/)
- [Jenkinsfile](./supply-chain-security-pipeline/Jenkinsfile)
- [Jenkins and SonarQube installation](./supply-chain-security-pipeline/installation-jenkins.md)
- [Jenkins runbook](./supply-chain-security-pipeline/jenkins-demo-runbook.md)

## Runbooks vs Reference Guides

Use this README for architecture reference.

Use the runbook for job execution.

| Document | Purpose |
|---|---|
| Main README | Architecture, control placement, diagrams, evidence model |
| Jenkins runbook | Installation checks, job execution, console inspection, troubleshooting |
| Sample app README | Application-specific context |

## Repository Map

```text
supply-chain-security-ref/
├── README.md
├── sample-application/
├── supply-chain-security-pipeline/
│   ├── Jenkinsfile
│   ├── installation-jenkins.md
│   └── jenkins-demo-runbook.md
└── ./docs/security-reports/
```

## Roadmap

Planned additions:

1. admission-time verification of signatures and attestations
2. policy-based promotion using signed evidence
3. GitOps manifest update
4. environment promotion strategy
5. policy-as-code gates
6. license compliance reporting
7. admission controller integration
8. registry-native evidence discovery

## Quick Links

- [Security Reports Dashboard](https://github-arun-repo.github.io/platform-engineering-reference-architectures/)
- [Tools Reference](./tools/README.md)
- [Cosign Signing Reference](./tools/cosign-signing.md)
- [SonarQube SAST Reference](./tools/sonarqube-sast.md)
- [Dependency-Track SCA Reference](./tools/dependency-track.md)
- [Jenkins Reference](./supply-chain-security-pipeline/)
- [Jenkins Runbook](./supply-chain-security-pipeline/jenkins-demo-runbook.md)
- [Sample Application](./sample-application/)
- [Main Repository README](../README.md)
