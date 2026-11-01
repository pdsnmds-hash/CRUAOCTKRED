package com.cruaoctkred.app.ui.bottomnav.fintech_3

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BottomBarLayoutsTest {
    private val items = listOf(
        BottomBarItem("Главная", Icons.Outlined.Home),
        BottomBarItem("Подбор", Icons.Outlined.Search),
        BottomBarItem("Калькулятор", Icons.Outlined.Calculate),
        BottomBarItem("Избранное", Icons.Outlined.FavoriteBorder),
        BottomBarItem("Профиль", Icons.Outlined.PersonOutline),
    )

    @get:Rule val compose = createComposeRule()

    @Test fun layout320() = assertLayout(320)
    @Test fun layout360() = assertLayout(360)
    @Test fun layout411() = assertLayout(411)

    private fun assertLayout(width: Int) {
        RuntimeEnvironment.setQualifiers("w${width}dp-h900dp-mdpi")
        var clicked = -1
        compose.setContent { AppBottomBar(items, 0, { clicked = it }) }
        val tabs = compose.onAllNodes(isSelectable())
        tabs.assertCountEquals(items.size)
        val bounds = tabs.fetchSemanticsNodes().map { it.boundsInRoot }
        bounds.forEachIndexed { index, rect ->
            tabs[index].assertIsDisplayed().assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
            assertTrue("Tab $index exceeds horizontal bounds at $width dp",
                rect.left >= 8f && rect.right <= width - 8f)
            if (index > 0) assertTrue("Tabs overlap at $width dp",
                bounds[index - 1].right <= rect.left)
            compose.onNodeWithText(items[index].label).assertIsDisplayed().performClick()
            assertEquals(index, clicked)
        }
    }
}
