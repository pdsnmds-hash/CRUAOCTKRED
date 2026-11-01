package com.cruaoctkred.app.ui.bottomnav.fintech_3

import com.cruaoctkred.app.ui.components.brandGradient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One tab. The label is a resolved String, not a @StringRes: a module never reads the
 * product's resources, so the product calls stringResource and passes the text in.
 */
data class BottomBarItem(
    val label: String,
    val icon: ImageVector,
)

data class BottomBarColors(
    val container: Color,
    val selectedIcon: Color,
    val selectedText: Color,
    val indicator: Color,
    val unselectedIcon: Color,
    val unselectedText: Color,
)

object BottomBarDefaults {

    /**
     * True when the bar is drawn OVER the feed and the host must not reserve a strip for it.
     *
     * Part of the slot contract: every variant declares it, and the product reads it from
     * whichever variant is wired in. A floating pill needs the content to run underneath or
     * its rounded edges sit on dead background; a solid full-width bar needs the opposite,
     * or the last card is hidden forever. Letting the variant answer keeps the two
     * interchangeable without touching the product.
     */
    const val Floating = true

    /** Reference palette; products may override every color explicitly. */
    @Composable
    fun colors(
        container: Color = Color(0xFF101D3B).copy(alpha = 0.96f),
        selectedIcon: Color = Color.White,
        selectedText: Color = Color.White,
        indicator: Color = Color(0xFF3266FF),
        unselectedIcon: Color = Color(0xFF9BAACB),
        unselectedText: Color = Color(0xFF9BAACB),
    ) = BottomBarColors(
        container = container,
        selectedIcon = selectedIcon,
        selectedText = selectedText,
        indicator = indicator,
        unselectedIcon = unselectedIcon,
        unselectedText = unselectedText,
    )
}

/** Glass-style capsule. Every destination and icon is supplied by the host. */
@Composable
fun AppBottomBar(
    items: List<BottomBarItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: BottomBarColors = BottomBarDefaults.colors(),
) {
    Surface(
        modifier = modifier.fillMaxWidth().navigationBarsPadding()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = colors.container,
        border = BorderStroke(1.dp, lerp(colors.indicator, colors.unselectedIcon, 0.25f)
            .copy(alpha = 0.75f)),
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .brandGradient(strength = .12f)
                .heightIn(min = 70.dp).padding(horizontal = 5.dp).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val active = selected == index
                Column(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .selectable(active, role = Role.Tab, onClick = { onSelect(index) })
                        .padding(horizontal = 1.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier.size(34.dp)
                            .then(if (active) Modifier.brandGradient(RoundedCornerShape(13.dp), .25f) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, contentDescription = null,
                            tint = if (active) colors.selectedIcon else colors.unselectedIcon,
                            modifier = Modifier.size(21.dp))
                    }
                    Text(
                        item.label,
                        style = TextStyle(fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (active) colors.selectedText else colors.unselectedText,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Bottom overlay; content needs no horizontal reservation. System insets are handled here. */
@Composable
fun AppNavShell(
    items: List<BottomBarItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: BottomBarColors = BottomBarDefaults.colors(),
    content: @Composable (endReservation: Dp) -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        content(0.dp)
        AppBottomBar(
            items = items,
            selected = selected,
            onSelect = onSelect,
            modifier = Modifier.align(Alignment.BottomCenter),
            colors = colors,
        )
    }
}
