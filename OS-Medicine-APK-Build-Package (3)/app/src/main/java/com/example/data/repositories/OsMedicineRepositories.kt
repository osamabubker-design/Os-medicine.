package com.example.data.repositories

import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.models.AdminLog
import com.example.data.models.AppNotification
import com.example.data.models.AppUser
import com.example.data.models.ChatMessage
import com.example.data.models.ChatThread
import com.example.data.models.MedicineItem
import com.example.data.models.MedicineOrder
import com.example.data.models.PaymentSubmission
import com.example.data.models.Pharmacy
import com.example.data.models.PharmacyRating
import com.example.data.models.PharmacyResponse
import com.example.data.models.PrescriptionRequest
import com.example.utils.GeoUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private fun Throwable.unwrapAndLog(op: OperationType, path: String) {
    val fsErr = generateSequence(this) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (fsErr == null && this is CancellationException) throw this
    handleFirestoreError(fsErr ?: this, op, path)
}

class UserRepository(private val db: FirebaseFirestore) {
    private val auth get() = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Please sign in with Google first.")

    suspend fun ensureUserProfile(
        fullName: String,
        email: String,
        phoneNumber: String = ""
    ) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid)
        try {
            val snap = ref.get().await()
            if (!snap.exists()) {
                val user = AppUser(
                    userId = uid,
                    fullName = fullName.ifBlank { "OS Medicine User" },
                    phoneNumber = phoneNumber,
                    email = email,
                    role = "patient",
                    accountStatus = "active"
                )
                ref.set(user.toCreateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
        }
    }

    fun observeCurrentUser(): Flow<AppUser?> = flow {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid)
        emitAll(
            ref.snapshots()
                .map { snap ->
                    snap.toObject(AppUser::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.GET, ref.path)
                    throw e
                }
        )
    }

    suspend fun updateUserProfile(fullName: String, phoneNumber: String, address: String) {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid)
        try {
            ref.update(
                mapOf(
                    "fullName" to fullName.trim(),
                    "phoneNumber" to phoneNumber.trim(),
                    "address" to address.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun updateFcmToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid)
        try {
            ref.update(
                mapOf(
                    "fcmToken" to token,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
        }
    }

    suspend fun checkIsAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.isEmailVerified && user.email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)) return true
        val ref = db.collection("admins").document(user.uid)
        return try {
            ref.get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, ref.path)
            false
        }
    }

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "osamsuse21@gmail.com"
    }
}

class PharmacyRepository(private val db: FirebaseFirestore) {
    private val auth get() = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Please sign in with Google first.")

    fun observeApprovedPharmacies(): Flow<List<Pharmacy>> = flow {
        val uid = requireUserId()
        val query = db.collection("pharmacies").whereEqualTo("approvalStatus", "APPROVED")
        emitAll(
            query.snapshots()
                .map { snap -> snap.toObjects(Pharmacy::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "pharmacies")
                    throw e
                }
        )
    }

    fun observeMyPharmacy(): Flow<Pharmacy?> = flow {
        val uid = requireUserId()
        val query = db.collection("pharmacies").whereEqualTo("ownerId", uid)
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Pharmacy::class.java, ServerTimestampBehavior.ESTIMATE).firstOrNull()
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "pharmacies")
                    throw e
                }
        )
    }

    fun observeAllPharmaciesAdmin(): Flow<List<Pharmacy>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("pharmacies")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Pharmacy::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "pharmacies")
                    throw e
                }
        )
    }

    suspend fun registerPharmacy(
        pharmacyName: String,
        pharmacistName: String,
        phoneNumber: String,
        email: String,
        address: String,
        latitude: Double,
        longitude: Double,
        businessLicenseNumber: String,
        profileImageUrl: String? = null
    ): String {
        val uid = requireUserId()
        // Validate all fields strictly
        require(pharmacyName.trim().length >= 2) { "Pharmacy name must be at least 2 characters." }
        require(pharmacistName.trim().length >= 2) { "Pharmacist name must be at least 2 characters." }
        require(GeoUtils.isValidEthiopianPhone(phoneNumber)) {
            "Enter a valid Ethiopian phone number (e.g. 0911223344 or +251911223344)."
        }
        require(email.isBlank() || email.contains("@")) { "Please enter a valid email address." }
        require(address.trim().length >= 2) { "Please enter the pharmacy street address." }
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0 && (latitude != 0.0 || longitude != 0.0)) {
            "Valid GPS coordinates are required. Tap 'Use my current location' or enter coordinates."
        }
        require(businessLicenseNumber.trim().length >= 2) { "Business license number is required for verification." }

        val existing = db.collection("pharmacies").whereEqualTo("ownerId", uid).get().await()
        if (!existing.isEmpty) {
            throw IllegalStateException("A pharmacy is already registered to your account (${existing.documents.first().getString("pharmacyName")}).")
        }

        val docRef = db.collection("pharmacies").document()
        val pharmacy = Pharmacy(
            ownerId = uid,
            pharmacyName = pharmacyName.trim(),
            pharmacistName = pharmacistName.trim(),
            phoneNumber = phoneNumber.trim(),
            email = email.trim().ifBlank { auth.currentUser?.email ?: "" },
            address = address.trim(),
            latitude = latitude,
            longitude = longitude,
            businessLicenseNumber = businessLicenseNumber.trim(),
            licenseVerificationStatus = "PENDING",
            approvalStatus = "PENDING",
            availability = true,
            averageRating = 0.0,
            totalRatings = 0L,
            profileImageUrl = profileImageUrl
        )
        try {
            docRef.set(pharmacy.toCreateMap()).await()
            // Notify admins of new pharmacy registration
            val notifRef = db.collection("notifications").document()
            notifRef.set(
                mapOf(
                    "recipientId" to "admin_broadcast",
                    "recipientRole" to "admin",
                    "senderId" to uid,
                    "title" to "New pharmacy registration",
                    "message" to "${pharmacy.pharmacyName} submitted license ${pharmacy.businessLicenseNumber} for approval.",
                    "notificationType" to "NEW_PHARMACY_REGISTRATION",
                    "relatedRequestId" to docRef.id,
                    "isRead" to false,
                    "actionType" to "OPEN_ADMIN_PHARMACIES",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updatePharmacyProfile(
        pharmacyId: String,
        pharmacyName: String,
        pharmacistName: String,
        phoneNumber: String,
        address: String,
        availability: Boolean,
        latitude: Double,
        longitude: Double
    ) {
        requireUserId()
        require(pharmacyId.isNotBlank())
        val ref = db.collection("pharmacies").document(pharmacyId)
        try {
            ref.update(
                mapOf(
                    "pharmacyName" to pharmacyName.trim(),
                    "pharmacistName" to pharmacistName.trim(),
                    "phoneNumber" to phoneNumber.trim(),
                    "address" to address.trim(),
                    "availability" to availability,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun adminUpdatePharmacyStatus(
        pharmacy: Pharmacy,
        newApprovalStatus: String,
        newLicenseStatus: String
    ) {
        val adminUid = requireUserId()
        val ref = db.collection("pharmacies").document(pharmacy.id)
        try {
            ref.update(
                mapOf(
                    "approvalStatus" to newApprovalStatus,
                    "licenseVerificationStatus" to newLicenseStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("adminLogs").document().set(
                mapOf(
                    "adminId" to adminUid,
                    "action" to "PHARMACY_$newApprovalStatus",
                    "targetId" to pharmacy.id,
                    "description" to "Set ${pharmacy.pharmacyName} approval=$newApprovalStatus, license=$newLicenseStatus",
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()

            val title = when (newApprovalStatus) {
                "APPROVED" -> "Pharmacy Account Approved"
                "REJECTED" -> "Pharmacy Registration Rejected"
                "SUSPENDED" -> "Pharmacy Account Suspended"
                else -> "Pharmacy Status Updated"
            }
            db.collection("notifications").document().set(
                mapOf(
                    "recipientId" to pharmacy.ownerId,
                    "recipientRole" to "pharmacy",
                    "senderId" to adminUid,
                    "title" to title,
                    "message" to "Your pharmacy ${pharmacy.pharmacyName} status is now $newApprovalStatus.",
                    "notificationType" to "PHARMACY_STATUS_$newApprovalStatus",
                    "isRead" to false,
                    "actionType" to "OPEN_PHARMACY",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    fun observeApprovedMedicines(): Flow<List<MedicineItem>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("medicines")
                .whereEqualTo("pharmacyApproved", true)
                .snapshots()
                .map { snap -> snap.toObjects(MedicineItem::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "medicines")
                    throw e
                }
        )
    }

    fun observePharmacyMedicines(pharmacyId: String): Flow<List<MedicineItem>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("medicines")
                .whereEqualTo("ownerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MedicineItem::class.java, ServerTimestampBehavior.ESTIMATE)
                        .filter { it.pharmacyId == pharmacyId }
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "medicines")
                    throw e
                }
        )
    }

    suspend fun upsertMedicine(medicine: MedicineItem) {
        val uid = requireUserId()
        val ref = if (medicine.id.isBlank()) {
            db.collection("medicines").document()
        } else {
            db.collection("medicines").document(medicine.id)
        }
        val stockStatus = when {
            medicine.quantityAvailable <= 0L -> "OUT_OF_STOCK"
            medicine.quantityAvailable <= 5L -> "LOW_STOCK"
            else -> "IN_STOCK"
        }
        try {
            ref.set(
                medicine.copy(ownerId = uid, stockStatus = stockStatus).toCreateMap()
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            throw e
        }
    }
}

class PrescriptionRepository(private val db: FirebaseFirestore) {
    private val auth get() = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Please sign in with Google first.")

    fun observeMyPrescriptionRequests(): Flow<List<PrescriptionRequest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("prescriptionRequests")
                .whereEqualTo("patientId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PrescriptionRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "prescriptionRequests")
                    throw e
                }
        )
    }

    fun observeIncomingPharmacyRequests(): Flow<List<PrescriptionRequest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("prescriptionRequests")
                .whereArrayContains("eligibleOwnerIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PrescriptionRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "prescriptionRequests")
                    throw e
                }
        )
    }

    fun observeAllRequestsAdmin(): Flow<List<PrescriptionRequest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("prescriptionRequests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PrescriptionRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "prescriptionRequests")
                    throw e
                }
        )
    }

    fun observeResponsesForPatient(): Flow<List<PharmacyResponse>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("pharmacyResponses")
                .whereEqualTo("patientId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PharmacyResponse::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "pharmacyResponses")
                    throw e
                }
        )
    }

    fun observeResponsesByPharmacy(): Flow<List<PharmacyResponse>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("pharmacyResponses")
                .whereEqualTo("pharmacyOwnerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PharmacyResponse::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "pharmacyResponses")
                    throw e
                }
        )
    }

    suspend fun createPrescriptionRequest(
        patientName: String,
        patientPhone: String,
        prescriptionImageUrl: String,
        medicineName: String?,
        deliveryMode: String,
        deliveryAddress: String?,
        latitude: Double,
        longitude: Double,
        nearbyApprovedPharmacies: List<Pharmacy>
    ): String {
        val uid = requireUserId()
        val eligibleOwners = nearbyApprovedPharmacies
            .filter {
                GeoUtils.haversineKm(latitude, longitude, it.latitude, it.longitude) <= GeoUtils.PHARMACY_SEARCH_RADIUS_KM
            }
            .map { it.ownerId }
            .distinct()
            .take(50)

        val docRef = db.collection("prescriptionRequests").document()
        val req = PrescriptionRequest(
            patientId = uid,
            patientName = patientName.trim(),
            patientPhone = patientPhone.trim(),
            prescriptionImageUrl = prescriptionImageUrl,
            medicineName = medicineName?.trim(),
            deliveryMode = deliveryMode,
            deliveryAddress = deliveryAddress?.trim(),
            latitude = latitude,
            longitude = longitude,
            requestStatus = "PENDING",
            paymentUnlocked = false,
            eligibleOwnerIds = eligibleOwners
        )
        try {
            docRef.set(req.toCreateMap()).await()
            // Notify patient confirmation
            db.collection("notifications").document().set(
                mapOf(
                    "recipientId" to uid,
                    "recipientRole" to "patient",
                    "senderId" to uid,
                    "title" to "Prescription request submitted",
                    "message" to "Sent to ${eligibleOwners.size} nearby pharmacies within 5 km.",
                    "notificationType" to "NEW_PRESCRIPTION_REQUEST",
                    "relatedRequestId" to docRef.id,
                    "isRead" to false,
                    "actionType" to "OPEN_PATIENT_REQUEST",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            // Notify each eligible pharmacy
            eligibleOwners.forEach { ownerUid ->
                db.collection("notifications").document().set(
                    mapOf(
                        "recipientId" to ownerUid,
                        "recipientRole" to "pharmacy",
                        "senderId" to uid,
                        "title" to "New patient prescription request",
                        "message" to "A nearby patient is looking for prescribed medicine (${deliveryMode}).",
                        "notificationType" to "NEW_PRESCRIPTION_REQUEST",
                        "relatedRequestId" to docRef.id,
                        "isRead" to false,
                        "actionType" to "OPEN_PHARMACY_INCOMING",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun respondToRequest(
        request: PrescriptionRequest,
        pharmacy: Pharmacy,
        available: Boolean,
        medicineName: String,
        medicinePrice: Double,
        quantityAvailable: Long,
        responseMessage: String,
        deliveryFee: Double,
        estimatedDeliveryTime: String
    ) {
        val uid = requireUserId()
        val respRef = db.collection("pharmacyResponses").document()
        val resp = PharmacyResponse(
            requestId = request.id,
            patientId = request.patientId,
            pharmacyId = pharmacy.id,
            pharmacyOwnerId = uid,
            pharmacyName = pharmacy.pharmacyName,
            pharmacyAddress = pharmacy.address,
            pharmacyPhone = pharmacy.phoneNumber,
            pharmacyLat = pharmacy.latitude,
            pharmacyLng = pharmacy.longitude,
            medicineAvailability = available,
            medicineName = medicineName.ifBlank { "Prescription Stock Confirmed" },
            medicinePrice = medicinePrice,
            quantityAvailable = quantityAvailable,
            responseMessage = responseMessage,
            estimatedDeliveryTime = estimatedDeliveryTime,
            deliveryFee = deliveryFee,
            deliveryStatus = "PENDING"
        )
        try {
            respRef.set(resp.toCreateMap()).await()
            if (request.requestStatus == "PENDING") {
                db.collection("prescriptionRequests").document(request.id)
                    .update("requestStatus", "RESPONDED")
                    .await()
            }
            db.collection("notifications").document().set(
                mapOf(
                    "recipientId" to request.patientId,
                    "recipientRole" to "patient",
                    "senderId" to uid,
                    "title" to if (available) "${pharmacy.pharmacyName} has your medicine!" else "${pharmacy.pharmacyName} replied",
                    "message" to if (available) {
                        "Medicine stock confirmed. Unlock exact location for 50 ETB."
                    } else {
                        "This pharmacy marked the requested medicine out of stock."
                    },
                    "notificationType" to "PHARMACY_RESPONSE",
                    "relatedRequestId" to request.id,
                    "isRead" to false,
                    "actionType" to "OPEN_PATIENT_REQUEST",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, respRef.path)
            throw e
        }
    }

    suspend fun updateResponseDetails(
        responseId: String,
        medicineName: String,
        medicinePrice: Double,
        quantityAvailable: Long,
        deliveryFee: Double,
        deliveryStatus: String
    ) {
        requireUserId()
        val ref = db.collection("pharmacyResponses").document(responseId)
        try {
            ref.update(
                mapOf(
                    "medicineName" to medicineName.trim(),
                    "medicinePrice" to medicinePrice,
                    "quantityAvailable" to quantityAvailable,
                    "deliveryFee" to deliveryFee,
                    "deliveryStatus" to deliveryStatus
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            throw e
        }
    }

    suspend fun createOrder(order: MedicineOrder): String {
        val uid = requireUserId()
        val ref = db.collection("orders").document()
        try {
            ref.set(order.copy(patientId = uid).toCreateMap()).await()
            return ref.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            throw e
        }
    }

    fun observePatientOrders(): Flow<List<MedicineOrder>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("orders")
                .whereEqualTo("patientId", uid)
                .snapshots()
                .map { it.toObjects(MedicineOrder::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "orders")
                    throw e
                }
        )
    }

    suspend fun submitRating(
        pharmacy: Pharmacy,
        requestId: String,
        score: Long,
        comment: String
    ) {
        val uid = requireUserId()
        val ratingRef = db.collection("ratings").document()
        try {
            ratingRef.set(
                mapOf(
                    "patientId" to uid,
                    "pharmacyId" to pharmacy.id,
                    "pharmacyOwnerId" to pharmacy.ownerId,
                    "requestId" to requestId,
                    "rating" to score.coerceIn(1L, 5L),
                    "comment" to comment.trim(),
                    "isPublic" to true,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ratingRef.path)
            throw e
        }
    }

    fun observeAllRatings(): Flow<List<PharmacyRating>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("ratings")
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { it.toObjects(PharmacyRating::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "ratings")
                    throw e
                }
        )
    }
}

class PaymentRepository(private val db: FirebaseFirestore) {
    private val auth get() = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Please sign in with Google first.")

    fun observeMyPayments(): Flow<List<PaymentSubmission>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("payments")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentSubmission::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "payments")
                    throw e
                }
        )
    }

    fun observeAllPaymentsAdmin(): Flow<List<PaymentSubmission>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("payments")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PaymentSubmission::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "payments")
                    throw e
                }
        )
    }

    suspend fun submitPaymentProof(
        requestId: String,
        provider: String,
        paymentPhone: String,
        paymentReference: String,
        screenshotUrl: String
    ): String {
        val uid = requireUserId()
        require(GeoUtils.isValidEthiopianPhone(paymentPhone)) {
            "Enter a valid Ethiopian phone number."
        }
        require(paymentReference.trim().length >= 3) {
            "Please enter your transaction reference (e.g. TXN-TB-12345)."
        }

        // Prevent duplicate transaction reference by the same user
        val existing = db.collection("payments")
            .whereEqualTo("userId", uid)
            .get()
            .await()
            .toObjects(PaymentSubmission::class.java)
        if (existing.any { it.paymentReference.equals(paymentReference.trim(), ignoreCase = true) }) {
            throw IllegalStateException("This transaction reference has already been submitted.")
        }

        val payRef = db.collection("payments").document()
        val payment = PaymentSubmission(
            userId = uid,
            requestId = requestId,
            paymentProvider = provider,
            paymentAmount = 50.0,
            recipientNumber = OFFICIAL_TELEBIRR_NUMBER,
            paymentPhone = paymentPhone.trim(),
            paymentReference = paymentReference.trim(),
            paymentScreenshotUrl = screenshotUrl,
            verificationStatus = "PENDING"
        )
        try {
            payRef.set(payment.toCreateMap()).await()
            db.collection("prescriptionRequests").document(requestId)
                .update("paymentId", payRef.id)
                .await()

            // Patient submitted notification
            db.collection("notifications").document().set(
                mapOf(
                    "recipientId" to uid,
                    "recipientRole" to "patient",
                    "senderId" to uid,
                    "title" to "Payment submitted",
                    "message" to "Your 50 ETB $provider payment (${paymentReference.trim()}) is awaiting admin verification.",
                    "notificationType" to "PAYMENT_SUBMITTED",
                    "relatedRequestId" to requestId,
                    "relatedPaymentId" to payRef.id,
                    "isRead" to false,
                    "actionType" to "OPEN_PATIENT_REQUEST",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Admin notification
            db.collection("notifications").document().set(
                mapOf(
                    "recipientId" to "admin_broadcast",
                    "recipientRole" to "admin",
                    "senderId" to uid,
                    "title" to "New Telebirr payment awaiting verification",
                    "message" to "50 ETB payment (${paymentReference.trim()}) to $OFFICIAL_TELEBIRR_NUMBER pending review.",
                    "notificationType" to "NEW_PAYMENT_VERIFICATION",
                    "relatedRequestId" to requestId,
                    "relatedPaymentId" to payRef.id,
                    "isRead" to false,
                    "actionType" to "OPEN_ADMIN_PAYMENTS",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            return payRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, payRef.path)
            throw e
        }
    }

    suspend fun adminVerifyPayment(
        payment: PaymentSubmission,
        approve: Boolean,
        rejectionReason: String = ""
    ) {
        val adminUid = requireUserId()
        val payRef = db.collection("payments").document(payment.id)
        val status = if (approve) "APPROVED" else "REJECTED"
        try {
            payRef.update(
                buildMap {
                    put("verificationStatus", status)
                    put("adminId", adminUid)
                    put("verifiedAt", FieldValue.serverTimestamp())
                    if (!approve && rejectionReason.isNotBlank()) {
                        put("rejectionReason", rejectionReason.trim())
                    }
                }
            ).await()

            if (approve && payment.requestId.isNotBlank()) {
                db.collection("prescriptionRequests").document(payment.requestId)
                    .update(
                        mapOf(
                            "paymentUnlocked" to true,
                            "requestStatus" to "UNLOCKED",
                            "paymentId" to payment.id
                        )
                    ).await()
            }

            db.collection("adminLogs").document().set(
                mapOf(
                    "adminId" to adminUid,
                    "action" to "PAYMENT_$status",
                    "targetId" to payment.id,
                    "description" to "$status 50 ETB ${payment.paymentProvider} ref=${payment.paymentReference}",
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("notifications").document().set(
                mapOf(
                    "recipientId" to payment.userId,
                    "recipientRole" to "patient",
                    "senderId" to adminUid,
                    "title" to if (approve) "Your Telebirr payment has been approved" else "Your Telebirr payment was rejected",
                    "message" to if (approve) {
                        "Location & Google Maps directions are now unlocked."
                    } else {
                        rejectionReason.ifBlank { "Payment proof could not be verified. Please contact support or resubmit." }
                    },
                    "notificationType" to "PAYMENT_$status",
                    "relatedRequestId" to payment.requestId,
                    "relatedPaymentId" to payment.id,
                    "isRead" to false,
                    "actionType" to "OPEN_PATIENT_REQUEST",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, payRef.path)
            throw e
        }
    }

    companion object {
        const val OFFICIAL_TELEBIRR_NUMBER = "+251925694548"
        const val OFFICIAL_TELEBIRR_LOCAL = "0925694548"
        const val BROADCAST_FEE_ETB = 50.0
    }
}

class ChatRepository(private val db: FirebaseFirestore) {
    private val auth get() = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Please sign in with Google first.")

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("messages")
                .whereEqualTo("chatId", chatId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.timestamp?.seconds ?: Long.MAX_VALUE }
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "messages")
                    throw e
                }
        )
    }

    fun observePatientChats(): Flow<List<ChatThread>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("chats")
                .whereEqualTo("patientId", uid)
                .snapshots()
                .map { it.toObjects(ChatThread::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "chats")
                    throw e
                }
        )
    }

    fun observePharmacyChats(): Flow<List<ChatThread>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("chats")
                .whereEqualTo("pharmacyOwnerId", uid)
                .snapshots()
                .map { it.toObjects(ChatThread::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "chats")
                    throw e
                }
        )
    }

    suspend fun sendMessage(
        requestId: String,
        patientId: String,
        patientName: String,
        pharmacyId: String,
        pharmacyOwnerId: String,
        pharmacyName: String,
        senderRole: String,
        text: String
    ) {
        val uid = requireUserId()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val chatId = "${requestId}_${pharmacyId}"
        val chatRef = db.collection("chats").document(chatId)
        try {
            val snap = chatRef.get().await()
            if (!snap.exists()) {
                chatRef.set(
                    mapOf(
                        "patientId" to patientId,
                        "pharmacyId" to pharmacyId,
                        "pharmacyOwnerId" to pharmacyOwnerId,
                        "requestId" to requestId,
                        "pharmacyName" to pharmacyName,
                        "patientName" to patientName,
                        "lastMessage" to trimmed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                chatRef.update(
                    mapOf(
                        "lastMessage" to trimmed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            db.collection("messages").document().set(
                mapOf(
                    "chatId" to chatId,
                    "senderId" to uid,
                    "senderRole" to senderRole,
                    "patientId" to patientId,
                    "pharmacyOwnerId" to pharmacyOwnerId,
                    "message" to trimmed,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "readStatus" to false
                )
            ).await()

            val recipientId = if (senderRole == "patient") pharmacyOwnerId else patientId
            val recipientRole = if (senderRole == "patient") "pharmacy" else "patient"
            db.collection("notifications").document().set(
                mapOf(
                    "recipientId" to recipientId,
                    "recipientRole" to recipientRole,
                    "senderId" to uid,
                    "title" to if (senderRole == "patient") "New message from patient" else "New message from $pharmacyName",
                    "message" to trimmed.take(120),
                    "notificationType" to "NEW_CHAT_MESSAGE",
                    "relatedRequestId" to requestId,
                    "isRead" to false,
                    "actionType" to if (recipientRole == "patient") "OPEN_PATIENT_REQUEST" else "OPEN_PHARMACY_HISTORY",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, chatRef.path)
            throw e
        }
    }
}

class NotificationRepository(private val db: FirebaseFirestore) {
    private val auth get() = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Please sign in with Google first.")

    fun observeUserNotifications(): Flow<List<AppNotification>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "notifications")
                    throw e
                }
        )
    }

    fun observeAdminNotifications(): Flow<List<AppNotification>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("notifications")
                .whereEqualTo("recipientRole", "admin")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "notifications")
                    throw e
                }
        )
    }

    suspend fun markAsRead(notificationId: String) {
        requireUserId()
        val ref = db.collection("notifications").document(notificationId)
        try {
            ref.update("isRead", true).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
        }
    }

    suspend fun clearReadNotifications(notifications: List<AppNotification>) {
        requireUserId()
        notifications.filter { it.isRead }.forEach { notif ->
            try {
                db.collection("notifications").document(notif.id).delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, "notifications/${notif.id}")
            }
        }
    }

    fun observeAdminLogs(): Flow<List<AdminLog>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("adminLogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { it.toObjects(AdminLog::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    if (auth.currentUser?.uid == uid) e.unwrapAndLog(OperationType.LIST, "adminLogs")
                    throw e
                }
        )
    }
}
