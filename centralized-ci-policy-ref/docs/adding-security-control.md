# Adding a New Security Control

Example: add a centrally managed license scanner gate.

Platform/Security change steps:

1. Update jenkins-shared-library/vars/goldenCI.groovy to run scanner stage and gate stage.
2. Extend normalized finding model or adapter parsing if needed.
3. Update central-policy/security-profiles.yaml with enforcement behavior by profile.
4. Update central-policy/exception-policy.yaml if exception behavior is allowed.
5. Extend PolicyValidator checks for required policy structure.
6. Add fixtures and tests in tests/policy-engine.
7. Update root README and docs.
8. Update pipeline and/or policy versions in central-policy/policy-metadata.yaml as appropriate.

Service teams do not edit 30 different pipelines. One Golden CI update applies to all.
