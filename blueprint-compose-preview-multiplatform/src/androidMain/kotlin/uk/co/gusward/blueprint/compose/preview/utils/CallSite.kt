package uk.co.gusward.blueprint.compose.preview.utils

actual fun getCallSiteId(): String {
    val stackTrace = Throwable().stackTrace
    // We want the frame that called BlueprintPreview.
    // Index 0: getCallSiteId
    // Index 1: BlueprintPreview (the caller of this)
    // Index 2: The actual code calling BlueprintPreview
    return if (stackTrace.size > 2) {
        val element = stackTrace[2]
        "${element.className}.${element.methodName}"
    } else {
        "unknown"
    }
}
