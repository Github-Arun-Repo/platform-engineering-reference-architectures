# Jenkins Installation Guide

## 1. Required Plugins

- Pipeline
- Pipeline: Groovy
- Pipeline Utility Steps
- Job DSL
- Git
- Credentials Binding
- Kubernetes

## 2. Register Shared Library

Jenkins -> Manage Jenkins -> System -> Global Pipeline Libraries

- Name: company-golden-ci
- Default version: v1.0.0
- Retrieval: Modern SCM (Git)
- Source repository: this demo repository or enterprise shared-library repository

## 3. Credentials (placeholders)

Configure the following credential IDs:
- github-read-credentials
- sonarqube-token
- nvd-api-key
- registry-credentials
- dependency-track-token
- cosign-private-key
- cosign-password

## 4. Stage-Scoped Credential Isolation

- Checkout: github-read-credentials only
- Dependency scan: nvd-api-key only
- SAST: sonarqube-token only
- Dependency-Track publication: dependency-track-token only
- Image push: registry-credentials only
- Image signing: cosign-private-key and cosign-password only

Do not expose registry or signing credentials to service-controlled build/test stages.

## 5. Kubernetes Cloud and Agent

- Configure Jenkins Kubernetes cloud.
- Use centralized-ci-policy-ref/jenkins/pod-template.yaml.
- Keep service account permissions minimal.
- Avoid privileged pods unless strictly needed.

## 6. Seed Job for Job DSL

Create a seed job that runs:
- script path: centralized-ci-policy-ref/jenkins/job-dsl.groovy

This creates centrally controlled jobs for all five services and injects trusted SERVICE_ID and SERVICE_PATH values.

## 7. Run Pipeline

Each generated job uses:
- script path: centralized-ci-policy-ref/jenkins/Jenkinsfile

The Jenkinsfile only calls the versioned Shared Library entrypoint.
