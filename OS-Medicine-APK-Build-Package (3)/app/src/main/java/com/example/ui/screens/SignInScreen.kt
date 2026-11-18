package com.example.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalOsExtraColors
import com.example.ui.theme.OsCoral
import com.example.ui.theme.OsPrimary
import com.example.ui.theme.OsPrimaryDark

@Composable
fun SignInScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleSignIn: () -> Unit
) {
    val extras = LocalOsExtraColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(OsPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalPharmacy,
                    contentDescription = "OS Medicine logo",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                        append("OS ")
                    }
                    withStyle(SpanStyle(color = OsCoral)) {
                        append("Medicine")
                    }
                },
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ethiopia's Real-Time Pharmacy Network",
                style = MaterialTheme.typography.titleSmall,
                color = OsPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = OsPrimaryDark,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "🇪🇹 Addis Ababa & Beyond",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Locate prescribed medicines fast.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sign in securely with Google to broadcast prescriptions to verified pharmacies within 5 km, chat live, and unlock Google Maps directions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD6E4DF)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FeatureLine(Icons.Filled.LocationOn, "5 km live pharmacy radar & pickup/delivery")
                    Spacer(modifier = Modifier.height(6.dp))
                    FeatureLine(Icons.Filled.VerifiedUser, "Verified Ethiopian licensed pharmacies")
                    Spacer(modifier = Modifier.height(6.dp))
                    FeatureLine(Icons.Filled.Security, "Cloud-synced prescriptions, chat & Telebirr receipts")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = extras.danger.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, extras.danger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = errorMessage,
                        color = extras.danger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = onGoogleSignIn,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = OsCoral),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Signing in...", fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        text = "Continue with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Protected by Firebase Authentication · Role-based access for Patients, Pharmacies & Admins",
                style = MaterialTheme.typography.bodySmall,
                color = extras.muted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OsCoral,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}
