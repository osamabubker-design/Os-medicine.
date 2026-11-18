package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.unwrapFirestoreMessage
import com.example.data.models.AdminLog
import com.example.data.models.AppNotification
import com.example.data.models.AppUser
import com.example.data.models.ChatMessage
import com.example.data.models.MedicineItem
import com.example.data.models.PaymentSubmission
import com.example.data.models.Pharmacy
import com.example.data.models.PharmacyRating
import com.example.data.models.PharmacyResponse
import com.example.data.models.PrescriptionRequest
import com.example.data.repositories.AiMedicineRepository
import com.example.data.repositories.ChatRepository
import com.example.data.repositories.NotificationRepository
import com.example.data.repositories.PaymentRepository
import com.example.data.repositories.PharmacyRepository
import com.example.data.repositories.PrescriptionRepository
import com.example.data.repositories.StorageRepository
import com.example.data.repositories.UploadState
import com.example.data.repositories.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class MainNavTab { HOME, PATIENT, PHARMACY, ADMIN }

data class AiChatItem(val isUser: Boolean, val text: String)

sealed interface ActionUiState {
    data object Idle : ActionUiState
    data class Loading(val message: String = "Processing...") : ActionUiState
    data class Success(val message: String) : ActionUiState
    data class Error(val message: String) : ActionUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class OsMedicineViewModel(
    private val currentUserId: String,
    private val userRepo: UserRepository,
    private val pharmacyRepo: PharmacyRepository,
    private val prescriptionRepo: PrescriptionRepository,
    private val paymentRepo: PaymentRepository,
    private val chatRepo: ChatRepository,
    private val notificationRepo: NotificationRepository,
    private val storageRepo: StorageRepository,
    private val aiRepo: AiMedicineRepository
) : ViewModel() {

    private val _activeTab = MutableStateFlow(MainNavTab.HOME)
    val activeTab: StateFlow<MainNavTab> = _activeTab.asStateFlow()

    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _darkTheme = MutableStateFlow(false)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _adminSessionVerified = MutableStateFlow(false)
    val adminSessionVerified: StateFlow<Boolean> = _adminSessionVerified.asStateFlow()

    private val _actionState = MutableStateFlow<ActionUiState>(ActionUiState.Idle)
    val actionState: StateFlow<ActionUiState> = _actionState.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _paymentUploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val paymentUploadState: StateFlow<UploadState> = _paymentUploadState.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiChatItem>>(emptyList())
    val aiMessages: StateFlow<List<AiChatItem>> = _aiMessages.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    val currentUserProfile: StateFlow<AppUser?> = userRepo.observeCurrentUser()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Error observing user profile: ${unwrapFirestoreMessage(e, "")}")
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val approvedPharmacies: StateFlow<List<Pharmacy>> = pharmacyRepo.observeApprovedPharmacies()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Error observing approved pharmacies", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myPharmacy: StateFlow<Pharmacy?> = pharmacyRepo.observeMyPharmacy()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Error observing my pharmacy", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val approvedMedicines: StateFlow<List<MedicineItem>> = pharmacyRepo.observeApprovedMedicines()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myPharmacyMedicines: StateFlow<List<MedicineItem>> = myPharmacy
        .flatMapLatest { ph ->
            if (ph == null) flowOf(emptyList()) else pharmacyRepo.observePharmacyMedicines(ph.id)
        }
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myPrescriptionRequests: StateFlow<List<PrescriptionRequest>> = prescriptionRepo.observeMyPrescriptionRequests()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Error observing patient requests", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val incomingPharmacyRequests: StateFlow<List<PrescriptionRequest>> = prescriptionRepo.observeIncomingPharmacyRequests()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Error observing incoming pharmacy requests", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val patientResponses: StateFlow<List<PharmacyResponse>> = prescriptionRepo.observeResponsesForPatient()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val pharmacyOwnResponses: StateFlow<List<PharmacyResponse>> = prescriptionRepo.observeResponsesByPharmacy()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myPayments: StateFlow<List<PaymentSubmission>> = paymentRepo.observeMyPayments()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val publicRatings: StateFlow<List<PharmacyRating>> = prescriptionRepo.observeAllRatings()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId.isNullOrBlank()) flowOf(emptyList()) else chatRepo.observeMessages(chatId)
        }
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Admin streams (gated strictly by _adminSessionVerified)
    val adminAllPharmacies: StateFlow<List<Pharmacy>> = _adminSessionVerified
        .flatMapLatest { verified ->
            if (!verified) flowOf(emptyList()) else pharmacyRepo.observeAllPharmaciesAdmin()
        }
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminAllRequests: StateFlow<List<PrescriptionRequest>> = _adminSessionVerified
        .flatMapLatest { verified ->
            if (!verified) flowOf(emptyList()) else prescriptionRepo.observeAllRequestsAdmin()
        }
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminAllPayments: StateFlow<List<PaymentSubmission>> = _adminSessionVerified
        .flatMapLatest { verified ->
            if (!verified) flowOf(emptyList()) else paymentRepo.observeAllPaymentsAdmin()
        }
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminLogs: StateFlow<List<AdminLog>> = _adminSessionVerified
        .flatMapLatest { verified ->
            if (!verified) flowOf(emptyList()) else notificationRepo.observeAdminLogs()
        }
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val userNotifications: StateFlow<List<AppNotification>> = notificationRepo.observeUserNotifications()
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminNotifications: StateFlow<List<AppNotification>> = _adminSessionVerified
        .flatMapLatest { verified ->
            if (!verified) flowOf(emptyList()) else notificationRepo.observeAdminNotifications()
        }
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val unreadNotificationCount: StateFlow<Int> = userNotifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    init {
        viewModelScope.launch {
            val authUser = Firebase.auth.currentUser
            userRepo.ensureUserProfile(
                fullName = authUser?.displayName ?: "OS Medicine User",
                email = authUser?.email ?: ""
            )
            _isAdmin.value = userRepo.checkIsAdmin()
            try {
                val token = Firebase.messaging.token.await()
                if (!token.isNullOrBlank()) {
                    userRepo.updateFcmToken(token)
                }
            } catch (e: Exception) {
                Log.w(TAG, "FCM token registration skipped/unavailable", e)
            }
        }
    }

    fun selectTab(tab: MainNavTab) {
        _activeTab.value = tab
    }

    fun toggleLanguage() {
        _language.value = if (_language.value == "en") "am" else "en"
    }

    fun toggleDarkTheme() {
        _darkTheme.value = !_darkTheme.value
    }

    fun clearActionState() {
        _actionState.value = ActionUiState.Idle
    }

    fun openChat(requestId: String, pharmacyId: String) {
        _activeChatId.value = "${requestId}_${pharmacyId}"
    }

    fun closeChat() {
        _activeChatId.value = null
    }

    fun uploadPrescriptionPhoto(uri: Uri) {
        viewModelScope.launch {
            storageRepo.uploadImage("prescriptions", uri).collect { state ->
                _uploadState.value = state
            }
        }
    }

    fun resetPrescriptionUpload() {
        _uploadState.value = UploadState.Idle
    }

    fun uploadPaymentScreenshot(uri: Uri) {
        viewModelScope.launch {
            storageRepo.uploadImage("payments", uri).collect { state ->
                _paymentUploadState.value = state
            }
        }
    }

    fun resetPaymentUpload() {
        _paymentUploadState.value = UploadState.Idle
    }

    fun submitPrescriptionRequest(
        patientName: String,
        patientPhone: String,
        prescriptionImageUrl: String,
        medicineName: String?,
        deliveryMode: String,
        deliveryAddress: String?,
        latitude: Double,
        longitude: Double,
        onSubmitted: (String) -> Unit
    ) {
        viewModelScope.launch {
            _actionState.value = ActionUiState.Loading("Broadcasting prescription to nearby pharmacies...")
            try {
                userRepo.updateUserProfile(
                    fullName = patientName,
                    phoneNumber = patientPhone,
                    address = deliveryAddress.orEmpty()
                )
                val requestId = prescriptionRepo.createPrescriptionRequest(
                    patientName = patientName,
                    patientPhone = patientPhone,
                    prescriptionImageUrl = prescriptionImageUrl,
                    medicineName = medicineName,
                    deliveryMode = deliveryMode,
                    deliveryAddress = deliveryAddress,
                    latitude = latitude,
                    longitude = longitude,
                    nearbyApprovedPharmacies = approvedPharmacies.value
                )
                _actionState.value = ActionUiState.Success("Prescription broadcast to nearby pharmacies!")
                onSubmitted(requestId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = ActionUiState.Error(
                    unwrapFirestoreMessage(e, "Failed to submit prescription request.")
                )
            }
        }
    }

    fun submitTelebirrPayment(
        requestId: String,
        provider: String,
        paymentPhone: String,
        paymentReference: String,
        screenshotUrl: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _actionState.value = ActionUiState.Loading("Submitting payment proof for admin verification...")
            try {
                val paymentId = paymentRepo.submitPaymentProof(
                    requestId = requestId,
                    provider = provider,
                    paymentPhone = paymentPhone,
                    paymentReference = paymentReference,
                    screenshotUrl = screenshotUrl
                )
                _actionState.value = ActionUiState.Success("Payment submitted! Awaiting admin approval.")
                onSuccess(paymentId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = ActionUiState.Error(
                    unwrapFirestoreMessage(e, "Payment submission failed.")
                )
            }
        }
    }

    fun sendChatMessage(
        requestId: String,
        patientId: String,
        patientName: String,
        pharmacyId: String,
        pharmacyOwnerId: String,
        pharmacyName: String,
        senderRole: String,
        text: String
    ) {
        viewModelScope.launch {
            try {
                chatRepo.sendMessage(
                    requestId = requestId,
                    patientId = patientId,
                    patientName = patientName,
                    pharmacyId = pharmacyId,
                    pharmacyOwnerId = pharmacyOwnerId,
                    pharmacyName = pharmacyName,
                    senderRole = senderRole,
                    text = text
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = ActionUiState.Error(
                    unwrapFirestoreMessage(e, "Failed to send message.")
                )
            }
        }
    }

    fun ratePharmacy(pharmacy: Pharmacy, requestId: String, score: Long, comment: String) {
        viewModelScope.launch {
            try {
                prescriptionRepo.submitRating(pharmacy, requestId, score, comment)
                _actionState.value = ActionUiState.Success("Thank you for rating ${pharmacy.pharmacyName}!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = ActionUiState.Error(
                    unwrapFirestoreMessage(e, "Failed to submit rating.")
                )
            }
        }
    }

    fun registerPharmacy(
        pharmacyName: String,
        pharmacistName: String,
        phoneNumber: String,
        email: String,
        address: String,
        latitude: Double,
        longitude: Double,
        businessLicenseNumber: String
    ) {
        viewModelScope.launch {
            _actionState.value = ActionUiState.Loading("Registering pharmacy with Firebase...")
            try {
                pharmacyRepo.registerPharmacy(
                    pharmacyName = pharmacyName,
                    pharmacistName = pharmacistName,
                    phoneNumber = phoneNumber,
                    email = email,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                    businessLicenseNumber = businessLicenseNumber
                )
                _actionState.value = ActionUiState.Success("Pharmacy registered! Pending admin verification.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = ActionUiState.Error(
                    unwrapFirestoreMessage(e, e.message ?: "Pharmacy registration failed.")
                )
            }
        }
    }

    fun updatePharmacyProfile(
        pharmacyId: String,
        pharmacyName: String,
        pharmacistName: String,
        phoneNumber: String,
        address: String,
        availability: Boolean,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            _actionState.value = ActionUiState.Loading("Updating pharmacy profile...")
            try {
                pharmacyRepo.updatePharmacyProfile(
                    pharmacyId, pharmacyName, pharmacistName, phoneNumber, address, availability, latitude, longitude
                )
                _actionState.value = ActionUiState.Success("Pharmacy profile updated.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = ActionUiState.Error(
                    unwrapFirestoreMessage(e, "Failed to update pharmacy profile.")
                )
            }
        }
    }

    fun respondToPrescription(
        request: PrescriptionRequest,
        pharmacy: Pharmacy,
        available: Boolean,
        medicineName: String = "Prescription Stock Confirmed",
        medicinePrice: Double = 0.0,
        quantityAvailable: Long = 1L,
        responseMessage: String = "",
        deliveryFee: Double = 40.0,
        estimatedDeliveryTime: String = "30-45 mins"
    ) {
        viewModelScope.launch {
            _actionState.value = ActionUiState.Loading("Sending response to patient...")
            try {
                prescriptionRepo.respondToRequest(
                    request, pharmacy, available, medicineName, medicinePrice,
                    quantityAvailable, responseMessage, deliveryFee, estimatedDeliveryTime
                )
                _actionState.value = ActionUiState.Success(
                    if (available) "Marked available — patient notified!" else "Marked out of stock."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = ActionUiState.Error(
                    unwrapFirestoreMessage(e, "Could not send response.")
                )
            }
        }
    }

    fun updateResponseDetails(
        responseId: String,
        medicineName: String,
        medicinePrice: Double,
        quantityAvailable: Long,
        deliveryFee: Double,
        deliveryStatus: String
    ) {
        viewModelScope.launch {
            try {
                prescriptionRepo.updateResponseDetails(
                    responseId, medicineName, medicinePrice, quantityAvailable, deliveryFee, deliveryStatus
                )
                _actionState.value = ActionUiState.Success("Response updated.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = ActionUiState.Error(
                    unwrapFirestoreMessage(e, "Failed to update response.")
                )
            }
        }
    }

    fun saveMedicineInventory(medicine: MedicineItem) {
        viewModelScope.launch {
            _actionState.value = ActionUiState.Loading("Saving medicine stock...")
            try {
                pharmacyRepo.upsertMedicine(medicine)
                _actionState.value = ActionUiState.Success("Medicine inventory saved.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = ActionUiState.Error(
                    unwrapFirestoreMessage(e, "Failed to save medicine.")
                )
            }
        }
    }

    fun verifyAdminAccess() {
        viewModelScope.launch {
            _actionState.value = ActionUiState.Loading("Verifying administrator authorization...")
            val authorized = userRepo.checkIsAdmin()
            _isAdmin.value = authorized
            if (authorized) {
                _adminSessionVerified.value = true
                _actionState.value = ActionUiState.Success("Administrator session verified.")
            } else {
                _adminSessionVerified.value = false
                _actionState.value = ActionUiState.Error(
                    "Access denied: your Google account is not authorized as an OS Medicine administrator."
                )
            }
        }
    }

    fun lockAdminSession() {
        _adminSessionVerified.value = false
    }

    fun adminUpdatePharmacy(pharmacy: Pharmacy, approvalStatus: String, licenseStatus: String) {
        viewModelScope.launch {
            try {
                pharmacyRepo.adminUpdatePharmacyStatus(pharmacy, approvalStatus, licenseStatus)
                _actionState.value = ActionUiState.Success("${pharmacy.pharmacyName} updated to $approvalStatus.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = ActionUiState.Error(
                    unwrapFirestoreMessage(e, "Admin pharmacy update failed.")
                )
            }
        }
    }

    fun adminVerifyPayment(payment: PaymentSubmission, approve: Boolean, reason: String = "") {
        viewModelScope.launch {
            try {
                paymentRepo.adminVerifyPayment(payment, approve, reason)
                _actionState.value = ActionUiState.Success(
                    if (approve) "Payment approved & pharmacy location unlocked!" else "Payment rejected."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.value = ActionUiState.Error(
                    unwrapFirestoreMessage(e, "Payment verification failed.")
                )
            }
        }
    }

    fun markNotificationRead(notification: AppNotification) {
        viewModelScope.launch {
            if (!notification.isRead) {
                notificationRepo.markAsRead(notification.id)
            }
            when {
                notification.actionType.startsWith("OPEN_ADMIN") && _isAdmin.value -> {
                    _adminSessionVerified.value = true
                    _activeTab.value = MainNavTab.ADMIN
                }
                notification.actionType.startsWith("OPEN_PHARMACY") -> {
                    _activeTab.value = MainNavTab.PHARMACY
                }
                notification.actionType.startsWith("OPEN_PATIENT") -> {
                    _activeTab.value = MainNavTab.PATIENT
                }
            }
        }
    }

    fun clearReadNotifications() {
        viewModelScope.launch {
            notificationRepo.clearReadNotifications(userNotifications.value)
        }
    }

    // AI Assistant features (Firebase AI Logic)
    fun analyzePrescriptionWithAi(uri: Uri) {
        viewModelScope.launch {
            val bitmap: Bitmap? = storageRepo.decodeBitmapForAi(uri)
            if (bitmap == null) {
                _aiError.value = "Could not decode prescription image for AI analysis."
                return@launch
            }
            _aiLoading.value = true
            _aiError.value = null
            _aiMessages.value = _aiMessages.value + AiChatItem(
                isUser = true,
                text = if (_language.value == "am") "የማዘዣ ፎቶዬን አንብብልኝ" else "Please read my uploaded prescription photo."
            )
            val result = aiRepo.analyzePrescription(bitmap, _language.value)
            _aiLoading.value = false
            result.onSuccess { reply ->
                _aiMessages.value = _aiMessages.value + AiChatItem(isUser = false, text = reply)
            }.onFailure { err ->
                _aiError.value = err.message ?: "AI analysis failed."
            }
        }
    }

    fun sendAiMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _aiLoading.value = true
            _aiError.value = null
            val currentList = _aiMessages.value + AiChatItem(isUser = true, text = trimmed)
            _aiMessages.value = currentList + AiChatItem(isUser = false, text = "")
            val streamingIdx = _aiMessages.value.lastIndex
            try {
                aiRepo.sendChatMessageStream(trimmed).collect { accumulated ->
                    val updated = _aiMessages.value.toMutableList()
                    if (streamingIdx in updated.indices) {
                        updated[streamingIdx] = AiChatItem(isUser = false, text = accumulated)
                        _aiMessages.value = updated
                    }
                }
                _aiLoading.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiLoading.value = false
                _aiError.value = e.localizedMessage ?: "AI assistant failed to respond."
                val updated = _aiMessages.value.toMutableList()
                if (streamingIdx in updated.indices && updated[streamingIdx].text.isBlank()) {
                    updated.removeAt(streamingIdx)
                    _aiMessages.value = updated
                }
            }
        }
    }

    companion object {
        private const val TAG = "OsMedicineVM"
    }
}
