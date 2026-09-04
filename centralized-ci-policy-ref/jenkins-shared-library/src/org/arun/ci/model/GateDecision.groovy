package org.arun.ci.model

class GateDecision {
    String service
    String profile
    String gate
    int criticalFindings = 0
    int highFindings = 0
    List<String> appliedExceptions = []
    List<String> warnings = []
    List<String> blockingFindings = []
    String decision
    String reason

    Map<String, Object> asMap() {
        return [
            service          : service,
            profile          : profile,
            gate             : gate,
            criticalFindings : criticalFindings,
            highFindings     : highFindings,
            appliedExceptions: appliedExceptions,
            warnings         : warnings,
            blockingFindings : blockingFindings,
            decision         : decision,
            reason           : reason
        ]
    }
}
