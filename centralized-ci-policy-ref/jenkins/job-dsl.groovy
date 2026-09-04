def services = [
    [id: 'payment-api', path: 'services/payment-api'],
    [id: 'navigation-api', path: 'services/navigation-api'],
    [id: 'telemetry-processor', path: 'services/telemetry-processor'],
    [id: 'map-cache-service', path: 'services/map-cache-service'],
    [id: 'legacy-routing-service', path: 'services/legacy-routing-service']
]

services.each { svc ->
    pipelineJob("golden-ci/${svc.id}") {
        description("Centrally managed Golden CI for ${svc.id}. SERVICE_ID and SERVICE_PATH are injected here and are not service-owned.")
        parameters {
            stringParam('GIT_BRANCH', 'main', 'Branch to build')
        }
        definition {
            cpsScm {
                scm {
                    git {
                        remote {
                            url('https://github.com/Github-Arun-Repo/platform-engineering-reference-architectures.git')
                            credentials('github-read-credentials')
                        }
                        branch('$GIT_BRANCH')
                    }
                }
                scriptPath('centralized-ci-policy-ref/jenkins/Jenkinsfile')
            }
        }
        environmentVariables {
            env('SERVICE_ID', svc.id)
            env('SERVICE_PATH', svc.path)
        }
    }
}
