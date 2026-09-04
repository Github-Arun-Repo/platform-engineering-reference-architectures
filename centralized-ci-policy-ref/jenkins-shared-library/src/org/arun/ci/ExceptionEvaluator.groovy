package org.arun.ci

import org.arun.ci.model.SecurityFinding

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ExceptionEvaluator {

    Map evaluate(
        String serviceId,
        String profile,
        String gate,
        List<SecurityFinding> findings,
        Map exceptionsWrapper,
        Map exceptionPolicyWrapper
    ) {
        Map exceptionPolicy = (Map) exceptionPolicyWrapper.exceptionPolicy
        List<Map> exceptions = (List<Map>) (exceptionsWrapper.exceptions ?: [])

        List<String> validationErrors = validateExceptions(exceptions, profile, exceptionPolicy)
        List<Map> matchingExceptions = exceptions.findAll { Map ex ->
            ex.service == serviceId && ex.gate == gate
        }

        Set<String> findingIds = findings.collect { it.normalizedId() } as Set<String>
        List<String> appliedExceptionIds = []
        Set<String> exceptedFindings = [] as Set<String>
        List<String> unusedExceptions = []

        matchingExceptions.each { Map ex ->
            String finding = normalize(ex.finding as String)
            if (findingIds.contains(finding)) {
                appliedExceptionIds << (ex.id as String)
                exceptedFindings << finding
            } else {
                unusedExceptions << (ex.id as String)
            }
        }

        return [
            appliedExceptionIds: appliedExceptionIds,
            exceptedFindings: exceptedFindings,
            unusedExceptions: unusedExceptions,
            validationErrors: validationErrors
        ]
    }

    List<String> validateExceptions(List<Map> exceptions, String profile, Map exceptionPolicy) {
        List<String> errors = []
        Set<String> ids = [] as Set<String>
        Set<String> allowed = (exceptionPolicy.allowed ?: []) as Set<String>
        Set<String> forbidden = (exceptionPolicy.forbidden ?: []) as Set<String>
        Integer maxTtl = (exceptionPolicy.maxTtlDays ?: [:])[profile] as Integer

        exceptions.each { Map ex ->
            String id = ex.id as String
            if (!id) {
                errors << 'Exception missing id'
            } else if (!ids.add(id)) {
                errors << "Duplicate exception id: ${id}"
            }

            ['service', 'gate', 'finding', 'reason', 'ticket', 'owner'].each { String key ->
                if (!(ex[key] as String)?.trim()) {
                    errors << "Exception ${id ?: '<unknown>'} missing ${key}"
                }
            }

            if (containsWildcard(ex.service as String) || containsWildcard(ex.gate as String) || containsWildcard(ex.finding as String)) {
                errors << "Exception ${id ?: '<unknown>'} cannot use wildcard service/gate/finding"
            }

            String gate = ex.gate as String
            if (forbidden.contains(gate)) {
                errors << "Exception ${id ?: '<unknown>'} targets forbidden gate ${gate}"
            }
            if (!allowed.contains(gate)) {
                errors << "Exception ${id ?: '<unknown>'} targets non-allowlisted gate ${gate}"
            }

            Map approval = (Map) ex.approval
            if (!(approval?.approvedBy instanceof List) || (approval.approvedBy as List).isEmpty()) {
                errors << "Exception ${id ?: '<unknown>'} missing security approval.approvedBy"
            }
            if (!(approval?.approvedAt as String)?.trim()) {
                errors << "Exception ${id ?: '<unknown>'} missing approval.approvedAt"
            }
            if (!(ex.expiresAt as String)?.trim()) {
                errors << "Exception ${id ?: '<unknown>'} missing expiresAt"
            }

            if ((approval?.approvedAt as String) && (ex.expiresAt as String)) {
                LocalDate approvedAt = LocalDate.parse(approval.approvedAt as String)
                LocalDate expiresAt = LocalDate.parse(ex.expiresAt as String)
                if (!expiresAt.isAfter(approvedAt)) {
                    errors << "Exception ${id ?: '<unknown>'} must have expiresAt after approvedAt"
                }
                long ttlDays = ChronoUnit.DAYS.between(approvedAt, expiresAt)
                if (maxTtl != null && ttlDays > maxTtl) {
                    errors << "Exception ${id ?: '<unknown>'} exceeds max TTL for ${profile}: ${ttlDays} > ${maxTtl}"
                }
                if (expiresAt.isBefore(LocalDate.now())) {
                    errors << "Exception ${id ?: '<unknown>'} is expired"
                }
            }
        }

        return errors
    }

    private static boolean containsWildcard(String value) {
        return value?.contains('*')
    }

    private static String normalize(String value) {
        return (value ?: '').trim().toUpperCase(Locale.ROOT)
    }
}
