# Onboarding a New Service

## Add a 31st Service

1. Create service directory and source code.
2. Add build-only ci.yaml.
3. Add tests and Dockerfile.
4. Platform adds service entry to central-policy/service-catalog.yaml.
5. Security/Architecture assigns baseline, standard, or critical profile.
6. Platform updates centralized-ci-policy-ref/jenkins/job-dsl.groovy with SERVICE_ID and SERVICE_PATH.
7. Seed job creates centrally managed Jenkins job.
8. Jenkins job injects trusted identity values.
9. Golden CI executes all mandatory controls and publishes evidence with policy and artifact digest.

Service teams must not copy security stages into service-level Jenkinsfiles.
