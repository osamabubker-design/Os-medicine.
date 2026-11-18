package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.models.ChatMessage
import com.example.data.models.MedicineItem
import com.example.data.models.Pharmacy
import com.example.data.models.PharmacyResponse
import com.example.data.models.PrescriptionRequest
import com.example.data.repositories.StorageRepository
import com.example.ui.theme.LocalOsExtraColors
import com.example.ui.theme.OsAccentGold
import com.example.ui.theme.OsCoral
import com.example.ui.theme.OsPrimary
import com.example.ui.theme.OsSuccess
import com.example.utils.AppStrings
import com.example.utils.GeoUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun PharmacyScreen(
    strings: AppStrings,
    myPharmacy: Pharmacy?,
    incomingRequests: List<PrescriptionRequest>,
    ownResponses: List<PharmacyResponse>,
    myMedicines: List<MedicineItem>,
    activeChatId: String?,
    activeChatMessages: List<ChatMessage>,
    onRegisterPharmacy: (
        pharmacyName: String,
        pharmacistName: String,
        phoneNumber: String,
        email: String,
        address: String,
        lat: Double,
        lng: Double,
        licenseNumber: String
    ) -> Unit,
    onUpdatePharmacyProfile: (
        pharmacyId: String,
        pharmacyName: String,
        pharmacistName: String,
        phoneNumber: String,
        address: String,
        availability: Boolean,
        lat: Double,
        lng: Double
    ) -> Unit,
    onRespondToRequest: (PrescriptionRequest, Pharmacy, Boolean) -> Unit,
    onUpdateResponseDetails: (String, String, Double, Long, Double, String) -> Unit,
    onSaveMedicine: (MedicineItem) -> Unit,
    onOpenChat: (requestId: String, pharmacyId: String) -> Unit,
    onSendChatMessage: (
        requestId: String,
        patientId: String,
        patientName: String,
        pharmacyId: String,
        pharmacyOwnerId: String,
        pharmacyName: String,
        text: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val extras = LocalOsExtraColors.current

    if (myPharmacy == null) {
        // Pharmacy Registration Form (fixed validation, duplicate prevention, no hardcoded passwords)
        var phName by remember { mutableStateOf("") }
        var pharmacistName by remember { mutableStateOf(Firebase.auth.currentUser?.displayName.orEmpty()) }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf(Firebase.auth.currentUser?.email.orEmpty()) }
        var address by remember { mutableStateOf("Addis Ababa") }
        var license by remember { mutableStateOf("") }
        var latText by remember { mutableStateOf(GeoUtils.DEFAULT_ADDIS_LAT.toString()) }
        var lngText by remember { mutableStateOf(GeoUtils.DEFAULT_ADDIS_LNG.toString()) }
        var validationError by remember { mutableStateOf<String?>(null) }

        @SuppressLint("MissingPermission")
        fun detectPharmacyGps() {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        latText = String.format("%.6f", loc.latitude)
                        lngText = String.format("%.6f", loc.longitude)
                    }
                }
        }

        val gpsLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grants ->
            if (grants.values.any { it }) detectPharmacyGps()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 560.dp)) {
                Text(text = strings.registerPharmacyTitle, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Register your licensed Ethiopian pharmacy to receive real-time patient prescriptions within 5 km.",
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.muted
                )
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, extras.border, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = phName,
                            onValueChange = { phName = it; validationError = null },
                            label = { Text("Pharmacy Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("reg_pharmacy_name")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = pharmacistName,
                            onValueChange = { pharmacistName = it; validationError = null },
                            label = { Text("Licensed Pharmacist Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("reg_pharmacist_name")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it; validationError = null },
                            label = { Text("Phone Number *") },
                            placeholder = { Text("0911 223 344") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("reg_pharmacy_phone")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; validationError = null },
                            label = { Text("Official Pharmacy Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it; validationError = null },
                            label = { Text("Street / Sub-City Address *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("reg_pharmacy_address")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = license,
                            onValueChange = { license = it; validationError = null },
                            label = { Text("EFDA / Business License Number *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("reg_pharmacy_license")
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = latText,
                                onValueChange = { latText = it },
                                label = { Text("Latitude") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lngText,
                                onValueChange = { lngText = it },
                                label = { Text("Longitude") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (granted) detectPharmacyGps()
                                else gpsLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            },
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use my current location (GPS)")
                        }

                        validationError?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(err, color = extras.danger, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val lat = latText.toDoubleOrNull()
                                val lng = lngText.toDoubleOrNull()
                                when {
                                    phName.trim().length < 2 ->
                                        validationError = "Pharmacy name must be at least 2 characters."
                                    pharmacistName.trim().length < 2 ->
                                        validationError = "Licensed pharmacist name is required."
                                    !GeoUtils.isValidEthiopianPhone(phone) ->
                                        validationError = "Enter a valid Ethiopian phone number (e.g. 0911223344)."
                                    address.trim().length < 2 ->
                                        validationError = "Pharmacy street address is required."
                                    license.trim().length < 2 ->
                                        validationError = "Business license number is required."
                                    lat == null || lng == null ->
                                        validationError = "Valid latitude and longitude coordinates are required."
                                    else -> onRegisterPharmacy(phName, pharmacistName, phone, email, address, lat, lng, license)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OsPrimary),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_pharmacy_reg_btn")
                        ) {
                            Text("Register Pharmacy Account", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val respondedIds = remember(ownResponses) { ownResponses.map { it.requestId }.toSet() }
    val newIncoming = incomingRequests.filter { it.id !in respondedIds }
    val nearbyCount = newIncoming.count {
        GeoUtils.haversineKm(myPharmacy.latitude, myPharmacy.longitude, it.latitude, it.longitude) <= GeoUtils.PHARMACY_SEARCH_RADIUS_KM
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp)) {
            // Pharmacy Header matching Screenshot_20261109_010128
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(OsPrimary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.LocalPharmacy, contentDescription = null, tint = OsPrimary)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(myPharmacy.pharmacyName, style = MaterialTheme.typography.titleMedium)
                            if (myPharmacy.isApproved) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = OsSuccess, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = when (myPharmacy.approvalStatus) {
                                "APPROVED" -> "Verified Partner"
                                "PENDING" -> "Pending Admin Verification"
                                else -> myPharmacy.approvalStatus
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (myPharmacy.isApproved) OsSuccess else OsAccentGold
                        )
                    }
                }
                OutlinedButton(onClick = onSignOut, shape = CircleShape) {
                    Text("Log out", fontSize = 12.sp)
                }
            }

            if (!myPharmacy.isApproved) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = OsAccentGold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.pendingVerificationBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = OsAccentGold,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(newIncoming.size.toString(), "New", OsPrimary, Modifier.weight(1f))
                StatChip(nearbyCount.toString(), "Nearby", OsCoral, Modifier.weight(1f))
                StatChip(ownResponses.size.toString(), "Replied", OsSuccess, Modifier.weight(1f))
                StatChip(String.format("%.1f ★", myPharmacy.averageRating), "Rating", OsAccentGold, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.incomingRequests) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.historyTab) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(strings.profileTab) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text(strings.inventoryTab) })
            }
            HorizontalDivider(color = extras.border)
            Spacer(modifier = Modifier.height(10.dp))

            when (selectedTab) {
                0 -> Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (newIncoming.isEmpty()) {
                        Text(
                            text = "No pending prescriptions within 5 km right now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = extras.muted,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        newIncoming.forEach { req ->
                            val distKm = GeoUtils.haversineKm(myPharmacy.latitude, myPharmacy.longitude, req.latitude, req.longitude)
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, extras.border, RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = remember(req.prescriptionImageUrl) {
                                            StorageRepository.decodeImageModel(req.prescriptionImageUrl)
                                        },
                                        contentDescription = "Patient prescription",
                                        modifier = Modifier.size(82.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("New Prescription Request", style = MaterialTheme.typography.titleSmall)
                                            Surface(color = OsPrimary.copy(alpha = 0.12f), shape = CircleShape) {
                                                Text(
                                                    text = String.format("%.1f km", distKm),
                                                    color = OsPrimary,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Mode: ${if (req.deliveryMode == "delivery") "🛵 Delivery" else "🚶 Pickup"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extras.muted
                                        )
                                        if (!req.medicineName.isNullOrBlank()) {
                                            Text("Note: ${req.medicineName}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onRespondToRequest(req, myPharmacy, true) },
                                                enabled = myPharmacy.isApproved,
                                                colors = ButtonDefaults.buttonColors(containerColor = OsSuccess),
                                                shape = CircleShape,
                                                modifier = Modifier.weight(1f).testTag("pharmacy_available_btn")
                                            ) {
                                                Text("Available", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            OutlinedButton(
                                                onClick = { onRespondToRequest(req, myPharmacy, false) },
                                                enabled = myPharmacy.isApproved,
                                                shape = CircleShape,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("No Stock", color = extras.danger, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                1 -> Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (ownResponses.isEmpty()) {
                        Text("No confirmed responses yet.", color = extras.muted, modifier = Modifier.padding(16.dp))
                    } else {
                        ownResponses.forEach { resp ->
                            var medName by remember(resp.id) { mutableStateOf(resp.medicineName) }
                            var priceText by remember(resp.id) { mutableStateOf(resp.medicinePrice.toInt().toString()) }
                            var qtyText by remember(resp.id) { mutableStateOf(resp.quantityAvailable.toString()) }
                            var feeText by remember(resp.id) { mutableStateOf(resp.deliveryFee.toInt().toString()) }
                            var chatText by remember { mutableStateOf("") }
                            val isChatOpen = activeChatId == "${resp.requestId}_${resp.pharmacyId}"

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, extras.border, RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(resp.medicineName, style = MaterialTheme.typography.titleMedium)
                                        StatusPill(if (resp.medicineAvailability) "AVAILABLE" else "NO_STOCK")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = medName,
                                        onValueChange = { medName = it },
                                        label = { Text("Confirmed Medicine & Pack") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = priceText,
                                            onValueChange = { priceText = it },
                                            label = { Text("Price (ETB)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = qtyText,
                                            onValueChange = { qtyText = it },
                                            label = { Text("Qty") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(0.7f)
                                        )
                                        OutlinedTextField(
                                            value = feeText,
                                            onValueChange = { feeText = it },
                                            label = { Text("Rider Fee") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                onUpdateResponseDetails(
                                                    resp.id,
                                                    medName,
                                                    priceText.toDoubleOrNull() ?: 0.0,
                                                    qtyText.toLongOrNull() ?: 1L,
                                                    feeText.toDoubleOrNull() ?: 40.0,
                                                    resp.deliveryStatus
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = OsPrimary),
                                            shape = CircleShape,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save Details", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { onOpenChat(resp.requestId, resp.pharmacyId) },
                                            shape = CircleShape,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Patient Chat", fontSize = 12.sp)
                                        }
                                    }

                                    if (isChatOpen) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        activeChatMessages.forEach { msg ->
                                            val fromMe = msg.senderRole == "pharmacy"
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start
                                            ) {
                                                Surface(
                                                    color = if (fromMe) OsPrimary else MaterialTheme.colorScheme.background,
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text(
                                                        text = msg.message,
                                                        color = if (fromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = chatText,
                                                onValueChange = { chatText = it },
                                                placeholder = { Text("Reply to patient...") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (chatText.isNotBlank()) {
                                                        onSendChatMessage(
                                                            resp.requestId,
                                                            resp.patientId,
                                                            "Patient",
                                                            resp.pharmacyId,
                                                            resp.pharmacyOwnerId,
                                                            resp.pharmacyName,
                                                            chatText
                                                        )
                                                        chatText = ""
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = OsPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                2 -> {
                    var name by remember(myPharmacy.id) { mutableStateOf(myPharmacy.pharmacyName) }
                    var pharmacist by remember(myPharmacy.id) { mutableStateOf(myPharmacy.pharmacistName) }
                    var phone by remember(myPharmacy.id) { mutableStateOf(myPharmacy.phoneNumber) }
                    var address by remember(myPharmacy.id) { mutableStateOf(myPharmacy.address) }
                    var avail by remember(myPharmacy.id) { mutableStateOf(myPharmacy.availability) }
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Accepting Requests Open Status", style = MaterialTheme.typography.titleSmall)
                            Switch(checked = avail, onCheckedChange = { avail = it })
                        }
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Pharmacy Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = pharmacist, onValueChange = { pharmacist = it }, label = { Text("Pharmacist Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                onUpdatePharmacyProfile(
                                    myPharmacy.id, name, pharmacist, phone, address, avail,
                                    myPharmacy.latitude, myPharmacy.longitude
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OsPrimary),
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                3 -> {
                    var medName by remember { mutableStateOf("") }
                    var brand by remember { mutableStateOf("") }
                    var dosage by remember { mutableStateOf("500mg") }
                    var category by remember { mutableStateOf("Antibiotics") }
                    var qtyText by remember { mutableStateOf("10") }
                    var priceText by remember { mutableStateOf("150") }
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Add Medicine Stock", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = medName, onValueChange = { medName = it }, label = { Text("Medicine Name *") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = dosage, onValueChange = { dosage = it }, label = { Text("Dosage") }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = qtyText, onValueChange = { qtyText = it }, label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Price (ETB)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = {
                                if (medName.trim().isNotEmpty()) {
                                    onSaveMedicine(
                                        MedicineItem(
                                            medicineName = medName.trim(),
                                            brand = brand.trim(),
                                            category = category,
                                            dosage = dosage.trim(),
                                            pharmacyId = myPharmacy.id,
                                            ownerId = myPharmacy.ownerId,
                                            pharmacyName = myPharmacy.pharmacyName,
                                            quantityAvailable = qtyText.toLongOrNull() ?: 0L,
                                            price = priceText.toDoubleOrNull() ?: 0.0,
                                            pharmacyApproved = myPharmacy.isApproved
                                        )
                                    )
                                    medName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OsPrimary),
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Medicine to Inventory", fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = extras.border)
                        myMedicines.forEach { med ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, extras.border, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${med.medicineName} (${med.dosage})", style = MaterialTheme.typography.titleSmall)
                                        Text("Qty: ${med.quantityAvailable} · ${med.price} ETB", style = MaterialTheme.typography.bodySmall, color = extras.muted)
                                    }
                                    StatusPill(med.stockStatus)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    val extras = LocalOsExtraColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accent)
            Text(text = label, fontSize = 11.sp, color = extras.muted)
        }
    }
}
