# Supply Chain Security Tools Reference

This page lists the **active tools used in this repository** for the CI/CD security supply chain reference architecture.

If you are looking for proof that these controls were run, start here:

- Security evidence dashboard: [docs/security-reports/index.html](../../docs/security-reports/index.html)
- CI/CD architecture overview: [cicd-reference-architectures/README.md](../README.md)
- Pipeline implementation: [supply-chain-security-jenkins/Jenkinsfile](../supply-chain-security-jenkins/Jenkinsfile)

## Active Tool Stack In This Repository

| Control area | Tool used | Status in this repo | Reference |
|---|---|---|---|
| Source checkout and pipeline orchestration | Jenkins + Git SCM | implemented | [Jenkins pipeline](../supply-chain-security-jenkins/Jenkinsfile) |
| Unit testing | Maven Surefire + JUnit | implemented | [Sample app](../sample-application/README.md) |
| Code coverage | JaCoCo | implemented | [CI/CD architecture](../README.md#5-code-coverage) |
| SAST and code quality | SonarQube | implemented | [SonarQube SAST](./sonarqube-sast.md) |
| Secret scanning | Gitleaks | implemented | [CI/CD architecture](../README.md#2-scan-secrets) |
| Filesystem vulnerability scanning | Trivy fs | implemented | [CI/CD architecture](../README.md#3-scan-filesystem) |
| Container image scanning | Trivy image | implemented | [CI/CD architecture](../README.md#12-scan-container-image) |
| SBOM generation | Syft | implemented | [CI/CD architecture](../README.md#9-generate-sbom) |
| SBOM vulnerability analysis | Grype | implemented | [CI/CD architecture](../README.md#11-scan-sbom) |
| SBOM intelligence platform | OWASP Dependency-Track | implemented | [Dependency-Track](./dependency-track.md) |
| Image signing and attestations | Cosign | implemented | [Cosign signing](./cosign-signing.md) |
| SBOM OCI attachment | ORAS | implemented | [CI/CD architecture](../README.md#18-attach-sbom) |

## Scope Of This Tools Folder

This folder contains focused reference guides for selected components in the implemented stack:

- [SonarQube SAST](./sonarqube-sast.md)
- [Dependency-Track](./dependency-track.md)
- [Cosign signing](./cosign-signing.md)

As additional controls are expanded in this repository, matching tool guides are added here.
