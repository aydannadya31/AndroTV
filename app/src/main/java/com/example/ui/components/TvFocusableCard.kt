package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TvSurfaceBorder
import com.example.ui.theme.TvSurfaceElevated

/**
 * Android TV D-pad kumandası için özel yumuşak ölçeklemeli ve zümrüt yeşili odaklamalı kart bileşeni.
 */
@Composable
fun TvFocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = TvSurfaceElevated,
    focusedBorderColor: Color = EmeraldPrimary,
    unfocusedBorderColor: Color = TvSurfaceBorder,
    focusedElevation: Dp = 12.dp,
    unfocusedElevation: Dp = 2.dp,
    scaleOnFocus: Float = 1.06f,
    focusRequester: FocusRequester? = null,
    testTag: String = "tv_focusable_card",
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isFocused) scaleOnFocus else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "tv_card_scale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    var finalModifier = modifier
        .testTag(testTag)
        .scale(animatedScale)
        .shadow(
            elevation = if (isFocused) focusedElevation else unfocusedElevation,
            shape = shape,
            spotColor = if (isFocused) EmeraldGlow else Color.Transparent
        )

    if (focusRequester != null) {
        finalModifier = finalModifier.focusRequester(focusRequester)
    }

    Surface(
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) focusedBorderColor else unfocusedBorderColor
        ),
        modifier = finalModifier
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                    val code = keyEvent.nativeKeyEvent.keyCode
                    if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
                        onClick()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
    ) {
        Box {
            content(isFocused)
        }
    }
}
