package org.arun.ci

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.yaml.snakeyaml.Yaml

import java.security.MessageDigest

class PolicyLoader {
    private final def steps
    private final String rootDir

    PolicyLoader(def steps, String rootDir = 'centralized-ci-policy-ref/central-policy') {
        this.steps = steps
        this.rootDir = rootDir
    }

    Map loadAllPolicy() {
        Map metadata = loadYamlFile('policy-metadata.yaml')
        Map catalog = loadYamlFile('service-catalog.yaml')
        Map profiles = loadYamlFile('security-profiles.yaml')
        Map exceptions = loadYamlFile('exceptions.yaml')
        Map exceptionPolicy = loadYamlFile('exception-policy.yaml')
        ObjectNode schema = loadJsonFile('service-config-schema.json')

        return [
            metadata: metadata,
            catalog: catalog,
            profiles: profiles,
            exceptions: exceptions,
            exceptionPolicy: exceptionPolicy,
            schema: schema,
            policyDigest: calculatePolicyDigest([
                'policy-metadata.yaml',
                'service-catalog.yaml',
                'security-profiles.yaml',
                'exceptions.yaml',
                'exception-policy.yaml'
            ])
        ]
    }

    Map resolveService(String serviceId, String servicePath, Map catalog) {
        if (!serviceId?.trim()) {
            throw new IllegalArgumentException('Missing SERVICE_ID. Central job configuration must inject SERVICE_ID.')
        }
        if (!servicePath?.trim()) {
            throw new IllegalArgumentException('Missing SERVICE_PATH. Central job configuration must inject SERVICE_PATH.')
        }

        Map services = (Map) catalog.services
        Map service = (Map) services[serviceId]
        if (!service) {
            throw new IllegalArgumentException("Unknown SERVICE_ID: ${serviceId}. Rejecting untrusted service identity.")
        }

        String expectedPath = service.path as String
        if (expectedPath != servicePath) {
            throw new IllegalArgumentException("SERVICE_PATH mismatch for ${serviceId}. Expected '${expectedPath}', got '${servicePath}'.")
        }

        return [
            id: serviceId,
            path: expectedPath,
            owner: service.owner as String,
            profile: service.profile as String
        ]
    }

    Map resolveProfile(Map profiles, String profileName) {
        Map profile = (Map) profiles.profiles[profileName]
        if (!profile) {
            throw new IllegalArgumentException("Unknown profile '${profileName}' configured in central service catalog.")
        }
        return profile
    }

    Map loadServiceConfig(String servicePath) {
        String configPath = "${servicePath}/ci.yaml"
        if (!fileExists(configPath)) {
            throw new IllegalArgumentException("Missing service configuration file: ${configPath}")
        }
        return loadYamlAbsolute(configPath)
    }

    Map loadYamlFile(String policyFileName) {
        String filePath = "${rootDir}/${policyFileName}"
        if (!fileExists(filePath)) {
            throw new IllegalStateException("Missing central policy file: ${filePath}")
        }
        return loadYamlAbsolute(filePath)
    }

    ObjectNode loadJsonFile(String policyFileName) {
        String filePath = "${rootDir}/${policyFileName}"
        if (!fileExists(filePath)) {
            throw new IllegalStateException("Missing central policy file: ${filePath}")
        }
        String raw = readFile(filePath)
        try {
            ObjectMapper mapper = new ObjectMapper()
            return (ObjectNode) mapper.readTree(raw)
        } catch (Exception ex) {
            throw new IllegalStateException("Malformed JSON in ${filePath}: ${ex.message}", ex)
        }
    }

    String calculatePolicyDigest(List<String> policyFiles) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        List<String> sorted = policyFiles.sort()
        sorted.each { fileName ->
            String path = "${rootDir}/${fileName}"
            if (!fileExists(path)) {
                throw new IllegalStateException("Missing policy input for digest: ${path}")
            }
            byte[] data = readFile(path).getBytes('UTF-8')
            digest.update(path.getBytes('UTF-8'))
            digest.update((byte) 0)
            digest.update(data)
            digest.update((byte) 0)
        }
        return 'sha256:' + digest.digest().collect { String.format('%02x', it) }.join('')
    }

    private Map loadYamlAbsolute(String path) {
        try {
            if (steps?.metaClass?.respondsTo(steps, 'readYaml', Map)) {
                Object value = steps.readYaml(file: path)
                return (Map) value
            }
            return (Map) new Yaml().load(readFile(path))
        } catch (Exception ex) {
            throw new IllegalStateException("Malformed YAML in ${path}: ${ex.message}", ex)
        }
    }

    private boolean fileExists(String path) {
        if (steps?.metaClass?.respondsTo(steps, 'fileExists', String)) {
            return steps.fileExists(path)
        }
        return new File(path).exists()
    }

    private String readFile(String path) {
        if (steps?.metaClass?.respondsTo(steps, 'readFile', String)) {
            return steps.readFile(path)
        }
        return new File(path).getText('UTF-8')
    }
}
