package uk.co.gusward.blueprint.report.gradle

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FqnFilterTest {

    @Test fun `empty patterns matches everything`() {
        assertTrue(FqnFilter.matches("com.example.ui.PreviewButton", emptyList()))
    }

    @Test fun `exact pattern matches identical fqn`() {
        assertTrue(FqnFilter.matches("com.example.ui.PreviewButton", listOf("com.example.ui.PreviewButton")))
    }

    @Test fun `exact pattern does not match different fqn`() {
        assertFalse(FqnFilter.matches("com.example.ui.PreviewCard", listOf("com.example.ui.PreviewButton")))
    }

    @Test fun `wildcard matches all in package`() {
        assertTrue(FqnFilter.matches("com.example.ui.PreviewButton", listOf("com.example.ui.*")))
        assertTrue(FqnFilter.matches("com.example.ui.buttons.PreviewPrimary", listOf("com.example.ui.*")))
    }

    @Test fun `wildcard does not match outside prefix`() {
        assertFalse(FqnFilter.matches("com.other.PreviewButton", listOf("com.example.ui.*")))
    }

    @Test fun `multiple patterns — matches if any matches`() {
        assertTrue(FqnFilter.matches("com.example.PreviewA", listOf("com.other.*", "com.example.PreviewA")))
    }
}
