package uk.co.gusward.blueprint.report.gradle

object FqnFilter {
    // Empty patterns = match all. Wildcard: trailing ".*" = prefix match on everything before it.
    fun matches(sourceFqn: String, patterns: List<String>): Boolean {
        if (patterns.isEmpty()) return true
        return patterns.any { pattern ->
            if (pattern.endsWith(".*")) {
                val prefix = pattern.dropLast(2) // strip ".*"
                sourceFqn.startsWith("$prefix.") || sourceFqn == prefix
            } else {
                sourceFqn == pattern
            }
        }
    }
}
