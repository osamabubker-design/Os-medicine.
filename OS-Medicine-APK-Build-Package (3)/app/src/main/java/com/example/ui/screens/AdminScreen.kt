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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.AdminLog
import com.example.data.models.PaymentSubmission
import com.example.data.models.Pharmacy
import com.example.data.models.PrescriptionRequest
import com.example.data.repositories.StorageRepository
import com.example.ui.theme.LocalOsExtraColors
import com.example.ui.theme.OsAccentGold
import com.example.ui.theme.OsCoral
import com.example.ui.theme.OsPrimary
import com.example.ui.theme.OsPrimaryDark
import com.example.ui.theme.OsSuccess
import com.example.utils.AppStrings
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun AdminScreen(
    strings: AppStrings,
    isAdminAuthorized: Boolean,
    adminSessionVerified: Boolean,
    allPharmacies: List<Pharmacy>,
    allRequests: List<PrescriptionRequest>,
    allPayments: List<PaymentSubmission>,
    adminLogs: List<AdminLog>,
    onVerifyAdminAccess: () -> Unit,
    onLockAdminSession: () -> Unit,
    onSwitchGoogleAccount: () -> Unit,
    onVerifyPayment: (PaymentSubmission, Boolean, String) -> Unit,
    onUpdatePharmacyStatus: (Pharmacy, String, String) -> Unit
) {
    val extras = LocalOsExtraColors.current

    if (!adminSessionVerified) {
        val currentEmail = Firebase.auth.currentUser?.email ?: "Not signed in"
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .border(1.dp, extras.border, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(OsPrimary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = "Admin authorization",
                            tint = OsPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Admin Access",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Protected by Firebase Authentication & Firestore Admin Rules (no hardcoded password). Signed-in account: $currentEmail",
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.muted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onVerifyAdminAccess,
                        colors = ButtonDefaults.buttonColors(containerColor = OsPrimary),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_verify_access_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAdminAuthorized) "Unlock Admin Dashboard" else "Verify Admin Authorization",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onSwitchGoogleAccount,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Switch Google Account")
                    }
                }
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val pendingPayments = allPayments.filter { it.verificationStatus == "PENDING" }
    val pendingPharmacies = allPharmacies.filter { it.approvalStatus == "PENDING" }
    val collectedEtb = allPayments.filter { it.verificationStatus == "APPROVED" }.sumOf { it.paymentAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = strings.adminDashboardTitle, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Monitor platform activity, approve pharmacies, verify telebirr fees",
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.muted
                    )
                }
                OutlinedButton(
                    onClick = onLockAdminSession,
                    shape = CircleShape,
                    modifier = Modifier.testTag("admin_lock_btn")
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lock")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            // 2x2 Stats Grid matching Screenshot_20261109_010244
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard(
                    value = "${collectedEtb.toInt()} ETB",
                    label = "Revenue Collected",
                    accent = OsSuccess,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    value = pendingPayments.size.toString(),
                    label = "Payment Approvals",
                    accent = OsAccentGold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard(
                    value = allPharmacies.size.toString(),
                    label = "Total Pharmacies",
                    accent = OsPrimary,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    value = pendingPharmacies.size.toString(),
                    label = "Pending Pharmacies",
                    accent = OsCoral,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("${strings.paymentApprovalsTab} (${pendingPayments.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${strings.pharmaciesTab} (${allPharmacies.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("${strings.requestsTab} (${allRequests.size})") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text(strings.auditLogsTab) }
                )
            }

            HorizontalDivider(color = extras.border)
            Spacer(modifier = Modifier.height(10.dp))

            when (selectedTab) {
                0 -> AdminPaymentsTab(allPayments, onVerifyPayment)
                1 -> AdminPharmaciesTab(allPharmacies, onUpdatePharmacyStatus)
                2 -> AdminRequestsTab(allRequests)
                3 -> AdminLogsTab(adminLogs)
            }
        }
    }
}

@Composable
private fun AdminPaymentsTab(
    payments: List<PaymentSubmission>,
    onVerifyPayment: (PaymentSubmission, Boolean, String) -> Unit
) {
    val extras = LocalOsExtraColors.current
    if (payments.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No Telebirr payment proofs submitted yet.", color = extras.muted)
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(payments, key = { it.id }) { payment ->
            var rejectionNote by remember(payment.id) { mutableStateOf("") }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, extras.border, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${payment.paymentProvider.uppercase()} · ${payment.paymentAmount.toInt()} ETB",
                            style = MaterialTheme.typography.titleMedium
                        )
                        StatusPill(payment.verificationStatus)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ref: ${payment.paymentReference} · Sender: ${payment.paymentPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.muted
                    )
                    Text(
                        text = "Recipient: ${payment.recipientNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.muted
                    )

                    if (payment.paymentScreenshotUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        AsyncImage(
                            model = remember(payment.paymentScreenshotUrl) {
                                StorageRepository.decodeImageModel(payment.paymentScreenshotUrl)
                            },
                            contentDescription = "Telebirr payment receipt",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (payment.verificationStatus == "PENDING") {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = rejectionNote,
                            onValueChange = { rejectionNote = it },
                            label = { Text("Optional rejection note") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onVerifyPayment(payment, true, "") },
                                colors = ButtonDefaults.buttonColors(containerColor = OsSuccess),
                                shape = CircleShape,
                                modifier = Modifier.weight(1f).height(46.dp).testTag("approve_payment_${payment.id}")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve & Unlock", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { onVerifyPayment(payment, false, rejectionNote) },
                                shape = CircleShape,
                                modifier = Modifier.weight(0.75f).height(46.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = extras.danger, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reject", color = extras.danger, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPharmaciesTab(
    pharmacies: List<Pharmacy>,
    onUpdatePharmacyStatus: (Pharmacy, String, String) -> Unit
) {
    val extras = LocalOsExtraColors.current
    if (pharmacies.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No pharmacies registered yet.", color = extras.muted)
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(pharmacies, key = { it.id }) { ph ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, extras.border, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ph.pharmacyName, style = MaterialTheme.typography.titleMedium)
                        StatusPill(ph.approvalStatus)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pharmacist: ${ph.pharmacistName} · ${ph.phoneNumber}", style = MaterialTheme.typography.bodySmall, color = extras.muted)
                    Text("License: ${ph.businessLicenseNumber} · ${ph.address}", style = MaterialTheme.typography.bodySmall, color = extras.muted)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!ph.isApproved) {
                            Button(
                                onClick = { onUpdatePharmacyStatus(ph, "APPROVED", "VERIFIED") },
                                colors = ButtonDefaults.buttonColors(containerColor = OsSuccess),
                                shape = CircleShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve", fontSize = 12.sp)
                            }
                        }
                        if (ph.approvalStatus == "PENDING") {
                            OutlinedButton(
                                onClick = { onUpdatePharmacyStatus(ph, "REJECTED", "REJECTED") },
                                shape = CircleShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject", color = extras.danger, fontSize = 12.sp)
                            }
                        }
                        if (ph.approvalStatus == "APPROVED") {
                            OutlinedButton(
                                onClick = { onUpdatePharmacyStatus(ph, "SUSPENDED", ph.licenseVerificationStatus) },
                                shape = CircleShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Block, contentDescription = null, tint = extras.danger, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Suspend", color = extras.danger, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRequestsTab(requests: List<PrescriptionRequest>) {
    val extras = LocalOsExtraColors.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(requests, key = { it.id }) { req ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, extras.border, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(req.patientName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${req.patientPhone} · ${req.deliveryMode} · Nearby notified: ${req.eligibleOwnerIds.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = extras.muted
                        )
                    }
                    StatusPill(req.requestStatus)
                }
            }
        }
    }
}

@Composable
private fun AdminLogsTab(logs: List<AdminLog>) {
    val extras = LocalOsExtraColors.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(logs, key = { it.id }) { log ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, extras.border, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(log.action, style = MaterialTheme.typography.labelMedium, color = OsPrimary)
                    Text(log.description, style = MaterialTheme.typography.bodySmall, color = extras.muted)
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    val extras = LocalOsExtraColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = accent)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = extras.muted)
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val (bg, fg) = when (status) {
        "APPROVED", "VERIFIED", "UNLOCKED", "DELIVERED", "IN_STOCK" -> OsSuccess.copy(alpha = 0.14f) to OsSuccess
        "PENDING", "LOW_STOCK", "RESPONDED", "OUT_FOR_DELIVERY" -> OsAccentGold.copy(alpha = 0.16f) to OsAccentGold
        else -> Color(0xFFB5423B).copy(alpha = 0.14f) to Color(0xFFB5423B)
    }
    Surface(color = bg, shape = CircleShape) {
        Text(
            text = status,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
