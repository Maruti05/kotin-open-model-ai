package com.vedica.labs.ind.app.chat.openmodels.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.DarkBorder
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.DarkCard
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.DarkObsidian
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.LightBorder
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.LightCard

@Composable
fun StyledCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    gradient: Brush? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (MaterialTheme.colorScheme.background == DarkObsidian) DarkBorder else LightBorder
    val bgColor = if (MaterialTheme.colorScheme.background == DarkObsidian) DarkCard else LightCard

    Card(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (gradient != null) Color.Transparent else bgColor
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (gradient != null) {
            Box(
                modifier = Modifier
                    .background(gradient, shape)
                    .then(Modifier.padding(16.dp))
            ) {
                content()
            }
        } else {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}
