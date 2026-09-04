package org.arun.ci.model

class SecurityFinding {
    String id
    String source
    String gate
    String component
    String installedVersion
    String fixedVersion
    String severity
    String description
    String reportLocation

    String normalizedId() {
        return (id ?: '').trim().toUpperCase(Locale.ROOT)
    }
}
