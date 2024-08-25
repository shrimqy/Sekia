package com.komu.presentation.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Battery2Bar
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncDisabled
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import komu.seki.domain.models.DeviceDetails

@Composable
fun DeviceCard(
    modifier: Modifier = Modifier,
    deviceDetails: DeviceDetails?,
    syncStatus: Boolean,
    onSyncAction: () -> Unit,
    batteryLevel: Int? = null,
) {
    Card(
        onClick = { /*TODO: Handle card click */ },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(),
        modifier = modifier
    ) {
        Box(
            Modifier.padding(16.dp)) {
            deviceDetails?.let { device ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular profile picture
                    Image(
                        painter = rememberAsyncImagePainter(model = null),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                    Spacer(Modifier.width(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                            device.deviceName?.let {
                                Text(
                                    text = it,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Row {
                                batteryLevel?.let { level ->
                                    Icon(
                                        imageVector = Icons.Rounded.Battery2Bar,
                                        contentDescription = "Battery Icon",
                                    )
                                    Text(text = "$level%")
                                }
                            }
                            Text(
                                text = if (syncStatus) "Connected" else "Disconnected",
                            )
                        }
                        IconButton(onClick = onSyncAction) {
                            Icon(
                                imageVector = if (syncStatus) Icons.Rounded.SyncDisabled else Icons.Rounded.Sync,
                                contentDescription = if (syncStatus) "Disconnect" else "Sync",
                            )
                        }
                    }
                }
            } ?: EmptyPlaceholder()
        }
    }
}

@Composable
fun EmptyPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "No device information available")
    }
}