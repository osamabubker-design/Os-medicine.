package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Pharmacy
import com.example.ui.theme.LocalOsExtraColors
import com.example.ui.theme.OsAccentGold
import com.example.ui.theme.OsCoral
import com.example.ui.theme.OsPrimary
import com.example.ui.theme.OsPrimaryDark
import com.example.ui.theme.OsSuccess
import com.example.ui.viewmodel.MainNavTab
import com.example.utils.AppStrings

@Composable
fun HomeScreen(
    strings: AppStrings,
    approvedPharmacies: List<Pharmacy>,
    onNavigateTab: (MainNavTab) -> Unit,
    onOpenAiHelper: () -> Unit
) {
    val extras = LocalOsExtraColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp)) {
            // Dark Emerald Hero Card matching Screenshot_20261103_124910
            Surface(
                color = OsPrimaryDark,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "🇪🇹 ${strings.heroBadge}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = strings.heroHeadline,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Color.White,
                            fontSize = 27.sp,
                            lineHeight = 33.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = strings.heroSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD5E4DF)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigateTab(MainNavTab.PATIENT) },
                            colors = ButtonDefaults.buttonColors(containerColor = OsCoral),
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("hero_search_medicine_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.searchMedicineCta,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { onNavigateTab(MainNavTab.PHARMACY) },
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color.White.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(48.dp)
                                .testTag("hero_pharmacy_btn")
                        ) {
                            Text(
                                text = strings.runPharmacyCta,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = strings.platformCapabilities,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 2x2 Platform Capabilities Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CapabilityCard(
                    icon = Icons.Filled.PhotoCamera,
                    badge = "Fast",
                    title = "Upload Prescription",
                    subtitle = "Photo or camera capture",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(MainNavTab.PATIENT) }
                )
                CapabilityCard(
                    icon = Icons.Filled.MedicalServices,
                    badge = "5km Radar",
                    title = "Search Stock",
                    subtitle = "Checked in 5km radius",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(MainNavTab.PATIENT) }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CapabilityCard(
                    icon = Icons.Filled.CreditCard,
                    badge = "Instant",
                    title = "telebirr & Chapa",
                    subtitle = "50 ETB unlock fee",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(MainNavTab.PATIENT) }
                )
                CapabilityCard(
                    icon = Icons.Filled.DeliveryDining,
                    badge = "Doorstep",
                    title = "Delivery Support",
                    subtitle = "Doorstep rider or pickup",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(MainNavTab.PATIENT) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            // AI Prescription Assistant Banner
            Surface(
                color = OsPrimary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OsPrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenAiHelper)
                    .testTag("home_ai_guide_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(OsPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI Medicine Guide",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.aiHelperTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = OsPrimary
                        )
                        Text(
                            text = "Ask medicine questions or read prescription handwriting with Firebase AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = extras.muted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            Text(text = strings.howItWorks, style = MaterialTheme.typography.titleLarge)
            Text(
                text = strings.howItWorksSub,
                style = MaterialTheme.typography.bodySmall,
                color = extras.muted
            )
            Spacer(modifier = Modifier.height(12.dp))

            StepCard(
                number = "1",
                title = "Upload & broadcast free",
                subtitle = "Snap your prescription and choose your location in Addis Ababa & across Ethiopia."
            )
            Spacer(modifier = Modifier.height(8.dp))
            StepCard(
                number = "2",
                title = "Pharmacies confirm stock",
                subtitle = "Verified pharmacies within 5 km check inventory and confirm availability live."
            )
            Spacer(modifier = Modifier.height(8.dp))
            StepCard(
                number = "3",
                title = "Unlock & pick up or get delivery",
                subtitle = "Pay 50 ETB via telebirr (+251925694548), upload proof, and unlock Google Maps directions."
            )

            Spacer(modifier = Modifier.height(22.dp))
            Text(text = strings.featuredPharmacies, style = MaterialTheme.typography.titleLarge)
            Text(
                text = strings.featuredPharmaciesSub,
                style = MaterialTheme.typography.bodySmall,
                color = extras.muted
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (approvedPharmacies.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, extras.border, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "No verified pharmacies listed yet",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Registered Ethiopian pharmacies appear here automatically once verified by an administrator.",
                            style = MaterialTheme.typography.bodySmall,
                            color = extras.muted
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    approvedPharmacies.take(5).forEach { pharmacy ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, extras.border, RoundedCornerShape(14.dp))
                                .clickable { onNavigateTab(MainNavTab.PATIENT) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = pharmacy.pharmacyName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Verified,
                                            contentDescription = "Verified",
                                            tint = OsSuccess,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "${pharmacy.address} · Pharmacist: ${pharmacy.pharmacistName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extras.muted
                                    )
                                }
                                Surface(
                                    color = if (pharmacy.availability) OsSuccess.copy(alpha = 0.12f) else extras.border,
                                    shape = CircleShape
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = OsAccentGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = String.format("%.1f", pharmacy.averageRating),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (pharmacy.availability) OsSuccess else extras.muted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            Text(text = strings.chooseYourRole, style = MaterialTheme.typography.titleLarge)
            Text(
                text = strings.chooseYourRoleSub,
                style = MaterialTheme.typography.bodySmall,
                color = extras.muted
            )
            Spacer(modifier = Modifier.height(10.dp))

            RoleCard(
                icon = Icons.Filled.Person,
                title = strings.navPatient,
                subtitle = "Upload prescription, search stock & unlock directions",
                accent = OsCoral,
                onClick = { onNavigateTab(MainNavTab.PATIENT) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            RoleCard(
                icon = Icons.Filled.LocalPharmacy,
                title = strings.navPharmacy,
                subtitle = "Register pharmacy, answer patient requests & manage stock",
                accent = OsPrimary,
                onClick = { onNavigateTab(MainNavTab.PHARMACY) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            RoleCard(
                icon = Icons.Filled.AdminPanelSettings,
                title = strings.navAdmin,
                subtitle = "Verify 50 ETB Telebirr receipts & approve licensed pharmacies",
                accent = OsPrimaryDark,
                onClick = { onNavigateTab(MainNavTab.ADMIN) }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CapabilityCard(
    icon: ImageVector,
    badge: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val extras = LocalOsExtraColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(1.dp, extras.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(OsPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = OsPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = badge,
                    color = OsPrimary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = extras.muted)
        }
    }
}

@Composable
private fun StepCard(number: String, title: String, subtitle: String) {
    val extras = LocalOsExtraColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, extras.border, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(OsCoral),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = extras.muted)
            }
        }
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    val extras = LocalOsExtraColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, extras.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = accent)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = extras.muted)
            }
        }
    }
}
