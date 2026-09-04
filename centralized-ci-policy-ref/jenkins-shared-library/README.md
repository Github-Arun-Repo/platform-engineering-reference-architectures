# Jenkins Shared Library

This folder contains the Golden CI implementation consumed by:
- centralized-ci-policy-ref/jenkins/Jenkinsfile

## Structure

- vars/goldenCI.groovy: central pipeline entrypoint
- src/org/arun/ci/PolicyLoader.groovy: policy loading and digest
- src/org/arun/ci/PolicyValidator.groovy: policy and service config validation
- src/org/arun/ci/ExceptionEvaluator.groovy: finding-specific exception checks and TTL enforcement
- src/org/arun/ci/SecurityGateEvaluator.groovy: normalized finding gate decisions
- src/org/arun/ci/BuildExecutor.groovy: service build execution
- src/org/arun/ci/EvidencePublisher.groovy: machine and human evidence output
- src/org/arun/ci/model/*: normalized models

## Enterprise Placement

This repository co-locates the Shared Library for demonstration.

In a real enterprise, the Shared Library should be hosted in a dedicated protected repository and registered as a centrally governed Jenkins Shared Library source.

## Versioning

- Pipeline implementation version: golden-ci-v1.0.0
- Security policy version: security-policy-v1.0.0

Recommended rollout model:
1. Validate with policy fixtures and tests.
2. Pilot with a small subset of services.
3. Pin jobs to a known library version.
4. Expand gradually.
5. Preserve evidence of exact pipeline and policy versions.
