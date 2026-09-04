package org.arun.ci

import com.fasterxml.jackson.databind.ObjectMapper
import org.arun.ci.model.SecurityFinding
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

import java.nio.file.Files
import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.*

class PolicyEngineTest {

    private static final String POLICY_ROOT = '../../central-policy'

    @Test
    void scenario1_criticalCoverageFailure() {
        Map profile = loadProfiles().profiles.critical as Map
        def decision = new SecurityGateEvaluator().evaluateCoverage('payment-api', 'critical', 72.0d, profile)
        assertEquals('FAIL', decision.decision)
    }

    @Test
    void scenario2_standardCoveragePass() {
        Map profile = loadProfiles().profiles.standard as Map
        def decision = new SecurityGateEvaluator().evaluateCoverage('navigation-api', 'standard', 72.0d, profile)
        assertEquals('PASS', decision.decision)
    }

    @Test
    void scenario3_baselineHighCveWarn() {
        Map profile = loadProfiles().profiles.baseline as Map
        def evaluator = new SecurityGateEvaluator()
        def findings = evaluator.parseDependencyCheckFindings('../fixtures/scans/dependency-check-high.json')
        def exResult = [exceptedFindings: [] as Set<String>, appliedExceptionIds: []]
        def decision = evaluator.evaluateVulnerabilityGate('legacy-routing-service', 'baseline', 'dependencyVulnerability', findings, profile, exResult)
        assertEquals('WARN', decision.decision)
    }

    @Test
    void scenario4_criticalServiceCriticalCveFail() {
        Map profile = loadProfiles().profiles.critical as Map
        def evaluator = new SecurityGateEvaluator()
        def findings = evaluator.parseDependencyCheckFindings('../fixtures/scans/dependency-check-critical.json')
        def exResult = [exceptedFindings: [] as Set<String>, appliedExceptionIds: []]
        def decision = evaluator.evaluateVulnerabilityGate('payment-api', 'critical', 'dependencyVulnerability', findings, profile, exResult)
        assertEquals('FAIL', decision.decision)
    }

    @Test
    void scenario5_exactApprovedExceptionOnlySuppressesExactFinding() {
        def exceptionEvaluator = new ExceptionEvaluator()
        def findings = [
            new SecurityFinding(id: 'CVE-A', severity: 'HIGH', gate: 'containerVulnerability'),
            new SecurityFinding(id: 'CVE-B', severity: 'HIGH', gate: 'containerVulnerability')
        ]
        Map exceptions = [
            exceptions: [[
                id: 'SEC-EX-001',
                service: 'legacy-routing-service',
                gate: 'containerVulnerability',
                finding: 'CVE-A',
                reason: 'Vendor patch unavailable',
                ticket: 'SEC-1234',
                owner: 'legacy-team',
                approval: [approvedBy: ['security-team'], approvedAt: '2099-01-01'],
                expiresAt: '2099-01-20'
            ]]
        ]

        Map exPolicy = loadExceptionPolicy()
        def exResult = exceptionEvaluator.evaluate('legacy-routing-service', 'baseline', 'containerVulnerability', findings, exceptions, exPolicy)
        assertTrue(exResult.appliedExceptionIds.contains('SEC-EX-001'))
        assertTrue((exResult.exceptedFindings as Set<String>).contains('CVE-A'))
        assertFalse((exResult.exceptedFindings as Set<String>).contains('CVE-B'))
    }

    @Test
    void scenario6_expiredExceptionIsInvalid() {
        def exceptionEvaluator = new ExceptionEvaluator()
        Map exPolicy = loadExceptionPolicy()
        List<Map> exceptions = [[
            id: 'SEC-EX-002', service: 'payment-api', gate: 'dependencyVulnerability', finding: 'CVE-X',
            reason: 'No patch', ticket: 'SEC-2', owner: 'payment-team',
            approval: [approvedBy: ['security-team'], approvedAt: '2020-01-01'], expiresAt: '2020-01-10'
        ]]
        def errors = exceptionEvaluator.validateExceptions(exceptions, 'critical', exPolicy.exceptionPolicy as Map)
        assertTrue(errors.any { it.contains('expired') })
    }

    @Test
    void scenario7_forbiddenSecurityConfigurationRejected() {
        def validator = new PolicyValidator()
        def schema = new ObjectMapper().readTree(new File("${POLICY_ROOT}/service-config-schema.json"))
        Map config = [
            build: [tool: 'maven', javaVersion: 21, projectPath: '.', testCommand: 'mvn test', packageCommand: 'mvn package -DskipTests'],
            container: [dockerfile: 'Dockerfile', context: '.'],
            security: [trivy: false]
        ]
        def ex = assertThrows(IllegalArgumentException.class) { validator.validateServiceConfig(config, schema) }
        assertTrue(ex.message.contains('Forbidden centrally controlled field'))
    }

    @Test
    void scenario8_developerSelectedProfileRejected() {
        def validator = new PolicyValidator()
        def schema = new ObjectMapper().readTree(new File("${POLICY_ROOT}/service-config-schema.json"))
        Map config = [
            build: [tool: 'maven', javaVersion: 21, projectPath: '.', testCommand: 'mvn test', packageCommand: 'mvn package -DskipTests'],
            container: [dockerfile: 'Dockerfile', context: '.'],
            profile: 'baseline'
        ]
        assertThrows(IllegalArgumentException.class) { validator.validateServiceConfig(config, schema) }
    }

    @Test
    void scenario9_scannerCrashFailsClosed() {
        Map profile = loadProfiles().profiles.standard as Map
        def decision = new SecurityGateEvaluator().evaluateScannerHealth('navigation-api', 'standard', 'trivy', false, true, true, true, profile)
        assertEquals('FAIL', decision.decision)
    }

    @Test
    void scenario10_scannerReportMissingFailsClosed() {
        Map profile = loadProfiles().profiles.standard as Map
        def decision = new SecurityGateEvaluator().evaluateScannerHealth('navigation-api', 'standard', 'trivy', true, false, true, true, profile)
        assertEquals('FAIL', decision.decision)
    }

    @Test
    void scenario11_scannerReportMalformedFailsClosed() {
        Map profile = loadProfiles().profiles.standard as Map
        def decision = new SecurityGateEvaluator().evaluateScannerHealth('navigation-api', 'standard', 'trivy', true, true, false, false, profile)
        assertEquals('FAIL', decision.decision)
    }

    @Test
    void scenario12_unknownServiceRejected() {
        def loader = new PolicyLoader(null, POLICY_ROOT)
        Map catalog = loadCatalog()
        assertThrows(IllegalArgumentException.class) { loader.resolveService('unknown-service', 'services/unknown-service', catalog) }
    }

    @Test
    void scenario13_missingServiceIdentityRejected() {
        def loader = new PolicyLoader(null, POLICY_ROOT)
        Map catalog = loadCatalog()
        assertThrows(IllegalArgumentException.class) { loader.resolveService('', 'services/payment-api', catalog) }
    }

    @Test
    void scenario14_servicePathMismatchRejected() {
        def loader = new PolicyLoader(null, POLICY_ROOT)
        Map catalog = loadCatalog()
        assertThrows(IllegalArgumentException.class) { loader.resolveService('payment-api', 'services/legacy-routing-service', catalog) }
    }

    @Test
    void scenario15_wrongServiceExceptionNotApplied() {
        def exceptionEvaluator = new ExceptionEvaluator()
        def findings = [new SecurityFinding(id: 'CVE-A', severity: 'HIGH', gate: 'dependencyVulnerability')]
        Map exceptions = [exceptions: [[
            id: 'SEC-EX-100', service: 'legacy-routing-service', gate: 'dependencyVulnerability', finding: 'CVE-A',
            reason: 'Reason', ticket: 'SEC-100', owner: 'legacy-team',
            approval: [approvedBy: ['security-team'], approvedAt: '2099-01-01'], expiresAt: '2099-01-20'
        ]]]
        Map exResult = exceptionEvaluator.evaluate('payment-api', 'critical', 'dependencyVulnerability', findings, exceptions, loadExceptionPolicy())
        assertTrue((exResult.appliedExceptionIds as List).isEmpty())
    }

    @Test
    void scenario16_exceptionForForbiddenGateRejected() {
        def exceptionEvaluator = new ExceptionEvaluator()
        List<Map> exceptions = [[
            id: 'SEC-EX-200', service: 'payment-api', gate: 'secretExposure', finding: 'SECRET-1',
            reason: 'Bad', ticket: 'SEC-200', owner: 'payment-team',
            approval: [approvedBy: ['security-team'], approvedAt: '2099-01-01'], expiresAt: '2099-01-20'
        ]]
        def errors = exceptionEvaluator.validateExceptions(exceptions, 'critical', loadExceptionPolicy().exceptionPolicy as Map)
        assertTrue(errors.any { it.contains('forbidden gate') })
    }

    @Test
    void scenario17_wildcardExceptionRejected() {
        def exceptionEvaluator = new ExceptionEvaluator()
        List<Map> exceptions = [[
            id: 'SEC-EX-201', service: 'payment-api', gate: 'dependencyVulnerability', finding: '*',
            reason: 'Bad', ticket: 'SEC-201', owner: 'payment-team',
            approval: [approvedBy: ['security-team'], approvedAt: '2099-01-01'], expiresAt: '2099-01-20'
        ]]
        def errors = exceptionEvaluator.validateExceptions(exceptions, 'critical', loadExceptionPolicy().exceptionPolicy as Map)
        assertTrue(errors.any { it.contains('wildcard') })
    }

    @Test
    void scenario18_exceptionExceedsTtlRejected() {
        def exceptionEvaluator = new ExceptionEvaluator()
        List<Map> exceptions = [[
            id: 'SEC-EX-202', service: 'payment-api', gate: 'dependencyVulnerability', finding: 'CVE-TTL',
            reason: 'Bad', ticket: 'SEC-202', owner: 'payment-team',
            approval: [approvedBy: ['security-team'], approvedAt: '2026-09-01'], expiresAt: '2026-11-01'
        ]]
        def errors = exceptionEvaluator.validateExceptions(exceptions, 'critical', loadExceptionPolicy().exceptionPolicy as Map)
        assertTrue(errors.any { it.contains('exceeds max TTL') })
    }

    @Test
    void scenario19_invalidDateOrderRejected() {
        def exceptionEvaluator = new ExceptionEvaluator()
        List<Map> exceptions = [[
            id: 'SEC-EX-203', service: 'payment-api', gate: 'dependencyVulnerability', finding: 'CVE-DATE',
            reason: 'Bad', ticket: 'SEC-203', owner: 'payment-team',
            approval: [approvedBy: ['security-team'], approvedAt: '2026-10-01'], expiresAt: '2026-09-01'
        ]]
        def errors = exceptionEvaluator.validateExceptions(exceptions, 'critical', loadExceptionPolicy().exceptionPolicy as Map)
        assertTrue(errors.any { it.contains('expiresAt after approvedAt') })
    }

    @Test
    void scenario20_unusedExceptionReported() {
        def exceptionEvaluator = new ExceptionEvaluator()
        def findings = [new SecurityFinding(id: 'CVE-B', severity: 'HIGH', gate: 'containerVulnerability')]
        Map exceptions = [exceptions: [[
            id: 'SEC-EX-300', service: 'legacy-routing-service', gate: 'containerVulnerability', finding: 'CVE-A',
            reason: 'No patch', ticket: 'SEC-300', owner: 'legacy-team',
            approval: [approvedBy: ['security-team'], approvedAt: '2099-01-01'], expiresAt: '2099-01-20'
        ]]]

        Map exResult = exceptionEvaluator.evaluate('legacy-routing-service', 'baseline', 'containerVulnerability', findings, exceptions, loadExceptionPolicy())
        assertTrue((exResult.unusedExceptions as List).contains('SEC-EX-300'))
        assertTrue((exResult.appliedExceptionIds as List).isEmpty())
    }

    @Test
    void scenario21_unknownProfileInCatalogFailsValidation() {
        def validator = new PolicyValidator()
        Map catalog = [services: ['svc-a': [path: 'services/svc-a', owner: 'x', profile: 'super-critical']]]
        Map profiles = [profiles: [baseline: [:], standard: [:], critical: [:]]]
        assertThrows(IllegalArgumentException.class) { validator.validateKnownProfiles(catalog, profiles) }
    }

    @Test
    void scenario22_nestedForbiddenFieldRejected() {
        def validator = new PolicyValidator()
        def schema = new ObjectMapper().readTree(new File("${POLICY_ROOT}/service-config-schema.json"))
        Map config = [
            build: [tool: 'maven', javaVersion: 21, projectPath: '.', testCommand: 'mvn test', packageCommand: 'mvn package -DskipTests'],
            container: [dockerfile: 'Dockerfile', context: '.'],
            metadata: [custom: [security: [trivy: false]]]
        ]
        def ex = assertThrows(IllegalArgumentException.class) { validator.validateServiceConfig(config, schema) }
        assertTrue(ex.message.contains('Forbidden centrally controlled field'))
    }

    @Test
    void scenario23_duplicateExceptionIdRejected() {
        def exceptionEvaluator = new ExceptionEvaluator()
        List<Map> exceptions = [
            [id: 'SEC-EX-400', service: 'payment-api', gate: 'dependencyVulnerability', finding: 'CVE-1', reason: 'r', ticket: 't', owner: 'o', approval: [approvedBy: ['security-team'], approvedAt: '2099-01-01'], expiresAt: '2099-01-20'],
            [id: 'SEC-EX-400', service: 'payment-api', gate: 'dependencyVulnerability', finding: 'CVE-2', reason: 'r', ticket: 't2', owner: 'o', approval: [approvedBy: ['security-team'], approvedAt: '2099-01-01'], expiresAt: '2099-01-20']
        ]
        def errors = exceptionEvaluator.validateExceptions(exceptions, 'critical', loadExceptionPolicy().exceptionPolicy as Map)
        assertTrue(errors.any { it.contains('Duplicate exception id') })
    }

    @Test
    void scenario24_missingSecurityApprovalRejected() {
        def exceptionEvaluator = new ExceptionEvaluator()
        List<Map> exceptions = [[
            id: 'SEC-EX-401', service: 'payment-api', gate: 'dependencyVulnerability', finding: 'CVE-1',
            reason: 'r', ticket: 't', owner: 'o', approval: [:], expiresAt: '2099-01-20'
        ]]
        def errors = exceptionEvaluator.validateExceptions(exceptions, 'critical', loadExceptionPolicy().exceptionPolicy as Map)
        assertTrue(errors.any { it.contains('missing security approval') })
    }

    @Test
    void scenario25_missingPolicyFileFailsClosed() {
        Path tmp = Files.createTempDirectory('policy-missing')
        def loader = new PolicyLoader(null, tmp.resolve('central-policy').toString())
        assertThrows(IllegalStateException.class) { loader.loadAllPolicy() }
    }

    private static Map loadYaml(String path) {
        return (Map) new Yaml().load(new File(path).getText('UTF-8'))
    }

    private static Map loadProfiles() {
        return loadYaml("${POLICY_ROOT}/security-profiles.yaml")
    }

    private static Map loadCatalog() {
        return loadYaml("${POLICY_ROOT}/service-catalog.yaml")
    }

    private static Map loadExceptionPolicy() {
        return loadYaml("${POLICY_ROOT}/exception-policy.yaml")
    }
}
