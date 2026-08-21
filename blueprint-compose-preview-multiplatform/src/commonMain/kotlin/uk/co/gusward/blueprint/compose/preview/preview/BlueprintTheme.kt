package uk.co.gusward.blueprint.compose.preview.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import uk.co.gusward.blueprint.compose.preview.constants.SemanticColors

@Composable
fun BlueprintTheme(
    backgroundAlpha: Float = 1.0f,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.background(SemanticColors.BlueprintBackground.copy(alpha = backgroundAlpha))
    ) {
        CompositionLocalProvider(
            LocalContentColor provides SemanticColors.BlueprintAccent
        ) {
            ProvideTextStyle(
                value = LocalTextStyle.current.merge(
                    TextStyle(
                        color = SemanticColors.BlueprintAccent
                    )
                )
            ) {
                content()
            }
        }
    }
}
