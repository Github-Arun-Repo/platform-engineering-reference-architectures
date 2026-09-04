package org.arun.ci

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

import java.security.MessageDigest

class EvidencePublisher {
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    Map buildEvidence(Map input) {
        return [
            service                      : input.service,
            servicePath                  : input.servicePath,
            serviceOwner                 : input.serviceOwner,
            securityProfile              : input.securityProfile,
            pipelineVersion              : input.pipelineVersion,
            policyVersion                : input.policyVersion,
            policyDigest                 : input.policyDigest,
            gitCommit                    : input.gitCommit,
            buildNumber                  : input.buildNumber,
            coverage                     : input.coverage,
            secretScanDecision           : input.secretScanDecision,
            unitTestDecision             : input.unitTestDecision,
            coverageDecision             : input.coverageDecision,
            scaDecision                  : input.scaDecision,
            sastDecision                 : input.sastDecision,
            containerDecision            : input.containerDecision,
            exceptionsApplied            : input.exceptionsApplied ?: [],
            unusedExceptions             : input.unusedExceptions ?: [],
            sbom                         : input.sbom,
            image                        : input.image,
            signatureResult              : input.signatureResult,
            attestationResult            : input.attestationResult,
            provenanceVerificationResult : input.provenanceVerificationResult
        ]
    }

    String toJson(Map evidence) {
        return mapper.writeValueAsString(evidence)
    }

    String buildHumanSummary(Map evidence) {
        return """
Service             : ${evidence.service}
Security Profile    : ${(evidence.securityProfile as String).toUpperCase(Locale.ROOT)}
Pipeline Version    : ${evidence.pipelineVersion}
Policy Version      : ${evidence.policyVersion}
Policy Digest       : ${evidence.policyDigest}
Coverage            : ${evidence.coverage}
Secret Scan         : ${evidence.secretScanDecision}
Unit Tests          : ${evidence.unitTestDecision}
Coverage Gate       : ${evidence.coverageDecision}
SCA Gate            : ${evidence.scaDecision}
SAST Gate           : ${evidence.sastDecision}
Container Gate      : ${evidence.containerDecision}
Signature           : ${evidence.signatureResult}
Attestation         : ${evidence.attestationResult}
Provenance Verify   : ${evidence.provenanceVerificationResult}
Image Digest        : ${evidence.image?.digest}
Applied Exceptions  : ${(evidence.exceptionsApplied as List).join(', ') ?: 'none'}
Unused Exceptions   : ${(evidence.unusedExceptions as List).join(', ') ?: 'none'}
""".trim()
    }

    String fileDigestSha256(String filePath) {
        File file = new File(filePath)
        if (!file.exists() || file.length() == 0L) {
            return null
        }
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        file.withInputStream { InputStream is ->
            byte[] buffer = new byte[8192]
            int read
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read)
            }
        }
        return 'sha256:' + digest.digest().collect { String.format('%02x', it) }.join('')
    }
}
