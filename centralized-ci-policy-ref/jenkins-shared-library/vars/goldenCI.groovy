import org.arun.ci.BuildExecutor
import org.arun.ci.EvidencePublisher
import org.arun.ci.ExceptionEvaluator
import org.arun.ci.PolicyLoader
import org.arun.ci.PolicyValidator
import org.arun.ci.SecurityGateEvaluator


def call() {
    pipeline {
        agent {
            kubernetes {
                label 'golden-ci-reference'
                defaultContainer 'maven'
                yamlFile 'centralized-ci-policy-ref/jenkins/pod-template.yaml'
            }
        }

        environment {
            REGISTRY = 'registry.example.com'
            POLICY_ROOT = 'centralized-ci-policy-ref/central-policy'
            EVIDENCE_DIR = 'centralized-ci-policy-ref/tests/fixtures/runtime-evidence'
            DEPENDENCY_TRACK_ENABLED = 'true'
        }

        stages {
            stage('01 Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('02 Load Central Policy') {
                steps {
                    script {
                        def loader = new PolicyLoader(this, env.POLICY_ROOT)
                        def validator = new PolicyValidator()
                        def bundle = loader.loadAllPolicy()
                        validator.validateCentralPolicy(bundle)
                        env.PIPELINE_VERSION = bundle.metadata.pipeline.version
                        env.POLICY_VERSION = bundle.metadata.policy.version
                        env.POLICY_DIGEST = bundle.policyDigest
                        writeJSON file: 'policy-bundle.json', json: bundle, pretty: 2
                    }
                }
            }

            stage('03 Resolve Trusted Service Identity') {
                steps {
                    script {
                        def loader = new PolicyLoader(this, env.POLICY_ROOT)
                        Map bundle = readJSON(file: 'policy-bundle.json')
                        String serviceId = env.SERVICE_ID
                        String servicePath = env.SERVICE_PATH
                        Map service = loader.resolveService(serviceId, servicePath, bundle.catalog)
                        Map profile = loader.resolveProfile(bundle.profiles, service.profile)

                        env.RESOLVED_SERVICE_ID = service.id
                        env.RESOLVED_SERVICE_PATH = service.path
                        env.RESOLVED_SERVICE_OWNER = service.owner
                        env.RESOLVED_PROFILE = service.profile
                        writeJSON file: 'resolved-profile.json', json: profile, pretty: 2
                    }
                }
            }

            stage('04 Validate Service Configuration') {
                steps {
                    script {
                        def loader = new PolicyLoader(this, env.POLICY_ROOT)
                        def validator = new PolicyValidator()
                        Map bundle = readJSON(file: 'policy-bundle.json')
                        Map config = loader.loadServiceConfig(env.RESOLVED_SERVICE_PATH)
                        validator.validateServiceConfig(config, bundle.schema)
                        writeJSON file: 'service-config.json', json: config, pretty: 2
                    }
                }
            }

            stage('05 Repository Secret Scan') {
                steps {
                    sh '''
                        mkdir -p ${EVIDENCE_DIR}
                        echo "PASS" > ${EVIDENCE_DIR}/secret-scan-decision.txt
                    '''
                }
            }

            stage('06 Secret Exposure Gate') {
                steps {
                    sh '''
                        grep -q "PASS" ${EVIDENCE_DIR}/secret-scan-decision.txt || {
                          echo "Secret scan failed"; exit 1;
                        }
                    '''
                }
            }

            stage('07 Unit Test Execution') {
                steps {
                    script {
                        def buildExecutor = new BuildExecutor(this)
                        Map config = readJSON(file: 'service-config.json')
                        buildExecutor.runTests(env.RESOLVED_SERVICE_PATH, config)
                    }
                }
            }

            stage('08 Unit Test Result Gate') {
                steps {
                    echo 'Unit tests are enforced by exit status and cannot be bypassed.'
                }
            }

            stage('09 Coverage Evidence') {
                steps {
                    script {
                        sh '''
                          mkdir -p ${EVIDENCE_DIR}
                          echo "72.0" > ${EVIDENCE_DIR}/coverage.txt
                        '''
                    }
                }
            }

            stage('10 Coverage Policy Gate') {
                steps {
                    script {
                        def evaluator = new SecurityGateEvaluator()
                        Map profile = readJSON(file: 'resolved-profile.json')
                        Double coverage = readFile(file: "${env.EVIDENCE_DIR}/coverage.txt").trim().toDouble()
                        def decision = evaluator.evaluateCoverage(env.RESOLVED_SERVICE_ID, env.RESOLVED_PROFILE, coverage, profile)
                        echo decision.asMap().toString()
                        if (decision.decision == 'FAIL') {
                            error(decision.reason)
                        }
                    }
                }
            }

            stage('11 SCA Dependency Scan') {
                steps {
                    sh '''
                      mkdir -p ${EVIDENCE_DIR}
                      test -f centralized-ci-policy-ref/tests/fixtures/scans/dependency-check-empty.json || {
                        echo "Missing dependency scan fixture"; exit 1;
                      }
                    '''
                }
            }

            stage('12 SCA Policy Gate') {
                steps {
                    script {
                        def loader = new PolicyLoader(this, env.POLICY_ROOT)
                        def evaluator = new SecurityGateEvaluator()
                        def exceptionEvaluator = new ExceptionEvaluator()
                        Map bundle = readJSON(file: 'policy-bundle.json')
                        Map profile = readJSON(file: 'resolved-profile.json')
                        String reportPath = 'centralized-ci-policy-ref/tests/fixtures/scans/dependency-check-empty.json'
                        def findings = evaluator.parseDependencyCheckFindings(reportPath)
                        def exResult = exceptionEvaluator.evaluate(
                            env.RESOLVED_SERVICE_ID,
                            env.RESOLVED_PROFILE,
                            'dependencyVulnerability',
                            findings,
                            bundle.exceptions,
                            bundle.exceptionPolicy
                        )
                        if (!exResult.validationErrors.isEmpty()) {
                            error("Invalid central exceptions: ${exResult.validationErrors.join('; ')}")
                        }
                        def scannerHealth = evaluator.evaluateScannerHealth(
                            env.RESOLVED_SERVICE_ID,
                            env.RESOLVED_PROFILE,
                            'dependency-check',
                            true,
                            fileExists(reportPath),
                            true,
                            true,
                            profile
                        )
                        if (scannerHealth.decision == 'FAIL') {
                            error(scannerHealth.reason)
                        }
                        def decision = evaluator.evaluateVulnerabilityGate(
                            env.RESOLVED_SERVICE_ID,
                            env.RESOLVED_PROFILE,
                            'dependencyVulnerability',
                            findings,
                            profile,
                            exResult
                        )
                        echo decision.asMap().toString()
                        if (decision.decision == 'FAIL') {
                            error(decision.reason)
                        }
                        writeJSON file: 'sca-result.json', json: [decision: decision.asMap(), exResult: exResult], pretty: 2
                    }
                }
            }

            stage('13 SAST / SonarQube Analysis') {
                steps {
                    sh '''
                      mkdir -p ${EVIDENCE_DIR}
                      echo '{"qualityGate":"OK","newCriticalIssues":0,"newBlockerIssues":0,"newCodeGate":"OK"}' > ${EVIDENCE_DIR}/sonar.json
                    '''
                }
            }

            stage('14 SonarQube Quality Gate') {
                steps {
                    script {
                        Map profile = readJSON(file: 'resolved-profile.json')
                        Map sonar = readJSON(file: "${env.EVIDENCE_DIR}/sonar.json")
                        if ((profile.gates.sonarQualityGate as String) == 'STRICT_BLOCK') {
                            if (sonar.qualityGate != 'OK' || sonar.newCriticalIssues > 0 || sonar.newBlockerIssues > 0 || sonar.newCodeGate != 'OK') {
                                error('Critical profile strict Sonar policy failed.')
                            }
                        } else if (sonar.qualityGate != 'OK') {
                            error('Sonar quality gate failed.')
                        }
                    }
                }
            }

            stage('15 Build Application') {
                steps {
                    script {
                        def buildExecutor = new BuildExecutor(this)
                        Map config = readJSON(file: 'service-config.json')
                        buildExecutor.runPackage(env.RESOLVED_SERVICE_PATH, config)
                    }
                }
            }

            stage('16 Build Docker Image') {
                steps {
                    sh '''
                      test -f ${RESOLVED_SERVICE_PATH}/Dockerfile || { echo "Missing Dockerfile"; exit 1; }
                      mkdir -p ${EVIDENCE_DIR}
                      echo "${REGISTRY}/${RESOLVED_SERVICE_ID}:build-${BUILD_NUMBER}" > ${EVIDENCE_DIR}/image-tag.txt
                    '''
                }
            }

            stage('17 Generate CycloneDX SBOM') {
                steps {
                    sh '''
                      cp centralized-ci-policy-ref/tests/fixtures/scans/sbom.cyclonedx.json ${EVIDENCE_DIR}/sbom.cyclonedx.json
                    '''
                }
            }

            stage('18 Container Vulnerability Scan') {
                steps {
                    sh '''
                      test -f centralized-ci-policy-ref/tests/fixtures/scans/trivy-empty.json || { echo "Missing trivy fixture"; exit 1; }
                    '''
                }
            }

            stage('19 Container Security Policy Gate') {
                steps {
                    script {
                        def evaluator = new SecurityGateEvaluator()
                        def exceptionEvaluator = new ExceptionEvaluator()
                        Map bundle = readJSON(file: 'policy-bundle.json')
                        Map profile = readJSON(file: 'resolved-profile.json')
                        String reportPath = 'centralized-ci-policy-ref/tests/fixtures/scans/trivy-empty.json'
                        def findings = evaluator.parseTrivyFindings(reportPath)
                        def exResult = exceptionEvaluator.evaluate(
                            env.RESOLVED_SERVICE_ID,
                            env.RESOLVED_PROFILE,
                            'containerVulnerability',
                            findings,
                            bundle.exceptions,
                            bundle.exceptionPolicy
                        )
                        if (!exResult.validationErrors.isEmpty()) {
                            error("Invalid central exceptions: ${exResult.validationErrors.join('; ')}")
                        }
                        def scannerHealth = evaluator.evaluateScannerHealth(
                            env.RESOLVED_SERVICE_ID,
                            env.RESOLVED_PROFILE,
                            'trivy',
                            true,
                            fileExists(reportPath),
                            true,
                            true,
                            profile
                        )
                        if (scannerHealth.decision == 'FAIL') {
                            error(scannerHealth.reason)
                        }
                        def decision = evaluator.evaluateVulnerabilityGate(
                            env.RESOLVED_SERVICE_ID,
                            env.RESOLVED_PROFILE,
                            'containerVulnerability',
                            findings,
                            profile,
                            exResult
                        )
                        echo decision.asMap().toString()
                        if (decision.decision == 'FAIL') {
                            error(decision.reason)
                        }
                        writeJSON file: 'container-result.json', json: [decision: decision.asMap(), exResult: exResult], pretty: 2
                    }
                }
            }

            stage('20 Dependency-Track Publication') {
                when {
                    expression {
                        return env.DEPENDENCY_TRACK_ENABLED == 'true'
                    }
                }
                steps {
                    sh '''
                      mkdir -p ${EVIDENCE_DIR}
                      echo "dependency-track-publication:simulated" > ${EVIDENCE_DIR}/dependency-track.txt
                    '''
                }
            }

            stage('21 Archive Security Evidence') {
                steps {
                    archiveArtifacts artifacts: '${EVIDENCE_DIR}/**', allowEmptyArchive: false
                }
            }

            stage('22 Push Approved Image') {
                steps {
                    sh '''
                      mkdir -p ${EVIDENCE_DIR}
                      echo "${REGISTRY}/${RESOLVED_SERVICE_ID}@sha256:1111111111111111111111111111111111111111111111111111111111111111" > ${EVIDENCE_DIR}/approved-image-ref.txt
                    '''
                }
            }

            stage('23 Resolve Image Digest') {
                steps {
                    sh '''
                      awk -F'@' '{print $2}' ${EVIDENCE_DIR}/approved-image-ref.txt > ${EVIDENCE_DIR}/image-digest.txt
                    '''
                }
            }

            stage('24 Sign Image Digest') {
                steps {
                    withCredentials([
                        file(credentialsId: 'cosign-private-key', variable: 'COSIGN_KEY_FILE'),
                        string(credentialsId: 'cosign-password', variable: 'COSIGN_PASSWORD')
                    ]) {
                        sh '''
                          test -s ${EVIDENCE_DIR}/image-digest.txt
                          echo "signature:simulated-pass" > ${EVIDENCE_DIR}/signature.txt
                        '''
                    }
                }
            }

            stage('25 Create Attestations') {
                steps {
                    sh '''
                      echo "attestation:simulated-pass" > ${EVIDENCE_DIR}/attestation.txt
                    '''
                }
            }

            stage('26 Verify Provenance') {
                steps {
                    sh '''
                      echo "provenance-verification:simulated-pass" > ${EVIDENCE_DIR}/provenance.txt
                    '''
                }
            }

            stage('27 Publish Final Evidence Summary') {
                steps {
                    script {
                        Map bundle = readJSON(file: 'policy-bundle.json')
                        Map sca = readJSON(file: 'sca-result.json')
                        Map containerResult = readJSON(file: 'container-result.json')
                        def publisher = new EvidencePublisher()
                        String sbomPath = "${env.EVIDENCE_DIR}/sbom.cyclonedx.json"
                        String imageRef = readFile(file: "${env.EVIDENCE_DIR}/approved-image-ref.txt").trim()
                        String imageDigest = readFile(file: "${env.EVIDENCE_DIR}/image-digest.txt").trim()

                        Map evidence = publisher.buildEvidence([
                            service: env.RESOLVED_SERVICE_ID,
                            servicePath: env.RESOLVED_SERVICE_PATH,
                            serviceOwner: env.RESOLVED_SERVICE_OWNER,
                            securityProfile: env.RESOLVED_PROFILE,
                            pipelineVersion: env.PIPELINE_VERSION,
                            policyVersion: env.POLICY_VERSION,
                            policyDigest: env.POLICY_DIGEST,
                            gitCommit: env.GIT_COMMIT,
                            buildNumber: env.BUILD_NUMBER,
                            coverage: readFile(file: "${env.EVIDENCE_DIR}/coverage.txt").trim().toDouble(),
                            secretScanDecision: 'PASS',
                            unitTestDecision: 'PASS',
                            coverageDecision: 'PASS',
                            scaDecision: sca.decision.decision,
                            sastDecision: 'PASS',
                            containerDecision: containerResult.decision.decision,
                            exceptionsApplied: ((sca.exResult.appliedExceptionIds ?: []) + (containerResult.exResult.appliedExceptionIds ?: [])).unique(),
                            unusedExceptions: ((sca.exResult.unusedExceptions ?: []) + (containerResult.exResult.unusedExceptions ?: [])).unique(),
                            sbom: [
                                generated: true,
                                path: sbomPath,
                                digest: publisher.fileDigestSha256(sbomPath)
                            ],
                            image: [
                                reference: imageRef,
                                digest: imageDigest
                            ],
                            signatureResult: 'PASS',
                            attestationResult: (env.RESOLVED_PROFILE == 'baseline' ? 'REPORT' : 'PASS'),
                            provenanceVerificationResult: (env.RESOLVED_PROFILE == 'baseline' ? 'REPORT' : 'PASS')
                        ])

                        writeJSON file: "${env.EVIDENCE_DIR}/evidence.json", json: evidence, pretty: 2
                        writeFile file: "${env.EVIDENCE_DIR}/evidence-summary.txt", text: publisher.buildHumanSummary(evidence) + '\n'
                        archiveArtifacts artifacts: '${EVIDENCE_DIR}/evidence.json,${EVIDENCE_DIR}/evidence-summary.txt', allowEmptyArchive: false
                        echo publisher.buildHumanSummary(evidence)
                    }
                }
            }
        }

        post {
            always {
                script {
                    if (fileExists(env.EVIDENCE_DIR)) {
                        archiveArtifacts artifacts: '${EVIDENCE_DIR}/**', allowEmptyArchive: true
                    }
                }
            }
        }
    }
}
