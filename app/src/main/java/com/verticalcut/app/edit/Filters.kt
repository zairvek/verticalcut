package com.verticalcut.app.edit

import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter

/** Efectos de Media3 que implementan cada filtro. Se usan igual en la vista previa y en la exportación. */
@OptIn(UnstableApi::class)
fun Filter.toEffects(): List<Effect> = when (this) {
    Filter.NONE -> emptyList()
    Filter.WARM -> listOf(RgbAdjustment.Builder().setRedScale(1.12f).setGreenScale(1.02f).setBlueScale(0.88f).build())
    Filter.COOL -> listOf(RgbAdjustment.Builder().setRedScale(0.9f).setGreenScale(1.0f).setBlueScale(1.15f).build())
    Filter.VIVID -> listOf(Contrast(0.25f), RgbAdjustment.Builder().setRedScale(1.08f).setGreenScale(1.08f).setBlueScale(1.08f).build())
    Filter.MONO -> listOf(RgbFilter.createGrayscaleFilter())
    Filter.NEGATIVE -> listOf(RgbFilter.createInvertedFilter())
}
