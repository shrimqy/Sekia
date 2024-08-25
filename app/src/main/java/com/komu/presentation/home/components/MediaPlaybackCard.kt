package com.komu.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import komu.seki.common.util.base64ToBitmap
import komu.seki.domain.models.PlaybackData

@Composable
fun MediaPlaybackCard(
    playbackData: PlaybackData?,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        playbackData?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                // Background image
                Image(
                    painter = rememberAsyncImagePainter(model = base64ToBitmap(playbackData.thumbnail)),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()  // Ensures the image takes up the entire card
                        .clip(RoundedCornerShape(16.dp)),  // Ensures the image respects the card's rounded corners
                    contentScale = ContentScale.Crop  // Ensures the image covers the entire background without distortion
                )
                // Overlay content
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)) // Add a semi-transparent overlay
                        .padding(16.dp),

                    ) {
                    Column(
                        modifier = Modifier
                            .weight(1f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        playbackData.appName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                            ) {
                                Text(
                                    text = playbackData.trackTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = playbackData.artist ?: "Unknown Artist",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = onPlayPauseClick, modifier = modifier) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp) // Adjust the size as needed
                                        .clip(RoundedCornerShape(12.dp)) // Rounded rectangle shape
                                        .background(MaterialTheme.colorScheme.primary) // Background color
                                        .padding(8.dp) // Padding inside the button
                                ) {
                                    Icon(
                                        imageVector = if (playbackData.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playbackData.isPlaying) "Pause" else "Play",
                                        tint = MaterialTheme.colorScheme.onPrimary, // Icon color
                                        modifier = Modifier.fillMaxSize() // Ensure the icon fills the button
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onSkipPreviousClick) {
                                Icon(
                                    imageVector = Icons.Outlined.SkipPrevious,
                                    contentDescription = "Skip Previous",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = onSkipNextClick) {
                                Icon(
                                    imageVector = Icons.Outlined.SkipNext,
                                    contentDescription = "Skip Next",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        } ?: EmptyPlaybackHolder()
    }
}

@Composable
fun EmptyPlaybackHolder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "No Media Playback")
    }
}