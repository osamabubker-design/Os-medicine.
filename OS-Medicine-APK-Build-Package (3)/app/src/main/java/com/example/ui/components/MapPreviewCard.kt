package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalOsExtraColors
import com.example.ui.theme.OsAccentGold
import com.example.ui.theme.OsCoral
import com.example.ui.theme.OsPrimary
import com.example.ui.theme.OsPrimaryDark

@Composable
fun LockedLocationMapCard(
    distanceKm: Double,
    unlockButtonLabel: String,
    onUnlockClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF28322E))
            .border(1.dp, Color(0xFF3E4B46), RoundedCornerShape(16.dp))
    ) {
        // Subtle blurred map grid background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 48f
            for (x in 0..size.width.toInt() step step.toInt()) {
                drawLine(Color(0x22FFFFFF), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1.5f)
            }
            for (y in 0..size.height.toInt() step step.toInt()) {
                drawLine(Color(0x22FFFFFF), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1.5f)
            }
            drawLine(
                color = Color(0x44C9963B),
                start = Offset(0f, size.height * 0.8f),
                end = Offset(size.width, size.height * 0.2f),
                strokeWidth = 10f
            )
        }

        // Distance pill top-right
        Surface(
            color = Color(0xCC111917),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            Text(
                text = String.format("%.1f km away", distanceKm),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(OsAccentGold.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Location locked",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pharmacy Approved! Location Hidden",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Pay 50 ETB fee to reveal exact location & Google Maps",
                color = Color(0xFFD0D5D2),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onUnlockClick,
                colors = ButtonDefaults.buttonColors(containerColor = OsCoral),
                shape = CircleShape,
                modifier = Modifier.testTag("unlock_location_map_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(unlockButtonLabel, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Map,
                contentDescription = null,
                tint = Color(0xFFB4BBB7),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Google Maps · Ethiopia",
                color = Color(0xFFB4BBB7),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun UnlockedMapDirectionsCard(
    pharmacyName: String,
    address: String,
    distanceKm: Double,
    directionsButtonLabel: String,
    onDirectionsClick: () -> Unit
) {
    val extras = LocalOsExtraColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, extras.border, RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .background(Color(0xFFE7EAE4))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val blockSize = 68f
                var x = 0f
                while (x < size.width) {
                    drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = 10f)
                    x += blockSize
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(Color.White, Offset(0f, y), Offset(size.width, y), strokeWidth = 10f)
                    y += blockSize
                }
                // Yellow arterial road
                val p1 = Offset(0f, size.height * 0.78f)
                val p2 = Offset(size.width, size.height * 0.22f)
                drawLine(Color(0xFFF6C667), p1, p2, strokeWidth = 14f)
                // Dashed route between patient dot and pharmacy pin
                val startPt = Offset(size.width * 0.30f, size.height * 0.62f)
                val endPt = Offset(size.width * 0.72f, size.height * 0.38f)
                drawLine(
                    color = OsPrimaryDark,
                    start = startPt,
                    end = endPt,
                    strokeWidth = 6f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
                // Patient blue marker
                drawCircle(Color(0x331565C0), radius = 26f, center = startPt)
                drawCircle(Color.White, radius = 14f, center = startPt)
                drawCircle(Color(0xFF1565C0), radius = 9f, center = startPt)
                // Pharmacy green marker
                drawCircle(Color(0x330F5C4C), radius = 30f, center = endPt)
                drawCircle(Color.White, radius = 15f, center = endPt)
                drawCircle(OsPrimary, radius = 10f, center = endPt)
            }

            Surface(
                color = OsPrimaryDark,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Text(
                    text = String.format("%.1f km away", distanceKm),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Surface(
                color = Color.White.copy(alpha = 0.92f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = null,
                        tint = OsPrimaryDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Google Maps · Ethiopia",
                        color = Color(0xFF1B2420),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = pharmacyName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = OsCoral,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.muted
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onDirectionsClick,
                colors = ButtonDefaults.buttonColors(containerColor = OsPrimaryDark),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("start_google_maps_directions_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(directionsButtonLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
