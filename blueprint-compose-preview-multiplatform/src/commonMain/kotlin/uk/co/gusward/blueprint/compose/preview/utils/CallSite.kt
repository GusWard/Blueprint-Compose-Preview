package uk.co.gusward.blueprint.compose.preview.utils

/**
 * Returns a unique string identifying the call site of this function.
 * On JVM platforms, this uses the stack trace to include function name and line number.
 */
expect fun getCallSiteId(): String
