# Security Profiles

Profiles define enforcement strictness for one Golden CI pipeline. They do not create separate pipelines.

## Profiles

- baseline: lower-risk internal workloads
- standard: default enterprise workloads
- critical: high-impact or internet-facing workloads

## Control Matrix

| Control | Baseline | Standard | Critical |
|---|---|---|---|
| Secret scan | Block | Block | Block |
| Unit tests | Block | Block | Block |
| Coverage | 60% | 70% | 80% |
| SCA Critical | Block | Block | Block |
| SCA High | Warn | Block | Block |
| SAST execution | Required | Required | Required |
| Sonar quality gate | Block | Block | Strict Block |
| Container Critical | Block | Block | Block |
| Container High | Warn | Block | Block |
| SBOM | Required | Required | Required |
| Signing | Required | Required | Required |
| Attestation | Optional/Report | Required | Required |
| Provenance verification | Report | Required | Required |
| Maximum exception TTL | 90 days | 60 days | 30 days |
| Scanner execution error | Block | Block | Block |

Reference thresholds should be adapted to organization risk and compliance requirements.
