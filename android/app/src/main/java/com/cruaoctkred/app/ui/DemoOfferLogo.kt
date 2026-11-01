package com.cruaoctkred.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.cruaoctkred.app.R
import kotlin.math.min
import kotlin.math.roundToInt

/** Shows the supplied screenshots without their empty top and bottom margins. */
@Composable
fun DemoOfferLogo(cardId: String, borderColor: Color) {
    val (resource, cropTop, cropBottom, title) = when (cardId) {
        "demo-moneycat" -> LogoCrop(R.drawable.demo_moneycat_logo, 66, 68, "MoneyCat")
        "demo-kviku" -> LogoCrop(R.drawable.demo_kviku_logo, 42, 48, "Kviku")
        else -> return
    }
    val image = ImageBitmap.imageResource(resource)
    val width = if (LocalConfiguration.current.screenWidthDp <= 340) 68 else 92
    val height = ((width - 4f) * (image.height - cropTop - cropBottom) / image.width + 4f).roundToInt()
    Surface(
        modifier = Modifier.size(width = width.dp, height = height.dp)
            .semantics { contentDescription = title },
        shape = RoundedCornerShape(9.dp),
        color = Color.White,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Canvas(Modifier.fillMaxSize().padding(2.dp)) {
            drawCroppedLogo(image, cropTop, cropBottom)
        }
    }
}

private data class LogoCrop(
    val resource: Int,
    val cropTop: Int,
    val cropBottom: Int,
    val title: String,
)

private fun DrawScope.drawCroppedLogo(image: ImageBitmap, cropTop: Int, cropBottom: Int) {
    val sourceHeight = image.height - cropTop - cropBottom
    val aspect = image.width.toFloat() / sourceHeight
    val width = min(size.width, size.height * aspect)
    val height = width / aspect
    drawImage(
        image = image,
        srcOffset = IntOffset(0, cropTop),
        srcSize = IntSize(image.width, sourceHeight),
        dstOffset = IntOffset(((size.width - width) / 2).roundToInt(), ((size.height - height) / 2).roundToInt()),
        dstSize = IntSize(width.roundToInt(), height.roundToInt()),
    )
}
