package com.vixcy.banana.ui.theme

import androidx.compose.material3.Typography
import com.vixcy.banana.ui.Banana

// Material3 Typography wired to our Banana type scale.
// Most screens read directly from Banana.Type — this exists so MaterialTheme
// defaults (Buttons, Surfaces, etc.) inherit a sensible base.
val Typography = Typography(
    displayLarge   = Banana.Type.display,
    displayMedium  = Banana.Type.largeTitle,
    titleLarge     = Banana.Type.title,
    titleMedium    = Banana.Type.title2,
    titleSmall     = Banana.Type.headline,
    bodyLarge      = Banana.Type.body,
    bodyMedium     = Banana.Type.bodyMedium,
    bodySmall      = Banana.Type.callout,
    labelLarge     = Banana.Type.headline,
    labelMedium    = Banana.Type.captionBold,
    labelSmall     = Banana.Type.tagLabel
)
