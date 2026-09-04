# Exception Management

Centralized exception records are defined in central-policy/exceptions.yaml.

The active policy starts with:

- exceptions: []

## Rules

1. Exceptions must be finding-specific.
2. No wildcards for service, gate, or finding.
3. Must include owner, reason, ticket, security approval, approval date, and expiry.
4. TTL is validated as expiresAt - approval.approvedAt.
5. TTL must not exceed profile max (baseline 90, standard 60, critical 30).
6. Forbidden gates cannot receive exceptions.
7. Expired exceptions are invalid and explicitly reported.
8. Unused exceptions are reported in evidence.

## Anti-patterns rejected

- skipTrivy: true
- skipSCA: true
- ignoreAllCVEs: true
- finding: "*"
- service: "*"
- gate: "*"

## Lifecycle

1. Security review and approve.
2. Add exception with bounded TTL.
3. Pipeline applies only exact matches.
4. Evidence records applied and unused exceptions.
5. Remove or renew before expiry.

Future enhancement: scheduled reporting for exceptions nearing expiry.
