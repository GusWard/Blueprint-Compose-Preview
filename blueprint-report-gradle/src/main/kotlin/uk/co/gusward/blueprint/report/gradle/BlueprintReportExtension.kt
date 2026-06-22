package uk.co.gusward.blueprint.report.gradle

import androidx.annotation.FloatRange

@DslMarker
annotation class BlueprintReportDsl

@BlueprintReportDsl
abstract class BlueprintReportExtension {
    internal val fqnPatterns: MutableList<String> = mutableListOf()

    @FloatRange(from = 0.0, to = 1.0)
    var backgroundAlpha: Float = 1f

    @FloatRange(from = 0.0, to = 1.0)
    var contentAlpha: Float = 1f

    fun fqn(pattern: String) {
        fqnPatterns += pattern
    }
}
