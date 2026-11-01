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
class BottomBarTest {
    @get:Rule val compose = createComposeRule()

    private val items = listOf(
        BottomBarItem("Главная", Icons.Outlined.Home),
        BottomBarItem("Подбор", Icons.Outlined.Search),
        BottomBarItem("Калькулятор", Icons.Outlined.Calculate),
        BottomBarItem("Избранное", Icons.Outlined.FavoriteBorder),
        BottomBarItem("Профиль", Icons.Outlined.PersonOutline),
    )

    @Test fun destinationsAreControlledByHost() {
        RuntimeEnvironment.setQualifiers("w320dp-h900dp-mdpi")
        var clicked = -1
        compose.setContent { AppBottomBar(items, 1, { clicked = it }) }
        compose.onAllNodes(isSelectable()).assertCountEquals(5)
        items.forEachIndexed { index, item ->
            compose.onNodeWithText(item.label).assertIsDisplayed().performClick()
            assertEquals(index, clicked)
        }
        compose.onNodeWithText("Подбор").assertIsSelected()
        compose.onNodeWithText("Калькулятор").assertIsNotSelected()
    }

    @Test fun shellReservesNoHorizontalSpace() {
        var reservation = (-1).dp
        compose.setContent {
            AppNavShell(items.take(4), 0, {}) { reservation = it }
        }
        compose.waitForIdle()
        assertTrue(BottomBarDefaults.Floating)
        assertEquals(0.dp, reservation)
        compose.onAllNodes(isSelectable()).assertCountEquals(4)
    }
}
