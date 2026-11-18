package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AppNotification
import com.example.ui.theme.LocalOsExtraColors
import com.example.ui.theme.OsCoral
import com.example.ui.theme.OsPrimary
import com.example.utils.AppStrings
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    strings: AppStrings,
    notifications: List<AppNotification>,
    onNotificationClick: (AppNotification) -> Unit,
    onClearRead: () -> Unit,
    onDismiss: () -> Unit
) {
    val extras = LocalOsExtraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.notificationsTitle,
                    style = MaterialTheme.typography.titleLarge
                )
                if (notifications.any { it.isRead }) {
                    TextButton(onClick = onClearRead) {
                        Text(strings.clearReadBtn, color = OsPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsNone,
                        contentDescription = null,
                        tint = extras.muted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.noNotifications,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extras.muted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (!item.isRead) OsPrimary.copy(alpha = 0.06f)
                                    else MaterialTheme.colorScheme.background
                                )
                                .border(1.dp, extras.border, RoundedCornerShape(12.dp))
                                .clickable { onNotificationClick(item) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(if (!item.isRead) OsCoral else extras.border)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = item.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "Just now",
                                        fontSize = 11.sp,
                                        color = extras.muted
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = item.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extras.muted
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
