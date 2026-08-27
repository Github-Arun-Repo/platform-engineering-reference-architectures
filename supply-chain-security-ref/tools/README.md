# Supply Chain Security Tools Reference

This page lists the **active tools used in this repository** for the CI/CD security supply chain reference architecture.

If you are looking for proof that these controls were run, start here:

- Security evidence dashboard: [docs/security-reports/index.html](../docs/security-reports/index.html)
- CI/CD architecture overview: [supply-chain-security-ref/README.md](../README.md)
- Pipeline implementation: [supply-chain-security-pipeline/Jenkinsfile](../supply-chain-security-pipeline/Jenkinsfile)

## Active Tool Stack In This Repository

| Control area | Tool used | Status in this repo | Reference |
|---|---|---|---|
| Source checkout and pipeline orchestration | Jenkins + Git SCM | implemented | [Jenkins pipeline](../supply-chain-security-pipeline/Jenkinsfile) |
| Unit testing | Maven Surefire + JUnit | implemented | [Unit test execution](../README.md#4-unit-test-execution-junit) |
| Code coverage | JaCoCo | implemented | [Coverage evidence](../README.md#6-coverage-evidence-generation-jacoco) |
| SAST and code quality | SonarQube | implemented | [SonarQube SAST](./sonarqube-sast.md) |
| Secret scanning | Gitleaks | implemented | [Repository secret scan](../README.md#2-repository-secret-scan) |
| Container image scanning | Trivy image | implemented | [Container image scan](../README.md#15-container-image-vulnerability-scan-trivy) |
| SBOM generation | Trivy CycloneDX | implemented | [CycloneDX SBOM](../README.md#14-generate-cyclonedx-sbom-with-trivy) |
| Vulnerability gate evaluation | Trivy severity policy | implemented | [Container security gate](../README.md#16-container-security-policy-gate-trivy) |
| SBOM intelligence platform | OWASP Dependency-Track | implemented | [Dependency-Track](./dependency-track.md) |
| Image signing and attestations | Cosign | implemented | [Cosign signing](./cosign-signing.md) |

## Scope Of This Tools Folder

This folder contains focused reference guides for selected components in the implemented stack:

- [SonarQube SAST](./sonarqube-sast.md)
- [Dependency-Track](./dependency-track.md)
- [Cosign signing](./cosign-signing.md)

As additional controls are expanded in this repository, matching tool guides are added here.
