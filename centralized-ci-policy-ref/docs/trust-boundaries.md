# Trust Boundaries

## Trusted Inputs

- Central service catalog and assigned profiles.
- Central security profile thresholds.
- Central exception policy and approved exceptions.
- Centrally injected SERVICE_ID and SERVICE_PATH.

## Untrusted Inputs

- Service source code and tests.
- Service Dockerfile.
- Service ci.yaml build commands.

Build commands are treated as untrusted workload input and executed with least privilege.

## Credential Boundaries

Credentials are injected only into stages that require them.

Build/test stages do not receive registry, cosign, or dependency-track credentials.

## Artifact Trust

Only after all blocking gates pass:

1. Push image.
2. Resolve immutable digest.
3. Sign digest.
4. Attest digest.
5. Verify digest provenance.
6. Record all outcomes in evidence.

## Security Notes for Pod Template

- Docker socket access should be minimized.
- Prefer rootless build solutions where possible.
- Keep workspace sharing explicit.
- Keep service account RBAC minimal.
- Pin tool versions.
- Avoid hard-coded secrets.
