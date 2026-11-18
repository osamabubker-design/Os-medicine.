package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.models.AppUser
import com.example.data.models.ChatMessage
import com.example.data.models.MedicineItem
import com.example.data.models.PaymentSubmission
import com.example.data.models.Pharmacy
import com.example.data.models.PharmacyResponse
import com.example.data.models.PrescriptionRequest
import com.example.data.repositories.PaymentRepository
import com.example.data.repositories.StorageRepository
import com.example.data.repositories.UploadState
import com.example.ui.components.LockedLocationMapCard
import com.example.ui.components.UnlockedMapDirectionsCard
import com.example.ui.theme.LocalOsExtraColors
import com.example.ui.theme.OsAccentGold
import com.example.ui.theme.OsCoral
import com.example.ui.theme.OsPrimary
import com.example.ui.theme.OsSuccess
import com.example.utils.AppStrings
import com.example.utils.GeoUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun PatientScreen(
    strings: AppStrings,
    currentUser: AppUser?,
    approvedPharmacies: List<Pharmacy>,
    approvedMedicines: List<MedicineItem>,
    myRequests: List<PrescriptionRequest>,
    patientResponses: List<PharmacyResponse>,
    myPayments: List<PaymentSubmission>,
    uploadState: UploadState,
    activeChatId: String?,
    activeChatMessages: List<ChatMessage>,
    onUploadPrescription: (Uri) -> Unit,
    onAnalyzeRxWithAi: (Uri) -> Unit,
    onSubmitRequest: (
        name: String,
        phone: String,
        imageUrl: String,
        medicineName: String?,
        mode: String,
        address: String?,
        lat: Double,
        lng: Double,
        onCreated: (String) -> Unit
    ) -> Unit,
    onOpenPaymentForRequest: (String) -> Unit,
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
    onRatePharmacy: (Pharmacy, String, Long, String) -> Unit
) {
    val context = LocalContext.current
    val extras = LocalOsExtraColors.current

    var subTab by remember { mutableIntStateOf(0) }
    var patientName by remember { mutableStateOf(currentUser?.fullName.orEmpty()) }
    var patientPhone by remember { mutableStateOf(currentUser?.phoneNumber.orEmpty()) }
    var medicineHint by remember { mutableStateOf("") }
    var deliveryMode by remember { mutableStateOf("pickup") }
    var deliveryAddress by remember { mutableStateOf(currentUser?.address.orEmpty()) }
    var latitude by remember { mutableDoubleStateOf(GeoUtils.DEFAULT_ADDIS_LAT) }
    var longitude by remember { mutableDoubleStateOf(GeoUtils.DEFAULT_ADDIS_LNG) }
    var gpsStatus by remember { mutableStateOf("Addis Ababa · 5 km search radius ready") }
    var selectedRxUri by remember { mutableStateOf<Uri?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var activeRequestId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser) {
        if (patientName.isBlank() && !currentUser?.fullName.isNullOrBlank()) {
            patientName = currentUser!!.fullName
        }
        if (patientPhone.isBlank() && !currentUser?.phoneNumber.isNullOrBlank()) {
            patientPhone = currentUser!!.phoneNumber
        }
    }

    LaunchedEffect(myRequests) {
        if (activeRequestId == null && myRequests.isNotEmpty()) {
            activeRequestId = myRequests.first().id
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchGpsLocation() {
        val client = LocationServices.getFusedLocationProviderClient(context)
        gpsStatus = "Detecting current GPS coordinates..."
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    gpsStatus = "GPS locked (${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)})"
                } else {
                    client.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            latitude = last.latitude
                            longitude = last.longitude
                            gpsStatus = "GPS locked (${String.format("%.4f", last.latitude)}, ${String.format("%.4f", last.longitude)})"
                        } else {
                            gpsStatus = "GPS unavailable on emulator — using Addis Ababa center (${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)})"
                        }
                    }.addOnFailureListener {
                        gpsStatus = "Using Addis Ababa center (${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)})"
                    }
                }
            }
            .addOnFailureListener {
                gpsStatus = "Using Addis Ababa center (${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)})"
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchGpsLocation()
        } else {
            gpsStatus = "Location permission denied. Using Addis Ababa reference coordinates."
        }
    }

    val rxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedRxUri = uri
            formError = null
            onUploadPrescription(uri)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = subTab, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text(strings.tabFindMedicine, maxLines = 1) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("${strings.tabMyRequests} (${myRequests.size})", maxLines = 1) }
            )
            Tab(
                selected = subTab == 2,
                onClick = { subTab = 2 },
                text = { Text(strings.tabMedicines, maxLines = 1) }
            )
        }
        HorizontalDivider(color = extras.border)

        when (subTab) {
            0 -> {
                val activeReq = myRequests.firstOrNull { it.id == activeRequestId } ?: myRequests.firstOrNull()
                val responsesForReq = patientResponses.filter { it.requestId == activeReq?.id }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(modifier = Modifier.widthIn(max = 600.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, extras.border, RoundedCornerShape(18.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = patientName,
                                    onValueChange = { patientName = it; formError = null },
                                    label = { Text(strings.yourName) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("patient_name_input")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = patientPhone,
                                    onValueChange = { patientPhone = it; formError = null },
                                    label = { Text(strings.phoneNumber) },
                                    placeholder = { Text("0911 223 344") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("patient_phone_input")
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = medicineHint,
                                    onValueChange = { medicineHint = it },
                                    label = { Text("Medicine name (optional if clear in photo)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = strings.prescriptionPhoto, style = MaterialTheme.typography.labelLarge)
                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            1.5.dp,
                                            if (uploadState is UploadState.Success) OsSuccess else extras.border,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            rxPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                        .padding(14.dp)
                                        .testTag("prescription_photo_picker"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (selectedRxUri != null) {
                                            AsyncImage(
                                                model = selectedRxUri,
                                                contentDescription = "Prescription preview",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(150.dp)
                                                    .clip(RoundedCornerShape(10.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.AddPhotoAlternate,
                                                contentDescription = "Upload prescription",
                                                tint = OsPrimary,
                                                modifier = Modifier.size(34.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                        Text(
                                            text = when (uploadState) {
                                                is UploadState.Uploading -> "Preparing & uploading photo (${uploadState.progressPercent}%)..."
                                                is UploadState.Success -> "Prescription photo ready"
                                                is UploadState.Error -> "Tap to retry photo upload"
                                                UploadState.Idle -> strings.tapToUploadRx
                                            },
                                            color = if (uploadState is UploadState.Error) extras.danger else OsPrimary,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                if (uploadState is UploadState.Uploading) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { uploadState.progressPercent / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (uploadState is UploadState.Error) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(uploadState.message, color = extras.danger, style = MaterialTheme.typography.bodySmall)
                                    if (selectedRxUri != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = { selectedRxUri?.let(onUploadPrescription) },
                                            shape = CircleShape,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Retry Photo Upload")
                                        }
                                    }
                                }

                                if (selectedRxUri != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { selectedRxUri?.let(onAnalyzeRxWithAi) },
                                        shape = CircleShape,
                                        modifier = Modifier.fillMaxWidth().testTag("ai_read_prescription_btn")
                                    ) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = OsPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AI Read Prescription", color = OsPrimary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(strings.yourLocation, style = MaterialTheme.typography.labelLarge)
                                        Text(gpsStatus, style = MaterialTheme.typography.bodySmall, color = extras.muted)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                                            if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                                                fetchGpsLocation()
                                            } else {
                                                locationPermissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            }
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.testTag("patient_gps_btn")
                                    ) {
                                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.useGps)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text(strings.howDoYouWantIt, style = MaterialTheme.typography.labelLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ModeChip(
                                        label = strings.pickup,
                                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                                        selected = deliveryMode == "pickup",
                                        modifier = Modifier.weight(1f).testTag("mode_pickup_btn"),
                                        onClick = { deliveryMode = "pickup" }
                                    )
                                    ModeChip(
                                        label = strings.delivery,
                                        icon = Icons.Filled.DeliveryDining,
                                        selected = deliveryMode == "delivery",
                                        modifier = Modifier.weight(1f).testTag("mode_delivery_btn"),
                                        onClick = { deliveryMode = "delivery" }
                                    )
                                }

                                if (deliveryMode == "delivery") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = deliveryAddress,
                                        onValueChange = { deliveryAddress = it },
                                        label = { Text(strings.deliveryAddressHint) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                formError?.let { err ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(err, color = extras.danger, style = MaterialTheme.typography.bodySmall)
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        val uploadedUrl = (uploadState as? UploadState.Success)?.downloadUrl
                                        when {
                                            patientName.trim().length < 2 ->
                                                formError = "Please enter your full name."
                                            !GeoUtils.isValidEthiopianPhone(patientPhone) ->
                                                formError = "Enter a valid Ethiopian phone number (e.g. 0911223344)."
                                            uploadedUrl.isNullOrBlank() ->
                                                formError = "Please upload your prescription photo first."
                                            deliveryMode == "delivery" && deliveryAddress.trim().length < 3 ->
                                                formError = "Please enter your delivery address."
                                            else -> {
                                                onSubmitRequest(
                                                    patientName,
                                                    patientPhone,
                                                    uploadedUrl,
                                                    medicineHint.ifBlank { null },
                                                    deliveryMode,
                                                    deliveryAddress.ifBlank { null },
                                                    latitude,
                                                    longitude
                                                ) { createdId ->
                                                    activeRequestId = createdId
                                                }
                                            }
                                        }
                                    },
                                    enabled = uploadState !is UploadState.Uploading,
                                    colors = ButtonDefaults.buttonColors(containerColor = OsCoral),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("broadcast_prescription_btn")
                                ) {
                                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.askNearbyPharmacies, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "💡 ${strings.noUpfrontNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extras.muted
                                )
                            }
                        }

                        if (activeReq != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            RadarActiveCard(
                                strings = strings,
                                eligibleCount = activeReq.eligibleOwnerIds.size,
                                paymentUnlocked = activeReq.paymentUnlocked
                            )

                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = strings.pharmacyResponses,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (responsesForReq.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, extras.border, RoundedCornerShape(14.dp))
                                ) {
                                    Text(
                                        text = "Waiting for nearby verified pharmacies within 5 km to confirm stock...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extras.muted,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                responsesForReq.forEach { resp ->
                                    val matchingPharmacy = approvedPharmacies.firstOrNull { it.id == resp.pharmacyId }
                                    val matchingPayment = myPayments.firstOrNull { it.requestId == activeReq.id }
                                    PharmacyResponseCard(
                                        strings = strings,
                                        request = activeReq,
                                        response = resp,
                                        pharmacy = matchingPharmacy,
                                        payment = matchingPayment,
                                        isChatOpen = activeChatId == "${activeReq.id}_${resp.pharmacyId}",
                                        chatMessages = activeChatMessages,
                                        onUnlockClick = { onOpenPaymentForRequest(activeReq.id) },
                                        onOpenChat = { onOpenChat(activeReq.id, resp.pharmacyId) },
                                        onSendChat = { msg ->
                                            onSendChatMessage(
                                                activeReq.id,
                                                activeReq.patientId,
                                                activeReq.patientName,
                                                resp.pharmacyId,
                                                resp.pharmacyOwnerId,
                                                resp.pharmacyName,
                                                msg
                                            )
                                        },
                                        onRate = { score, comment ->
                                            val target = matchingPharmacy ?: Pharmacy(
                                                id = resp.pharmacyId,
                                                ownerId = resp.pharmacyOwnerId,
                                                pharmacyName = resp.pharmacyName
                                            )
                                            onRatePharmacy(target, activeReq.id, score, comment)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            1 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(modifier = Modifier.widthIn(max = 600.dp)) {
                        if (myRequests.isEmpty()) {
                            Text("No prescription requests submitted yet.", color = extras.muted)
                        } else {
                            myRequests.forEach { req ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, extras.border, RoundedCornerShape(14.dp))
                                        .clickable {
                                            activeRequestId = req.id
                                            subTab = 0
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = remember(req.prescriptionImageUrl) {
                                                StorageRepository.decodeImageModel(req.prescriptionImageUrl)
                                            },
                                            contentDescription = "Prescription",
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = req.medicineName?.ifBlank { null } ?: "Prescription Request",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = "${req.deliveryMode.uppercase()} · ${req.eligibleOwnerIds.size} nearby pharmacies",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = extras.muted
                                            )
                                        }
                                        StatusPill(req.requestStatus)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            2 -> {
                var query by remember { mutableStateOf("") }
                val filtered = approvedMedicines.filter {
                    query.isBlank() ||
                        it.medicineName.contains(query, ignoreCase = true) ||
                        it.brand.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(modifier = Modifier.widthIn(max = 600.dp)) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Search medicines, brand, or category") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("medicine_search_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (filtered.isEmpty()) {
                            Text(
                                text = "No matching listed medicines from verified pharmacies yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = extras.muted
                            )
                        } else {
                            filtered.forEach { med ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, extras.border, RoundedCornerShape(14.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(med.medicineName, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = "${med.brand} · ${med.dosage} · ${med.pharmacyName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = extras.muted
                                            )
                                            Text(
                                                text = "Price: ${med.price} ETB · Qty: ${med.quantityAvailable}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = OsPrimary
                                            )
                                        }
                                        StatusPill(med.stockStatus)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val extras = LocalOsExtraColors.current
    Surface(
        color = if (selected) OsPrimary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun RadarActiveCard(
    strings: AppStrings,
    eligibleCount: Int,
    paymentUnlocked: Boolean
) {
    val extras = LocalOsExtraColors.current
    val pulse by rememberInfiniteTransition(label = "radar").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, extras.border, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = OsPrimary.copy(alpha = 0.12f),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (paymentUnlocked) "✓ Location Unlocked" else "📡 Free Search Active · $eligibleCount Nearby",
                        color = OsPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = if (paymentUnlocked) "Unlocked" else "Pending Unlock",
                    style = MaterialTheme.typography.labelSmall,
                    color = extras.muted
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(OsPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OsPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalPharmacy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = strings.searchingNearby,
                style = MaterialTheme.typography.titleSmall,
                color = OsPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.checkingReplies,
                style = MaterialTheme.typography.bodySmall,
                color = extras.muted
            )
        }
    }
}

@Composable
private fun PharmacyResponseCard(
    strings: AppStrings,
    request: PrescriptionRequest,
    response: PharmacyResponse,
    pharmacy: Pharmacy?,
    payment: PaymentSubmission?,
    isChatOpen: Boolean,
    chatMessages: List<ChatMessage>,
    onUnlockClick: () -> Unit,
    onOpenChat: () -> Unit,
    onSendChat: (String) -> Unit,
    onRate: (Long, String) -> Unit
) {
    val context = LocalContext.current
    val extras = LocalOsExtraColors.current
    val distanceKm = GeoUtils.haversineKm(
        request.latitude, request.longitude,
        response.pharmacyLat, response.pharmacyLng
    )
    var chatInput by remember { mutableStateOf("") }
    var ratingExpanded by remember { mutableStateOf(false) }
    var ratingScore by remember { mutableLongStateOf(5L) }
    var ratingComment by remember { mutableStateOf("") }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OsSuccess.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (request.paymentUnlocked) response.pharmacyName else "${response.pharmacyName} (Stock Reserved)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (request.paymentUnlocked) {
                            "📍 ${response.pharmacyAddress}"
                        } else {
                            "📍 Exact street & Google Maps pin locked 🔒"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.muted
                    )
                }
                Surface(
                    color = if (response.medicineAvailability) OsSuccess.copy(alpha = 0.14f) else extras.danger.copy(alpha = 0.14f),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (response.medicineAvailability) "✓ ${strings.availableBadge}" else strings.notAvailableBadge,
                        color = if (response.medicineAvailability) OsSuccess else extras.danger,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "💊 Medicine: ${response.medicineName} (${response.quantityAvailable} Pack / Strip)" +
                    if (response.medicinePrice > 0.0) " · ${response.medicinePrice.toInt()} ETB" else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (response.responseMessage.isNotBlank()) {
                Text(response.responseMessage, style = MaterialTheme.typography.bodySmall, color = extras.muted)
            }

            if (response.medicineAvailability) {
                Spacer(modifier = Modifier.height(10.dp))
                if (!request.paymentUnlocked) {
                    LockedLocationMapCard(
                        distanceKm = distanceKm,
                        unlockButtonLabel = strings.unlockLocationBtn,
                        onUnlockClick = onUnlockClick
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (payment?.verificationStatus == "PENDING") {
                        Surface(
                            color = OsAccentGold.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⏳ Payment ref ${payment.paymentReference} is awaiting admin verification...",
                                style = MaterialTheme.typography.bodySmall,
                                color = OsAccentGold,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onUnlockClick,
                            colors = ButtonDefaults.buttonColors(containerColor = OsCoral),
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pay 50 ETB to telebirr (${PaymentRepository.OFFICIAL_TELEBIRR_LOCAL}) to Reveal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenChat,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.chatWithPharmacy)
                    }
                } else {
                    UnlockedMapDirectionsCard(
                        pharmacyName = response.pharmacyName,
                        address = response.pharmacyAddress,
                        distanceKm = distanceKm,
                        directionsButtonLabel = strings.startDirections,
                        onDirectionsClick = {
                            GeoUtils.openGoogleMapsDirections(
                                context,
                                response.pharmacyLat,
                                response.pharmacyLng,
                                response.pharmacyName
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { ratingExpanded = !ratingExpanded }.padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = OsAccentGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.rateThisPharmacy, color = OsAccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    if (ratingExpanded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            (1L..5L).forEach { star ->
                                IconButton(onClick = { ratingScore = star }) {
                                    Icon(
                                        imageVector = if (star <= ratingScore) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "Rate $star stars",
                                        tint = OsAccentGold
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = ratingComment,
                            onValueChange = { ratingComment = it },
                            label = { Text("Comment about pharmacy service") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                onRate(ratingScore, ratingComment)
                                ratingExpanded = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OsPrimary),
                            shape = CircleShape
                        ) {
                            Text("Submit Rating")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { GeoUtils.dialPhone(context, response.pharmacyPhone) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, tint = OsPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.callBtn, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                GeoUtils.openGoogleMapsDirections(
                                    context,
                                    response.pharmacyLat,
                                    response.pharmacyLng,
                                    response.pharmacyName
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.Directions, contentDescription = null, tint = OsPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.directionsBtn, fontSize = 12.sp, maxLines = 1)
                        }
                        Button(
                            onClick = onOpenChat,
                            colors = ButtonDefaults.buttonColors(containerColor = OsPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.chatBtn, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (isChatOpen) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, extras.border, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        chatMessages.forEach { msg ->
                            val isPatient = msg.senderRole == "patient"
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = if (isPatient) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (isPatient) OsPrimary else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = msg.message,
                                        color = if (isPatient) Color.White else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text("Message ${response.pharmacyName}...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (chatInput.isNotBlank()) {
                                        onSendChat(chatInput)
                                        chatInput = ""
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
}
