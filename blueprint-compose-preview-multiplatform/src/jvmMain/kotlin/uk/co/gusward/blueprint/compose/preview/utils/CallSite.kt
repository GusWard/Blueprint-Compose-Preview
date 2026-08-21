package uk.co.gusward.blueprint.compose.preview.utils

actual fun getCallSiteId(): String {
    val stackTrace = Throwable().stackTrace
    return if (stackTrace.size > 2) {
        val element = stackTrace[2]
        "${element.className}.${element.methodName}"
    } else {
        "unknown"
    }
}
