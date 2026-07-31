# Software Composition Analysis Reference

OWASP Dependency-Check and OWASP Dependency-Track are the selected SCA controls for this reference architecture.

In this repository, SCA is split into two layers:

- build-time SCA scan and gate with OWASP Dependency-Check
- SBOM intelligence publication with OWASP Dependency-Track

This pattern keeps immediate enforcement in CI while preserving longer-term component intelligence.

## Where SCA Fits

```mermaid
flowchart LR
    commit[Developer Commit]
    tests[Unit Tests]
    coverage[Coverage]
    depcheck[SCA Scan Dependency-Check]
    scagate[SCA Policy Gate]
    sast[SAST SonarQube]
    package[Package App]
    image[Build Image]
    sbom[Trivy CycloneDX SBOM]
    dtrack[Dependency-Track Upload]
    dashboard[Evidence Dashboard]

    commit --> tests --> coverage --> depcheck --> scagate --> sast --> package --> image --> sbom --> dtrack --> dashboard

    classDef source fill:#e7f0ff,stroke:#1f6feb,color:#0b1f44;
    classDef quality fill:#ecfdf3,stroke:#1a7f37,color:#062b16;
    classDef security fill:#ffebe9,stroke:#cf222e,color:#4d1113;
    classDef artifact fill:#fff8c5,stroke:#9a6700,color:#3b2300;

    class commit source;
    class tests,coverage quality;
    class depcheck,scagate,sast,dtrack security;
    class package,image,sbom,dashboard artifact;
```

SCA runs before artifact promotion and image publication. The SBOM upload runs after gates so only approved artifacts are published into the SCA system of record.

## Why This SCA Stack Is Chosen

This repository uses OWASP Dependency-Check plus Dependency-Track because the combination gives:

- actionable dependency vulnerability scan results during the build
- explicit gate controls for Critical and High findings
- CycloneDX SBOM ingestion and historical tracking
- API-driven CI/CD integration with evidence outputs
- low licensing friction for education, labs, and platform reference patterns

## What Is Implemented In This Repository

### Build-Time SCA Scan

Stage: SCA Dependency Scan OWASP Dependency-Check

- Maven dependency-check goal runs during pre-image controls.
- JSON and HTML reports are generated into security-reports.

### Build-Time SCA Gate

Stage: SCA Policy Gate Critical High

- pipeline fails when Critical findings exist and DEPENDENCY_GATE_FAIL_ON_CRITICAL is true
- pipeline fails when High findings exist and DEPENDENCY_GATE_FAIL_ON_HIGH is true

### SCA Intelligence Publication

Stage: Publish SCA SBOM to Dependency-Track

- Trivy-generated CycloneDX SBOM is uploaded to Dependency-Track
- upload response and status evidence are published into reports and dashboard

## Jenkins Inputs Used

| Item | Purpose |
|---|---|
| DEPENDENCY_GATE_FAIL_ON_CRITICAL | Fail gate when Critical vulnerabilities are detected |
| DEPENDENCY_GATE_FAIL_ON_HIGH | Fail gate when High vulnerabilities are detected |
| DEPENDENCY_TRACK_URL | Base URL of Dependency-Track server |
| DEPENDENCY_TRACK_API_KEY_CREDENTIALS_ID | Jenkins secret text credential ID for API key |
| DEPENDENCY_TRACK_PROJECT_NAME | Project name in Dependency-Track |
| IMAGE_TAG | Version value used for SBOM upload |

Current repository defaults for Dependency-Track integration:

| Jenkins setting | Value |
|---|---|
| DEPENDENCY_TRACK_URL | http://dtrack-dependency-track-api-server.dependency-track.svc.cluster.local:8080 |
| DEPENDENCY_TRACK_API_KEY_CREDENTIALS_ID | owasp_dependency_track |
| DEPENDENCY_TRACK_PROJECT_NAME | platform-engineering-reference-architectures |

## Demo Choice

| Decision | Value |
|---|---|
| Build-time SCA tool | OWASP Dependency-Check |
| SBOM intelligence platform | OWASP Dependency-Track |
| License cost signal | Open source path available |
| Deployment model | Self-hosted inside Kubernetes-compatible environments |
| Why this stack | Good fit for reference architecture demos and reproducible DevSecOps patterns |

## Licensed and Enterprise Alternatives

Pricing and features evolve frequently, so treat this as a decision guide and verify current commercial terms directly with vendors.

| Tool | Open source or free path | Licensed path | Strengths | Tradeoffs |
|---|---|---|---|---|
| OWASP Dependency-Check + Dependency-Track | Open source | N/A for core OSS | Strong educational and platform pattern fit, CycloneDX alignment, API integration | More self-managed operations and tuning effort |
| Trivy | Open source scanner | Enterprise options via ecosystem vendors | Fast CI scan, broad artifact coverage, good SBOM support | Governance surface depends on surrounding platform |
| Snyk Open Source | Free tier | Paid Snyk plans | Developer-friendly SaaS workflow, rich ecosystem | SaaS dependency and plan-based cost |
| Mend | Limited evaluation options | Commercial | Enterprise policy workflows and reporting | Commercial onboarding and cost |
| Sonatype Nexus Lifecycle IQ | Limited evaluation options | Commercial | Strong policy and package governance in enterprise pipelines | Commercial setup and operating model |
| JFrog Xray | Platform dependent | Commercial | Deep integration when Artifactory is central | Best value when aligned to JFrog stack |
| GitHub Dependabot plus advisory graph | Free for many scenarios | Enterprise value through broader GitHub programs | Native GitHub pull request remediation path | Less complete as a standalone SBOM intelligence platform |

## Selection Guidance

| Scenario | Recommended path |
|---|---|
| Platform reference architecture and internal demos | OWASP Dependency-Check plus Dependency-Track |
| Fast build-time scan-first workflows | Trivy plus dependency policy gate |
| GitHub-centric dependency remediation | Dependabot with policy guardrails |
| Enterprise governance with commercial support and compliance workflows | Evaluate Snyk, Mend, Sonatype IQ, or JFrog Xray |

## How To Integrate Dependency-Track With Jenkins

1. Deploy Dependency-Track in your environment.
2. Create a project or allow auto-create flow.
3. Generate an API key with BOM upload permissions.
4. Store API key in Jenkins credentials with ID owasp_dependency_track.
5. Set DEPENDENCY_TRACK_URL and DEPENDENCY_TRACK_PROJECT_NAME in pipeline environment.
6. Confirm build generates dependency-track-report.json and dependency-track-report.html.
7. Confirm docs dashboard shows Dependency-Track upload evidence.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| SCA gate fails unexpectedly | Dependency-Check found Critical or High issues under active thresholds |
| SCA gate never fails | DEPENDENCY_GATE_FAIL_ON_CRITICAL and DEPENDENCY_GATE_FAIL_ON_HIGH are disabled |
| Dependency-Track upload skipped | DEPENDENCY_TRACK_URL not configured |
| Dependency-Track upload fails 401 or 403 | API key is missing or lacks BOM upload permission |
| Evidence report missing in dashboard | Pipeline did not reach report publication stage |

## Reference Links

- OWASP Dependency-Check project: https://owasp.org/www-project-dependency-check/
- OWASP Dependency-Track project: https://dependencytrack.org/
- CycloneDX specification: https://cyclonedx.org/specification/