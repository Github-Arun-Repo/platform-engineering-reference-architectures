package org.arun.ci

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.arun.ci.model.GateDecision
import org.arun.ci.model.SecurityFinding

class SecurityGateEvaluator {
    private static final ObjectMapper MAPPER = new ObjectMapper()

    GateDecision evaluateCoverage(String service, String profileName, double coverage, Map profile) {
        int min = (profile.coverage.minimumLinePercent as Number).intValue()
        GateDecision decision = baseDecision(service, profileName, 'coverage')
        if (coverage >= min) {
            decision.decision = 'PASS'
            decision.reason = "Coverage ${coverage}% meets minimum ${min}%"
        } else {
            decision.decision = 'FAIL'
            decision.reason = "Coverage ${coverage}% below minimum ${min}%"
            decision.blockingFindings << "coverage:${coverage}<${min}"
        }
        return decision
    }

    GateDecision evaluateVulnerabilityGate(
        String service,
        String profileName,
        String gate,
        List<SecurityFinding> findings,
        Map profile,
        Map exceptionResult
    ) {
        GateDecision decision = baseDecision(service, profileName, gate)
        List<SecurityFinding> effective = findings.findAll {
            !((exceptionResult.exceptedFindings as Set<String>).contains(it.normalizedId()))
        }

        int critical = effective.count { isSeverity(it, 'CRITICAL') }
        int high = effective.count { isSeverity(it, 'HIGH') }
        decision.criticalFindings = critical
        decision.highFindings = high
        decision.appliedExceptions = (exceptionResult.appliedExceptionIds ?: []) as List<String>

        Map gatePolicy = (Map) profile.gates[gate]
        String criticalRule = gatePolicy.critical as String
        String highRule = gatePolicy.high as String

        List<String> warnings = []
        List<String> blockers = []

        if (critical > 0 && criticalRule == 'BLOCK') {
            blockers << "${critical} CRITICAL findings"
        }
        if (high > 0 && highRule == 'BLOCK') {
            blockers << "${high} HIGH findings"
        }
        if (high > 0 && highRule == 'WARN') {
            warnings << "${high} HIGH findings"
        }

        if (!blockers.isEmpty()) {
            decision.decision = 'FAIL'
            decision.reason = "${profileName} profile blocks ${blockers.join(' and ')}"
            decision.blockingFindings.addAll(blockers)
        } else if (!decision.appliedExceptions.isEmpty()) {
            decision.decision = warnings.isEmpty() ? 'EXCEPTION_APPLIED' : 'WARN'
            decision.reason = warnings.isEmpty() ? 'Approved finding-specific exception applied' : warnings.join(', ')
            decision.warnings.addAll(warnings)
        } else if (!warnings.isEmpty()) {
            decision.decision = 'WARN'
            decision.reason = warnings.join(', ')
            decision.warnings.addAll(warnings)
        } else {
            decision.decision = 'PASS'
            decision.reason = 'No blocking findings'
        }

        return decision
    }

    GateDecision evaluateScannerHealth(
        String service,
        String profileName,
        String scanner,
        boolean executionOk,
        boolean reportPresent,
        boolean reportParseable,
        boolean reportHasTrustedStructure,
        Map profile
    ) {
        GateDecision decision = baseDecision(service, profileName, 'scannerExecutionFailure')
        String scannerPolicy = profile.gates.scannerExecutionFailure as String
        String missingPolicy = profile.gates.missingSecurityReport as String
        String malformedPolicy = profile.gates.malformedSecurityReport as String

        List<String> failures = []
        if (!executionOk) {
            failures << "${scanner} execution failed"
        }
        if (!reportPresent) {
            failures << "${scanner} report missing"
        }
        if (!reportParseable || !reportHasTrustedStructure) {
            failures << "${scanner} report malformed or untrusted"
        }

        if (!failures.isEmpty()) {
            if (scannerPolicy == 'BLOCK' || missingPolicy == 'BLOCK' || malformedPolicy == 'BLOCK') {
                decision.decision = 'FAIL'
                decision.reason = failures.join('; ')
                decision.blockingFindings.addAll(failures)
            } else {
                decision.decision = 'WARN'
                decision.reason = failures.join('; ')
                decision.warnings.addAll(failures)
            }
        } else {
            decision.decision = 'PASS'
            decision.reason = "${scanner} execution and report integrity passed"
        }

        return decision
    }

    List<SecurityFinding> parseDependencyCheckFindings(String reportPath) {
        JsonNode root = parseJsonReport(reportPath)
        List<SecurityFinding> findings = []
        root.path('dependencies').forEach { dep ->
            String component = dep.path('fileName').asText('unknown')
            dep.path('vulnerabilities').forEach { vuln ->
                findings << new SecurityFinding(
                    id: vuln.path('name').asText('UNKNOWN'),
                    source: 'dependency-check',
                    gate: 'dependencyVulnerability',
                    component: component,
                    installedVersion: dep.path('version').asText('unknown'),
                    fixedVersion: vuln.path('fixedInVersion').asText('unknown'),
                    severity: vuln.path('severity').asText('UNKNOWN').toUpperCase(Locale.ROOT),
                    description: vuln.path('description').asText(''),
                    reportLocation: reportPath
                )
            }
        }
        return findings
    }

    List<SecurityFinding> parseTrivyFindings(String reportPath) {
        JsonNode root = parseJsonReport(reportPath)
        List<SecurityFinding> findings = []
        root.path('Results').forEach { result ->
            String component = result.path('Target').asText('unknown')
            result.path('Vulnerabilities').forEach { vuln ->
                findings << new SecurityFinding(
                    id: vuln.path('VulnerabilityID').asText('UNKNOWN'),
                    source: 'trivy',
                    gate: 'containerVulnerability',
                    component: component,
                    installedVersion: vuln.path('InstalledVersion').asText('unknown'),
                    fixedVersion: vuln.path('FixedVersion').asText('unknown'),
                    severity: vuln.path('Severity').asText('UNKNOWN').toUpperCase(Locale.ROOT),
                    description: vuln.path('Title').asText(vuln.path('Description').asText('')),
                    reportLocation: reportPath
                )
            }
        }
        return findings
    }

    private JsonNode parseJsonReport(String reportPath) {
        File report = new File(reportPath)
        if (!report.exists()) {
            throw new IllegalStateException("Missing report: ${reportPath}")
        }
        if (report.length() == 0L) {
            throw new IllegalStateException("Empty report: ${reportPath}")
        }
        try {
            return MAPPER.readTree(report)
        } catch (Exception ex) {
            throw new IllegalStateException("Malformed report ${reportPath}: ${ex.message}", ex)
        }
    }

    private static GateDecision baseDecision(String service, String profile, String gate) {
        return new GateDecision(service: service, profile: profile, gate: gate)
    }

    private static boolean isSeverity(SecurityFinding finding, String severity) {
        return (finding.severity ?: '').equalsIgnoreCase(severity)
    }
}
