package uk.co.gusward.blueprint.compose.preview.utils

import java.text.DecimalFormat

actual fun formatNumber(value: Float): String {
    return DecimalFormat("0.##").format(value)
}
