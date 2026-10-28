package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SensorType
import com.example.data.local.SmartSensorEntity
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TvSurfaceDark

@Composable
fun SmartSensorCard(
    sensor: SmartSensorEntity,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (sensor.type) {
        SensorType.TEMPERATURE -> Icons.Default.Thermostat
        SensorType.HUMIDITY -> Icons.Default.WaterDrop
        SensorType.LIGHT_SWITCH -> Icons.Default.Lightbulb
        SensorType.POWER_METER -> Icons.Default.Bolt
        SensorType.SECURITY_MOTION -> Icons.Default.Security
        SensorType.AIR_QUALITY -> Icons.Default.Air
    }

    val iconTint: Color = when (sensor.type) {
        SensorType.TEMPERATURE -> AccentAmber
        SensorType.HUMIDITY -> AccentCyan
        SensorType.LIGHT_SWITCH -> if (sensor.isActive) EmeraldLight else TextMuted
        SensorType.POWER_METER -> AccentAmber
        SensorType.SECURITY_MOTION -> if (sensor.isActive) AccentRed else EmeraldLight
        SensorType.AIR_QUALITY -> EmeraldLight
    }

    TvFocusableCard(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = TvSurfaceDark,
        testTag = "sensor_card_${sensor.entityId}"
    ) { isFocused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = if (isFocused) 0.25f else 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = sensor.name,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = sensor.name,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = sensor.location,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sensor.stateValue,
                    color = if (sensor.type == SensorType.LIGHT_SWITCH && sensor.isActive) EmeraldPrimary else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (sensor.unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = sensor.unit,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
