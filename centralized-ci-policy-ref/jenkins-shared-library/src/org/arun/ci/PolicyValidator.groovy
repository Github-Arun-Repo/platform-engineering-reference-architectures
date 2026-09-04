package org.arun.ci

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion

class PolicyValidator {
    private static final Set<String> FORBIDDEN_KEYS = [
        'profile', 'service', 'serviceId', 'security', 'gates', 'exceptions', 'trivy', 'sonar', 'sast',
        'sca', 'sbom', 'signing', 'attestation', 'coverageThreshold', 'skipSecurity', 'ignoreAllCVEs',
        'securityProfile', 'scannerFailurePolicy', 'dependencyTrackRequired'
    ] as Set

    void validateCentralPolicy(Map policyBundle) {
        require(policyBundle.metadata?.pipeline?.version, 'Missing pipeline version in policy-metadata.yaml')
        require(policyBundle.metadata?.policy?.version, 'Missing policy version in policy-metadata.yaml')
        require(policyBundle.catalog?.services instanceof Map, 'service-catalog.yaml must define services map')
        require(policyBundle.profiles?.profiles instanceof Map, 'security-profiles.yaml must define profiles map')
        require(policyBundle.exceptionPolicy?.exceptionPolicy instanceof Map, 'exception-policy.yaml must define exceptionPolicy map')

        validateKnownProfiles(policyBundle.catalog, policyBundle.profiles)
        validateServicePaths(policyBundle.catalog)
        validateProfileControls(policyBundle.profiles)
        validateExceptionPolicy(policyBundle.exceptionPolicy)
    }

    void validateKnownProfiles(Map catalog, Map profiles) {
        Set<String> profileNames = (profiles.profiles as Map).keySet() as Set<String>
        (catalog.services as Map).each { String serviceId, Map service ->
            String profile = service.profile as String
            if (!profileNames.contains(profile)) {
                throw new IllegalArgumentException("Service '${serviceId}' references unknown profile '${profile}'.")
            }
        }
    }

    void validateServicePaths(Map catalog) {
        Map<String, String> byPath = [:]
        (catalog.services as Map).each { String serviceId, Map service ->
            String path = service.path as String
            require(path, "Service '${serviceId}' must define a non-empty path")
            if (byPath.containsKey(path)) {
                throw new IllegalArgumentException("Duplicate service path '${path}' for '${serviceId}' and '${byPath[path]}'.")
            }
            byPath[path] = serviceId
        }
    }

    void validateProfileControls(Map profiles) {
        ['baseline', 'standard', 'critical'].each { name ->
            Map profile = (Map) profiles.profiles[name]
            require(profile, "Missing required profile '${name}'")
            require(profile.coverage?.minimumLinePercent != null, "Profile '${name}' missing coverage.minimumLinePercent")
            require(profile.gates?.secretExposure, "Profile '${name}' missing gates.secretExposure")
            require(profile.gates?.scannerExecutionFailure, "Profile '${name}' missing gates.scannerExecutionFailure")
            require(profile.gates?.dependencyVulnerability, "Profile '${name}' missing gates.dependencyVulnerability")
            require(profile.gates?.containerVulnerability, "Profile '${name}' missing gates.containerVulnerability")
        }
        Map critical = (Map) profiles.profiles.critical
        require(critical.sonarStrictPolicy?.qualityGateStatus == 'OK', 'Critical profile must enforce Sonar qualityGateStatus=OK')
        require((critical.sonarStrictPolicy?.maxNewCriticalIssues as Integer) == 0, 'Critical profile must enforce maxNewCriticalIssues=0')
        require((critical.sonarStrictPolicy?.maxNewBlockerIssues as Integer) == 0, 'Critical profile must enforce maxNewBlockerIssues=0')
        require(critical.sonarStrictPolicy?.requireNewCodeQualityGate == true, 'Critical profile must enforce requireNewCodeQualityGate=true')
    }

    void validateExceptionPolicy(Map exceptionPolicyWrapper) {
        Map policy = (Map) exceptionPolicyWrapper.exceptionPolicy
        require(policy.allowed instanceof List, 'exceptionPolicy.allowed must be a list')
        require(policy.forbidden instanceof List, 'exceptionPolicy.forbidden must be a list')
        require(policy.maxTtlDays?.baseline != null, 'exceptionPolicy.maxTtlDays.baseline required')
        require(policy.maxTtlDays?.standard != null, 'exceptionPolicy.maxTtlDays.standard required')
        require(policy.maxTtlDays?.critical != null, 'exceptionPolicy.maxTtlDays.critical required')
    }

    void validateServiceConfig(Map serviceConfig, JsonNode schemaNode) {
        ObjectMapper mapper = new ObjectMapper()
        JsonNode serviceConfigNode = mapper.valueToTree(serviceConfig)

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
        def schema = factory.getSchema(schemaNode)
        Set errors = schema.validate(serviceConfigNode)
        if (!errors.isEmpty()) {
            String message = errors.collect { it.message }.join('; ')
            throw new IllegalArgumentException("Service configuration rejected by JSON schema: ${message}")
        }

        List<String> forbidden = findForbiddenKeys(serviceConfig)
        if (!forbidden.isEmpty()) {
            throw new IllegalArgumentException(
                "Service configuration rejected. Forbidden centrally controlled field detected: ${forbidden[0]}. Service repositories may declare build facts only."
            )
        }
    }

    List<String> findForbiddenKeys(Object node, String path = '') {
        List<String> findings = []
        if (node instanceof Map) {
            (node as Map).each { Object key, Object value ->
                String keyName = String.valueOf(key)
                String lowered = keyName.toLowerCase(Locale.ROOT)
                String currentPath = path ? "${path}.${keyName}" : keyName
                if (FORBIDDEN_KEYS.contains(keyName) || FORBIDDEN_KEYS.contains(lowered)) {
                    findings << currentPath
                }
                findings.addAll(findForbiddenKeys(value, currentPath))
            }
        } else if (node instanceof List) {
            (node as List).eachWithIndex { Object child, int index ->
                findings.addAll(findForbiddenKeys(child, "${path}[${index}]"))
            }
        }
        return findings
    }

    private static void require(Object value, String message) {
        if (value == null || (value instanceof String && !value.trim())) {
            throw new IllegalArgumentException(message)
        }
    }
}
