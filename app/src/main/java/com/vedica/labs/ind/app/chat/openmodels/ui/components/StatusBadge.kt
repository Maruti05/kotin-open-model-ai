package com.vedica.labs.ind.app.chat.openmodels.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.ActiveGreen
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.ErrorRed

@Composable
fun StatusBadge(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) ActiveGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)
    val textColor = if (isActive) ActiveGreen else ErrorRed

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
