package com.cruaoctkred.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.cruaoctkred.app.R

@Composable
fun ProductSplash() {
    // Only the decorative outer pattern may extend beyond the screen; the logo is central.
    Image(painterResource(R.drawable.brand_splash_pattern), null,
        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
}
