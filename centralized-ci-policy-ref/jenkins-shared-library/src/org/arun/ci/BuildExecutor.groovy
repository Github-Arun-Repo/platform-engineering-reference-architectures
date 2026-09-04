package org.arun.ci

class BuildExecutor {
    private final def steps

    BuildExecutor(def steps) {
        this.steps = steps
    }

    void runTests(String servicePath, Map serviceConfig) {
        String tool = serviceConfig.build.tool as String
        ensureSupportedTool(tool)
        String command = serviceConfig.build.testCommand as String
        executeInServicePath(servicePath, command, 'unit tests')
    }

    void runPackage(String servicePath, Map serviceConfig) {
        String tool = serviceConfig.build.tool as String
        ensureSupportedTool(tool)
        String command = serviceConfig.build.packageCommand as String
        executeInServicePath(servicePath, command, 'package build')
    }

    void ensureSupportedTool(String tool) {
        if (tool != 'maven') {
            throw new IllegalArgumentException("Unsupported build tool '${tool}'. Version 1 supports only maven.")
        }
    }

    private void executeInServicePath(String servicePath, String command, String stageName) {
        if (!command?.trim()) {
            throw new IllegalArgumentException("Missing command for ${stageName}")
        }
        steps.dir(servicePath) {
            steps.sh label: "Execute ${stageName}", script: command
        }
    }
}
