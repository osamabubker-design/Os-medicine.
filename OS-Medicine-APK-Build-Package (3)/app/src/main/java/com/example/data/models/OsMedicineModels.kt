package com.example.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class AppUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val role: String = "patient",
    val address: String = "",
    val fcmToken: String? = null,
    val accountStatus: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("fullName", fullName)
        put("phoneNumber", phoneNumber)
        put("email", email)
        put("role", role)
        if (address.isNotBlank()) put("address", address)
        if (!fcmToken.isNullOrBlank()) put("fcmToken", fcmToken)
        put("accountStatus", accountStatus)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class Pharmacy(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val pharmacyName: String = "",
    val pharmacistName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val businessLicenseNumber: String = "",
    val licenseVerificationStatus: String = "PENDING",
    val approvalStatus: String = "PENDING",
    val availability: Boolean = true,
    val averageRating: Double = 0.0,
    val totalRatings: Long = 0L,
    val profileImageUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isApproved: Boolean get() = approvalStatus == "APPROVED"
    val isSuspended: Boolean get() = approvalStatus == "SUSPENDED"
    val isPending: Boolean get() = approvalStatus == "PENDING"

    fun toCreateMap(): Map<String, Any> = buildMap {
        put("ownerId", ownerId)
        put("pharmacyName", pharmacyName)
        put("pharmacistName", pharmacistName)
        put("phoneNumber", phoneNumber)
        put("email", email)
        put("address", address)
        put("latitude", latitude)
        put("longitude", longitude)
        put("businessLicenseNumber", businessLicenseNumber)
        put("licenseVerificationStatus", "PENDING")
        put("approvalStatus", "PENDING")
        put("availability", availability)
        put("averageRating", 0.0)
        put("totalRatings", 0L)
        if (!profileImageUrl.isNullOrBlank()) put("profileImageUrl", profileImageUrl)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class MedicineItem(
    @DocumentId val id: String = "",
    val medicineName: String = "",
    val brand: String = "",
    val category: String = "",
    val description: String = "",
    val dosage: String = "",
    val pharmacyId: String = "",
    val ownerId: String = "",
    val pharmacyName: String = "",
    val quantityAvailable: Long = 0L,
    val stockStatus: String = "IN_STOCK",
    val price: Double = 0.0,
    val imageUrl: String? = null,
    val pharmacyApproved: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("medicineName", medicineName)
        put("brand", brand)
        put("category", category)
        if (description.isNotBlank()) put("description", description)
        put("dosage", dosage)
        put("pharmacyId", pharmacyId)
        put("ownerId", ownerId)
        put("pharmacyName", pharmacyName)
        put("quantityAvailable", quantityAvailable)
        put("stockStatus", stockStatus)
        put("price", price)
        if (!imageUrl.isNullOrBlank()) put("imageUrl", imageUrl)
        put("pharmacyApproved", pharmacyApproved)
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class PrescriptionRequest(
    @DocumentId val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val patientPhone: String = "",
    val prescriptionImageUrl: String = "",
    val medicineName: String? = null,
    val deliveryMode: String = "pickup",
    val deliveryAddress: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val requestStatus: String = "PENDING",
    val paymentUnlocked: Boolean = false,
    val paymentId: String? = null,
    val eligibleOwnerIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("patientId", patientId)
        put("patientName", patientName)
        put("patientPhone", patientPhone)
        put("prescriptionImageUrl", prescriptionImageUrl)
        if (!medicineName.isNullOrBlank()) put("medicineName", medicineName)
        put("deliveryMode", deliveryMode)
        if (!deliveryAddress.isNullOrBlank()) put("deliveryAddress", deliveryAddress)
        put("latitude", latitude)
        put("longitude", longitude)
        put("requestStatus", "PENDING")
        put("paymentUnlocked", false)
        if (!paymentId.isNullOrBlank()) put("paymentId", paymentId)
        put("eligibleOwnerIds", eligibleOwnerIds.take(50))
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class PharmacyResponse(
    @DocumentId val id: String = "",
    val requestId: String = "",
    val patientId: String = "",
    val pharmacyId: String = "",
    val pharmacyOwnerId: String = "",
    val pharmacyName: String = "",
    val pharmacyAddress: String = "",
    val pharmacyPhone: String = "",
    val pharmacyLat: Double = 0.0,
    val pharmacyLng: Double = 0.0,
    val medicineAvailability: Boolean = false,
    val medicineName: String = "",
    val medicinePrice: Double = 0.0,
    val quantityAvailable: Long = 0L,
    val responseMessage: String = "",
    val estimatedDeliveryTime: String = "",
    val deliveryFee: Double = 0.0,
    val deliveryStatus: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("requestId", requestId)
        put("patientId", patientId)
        put("pharmacyId", pharmacyId)
        put("pharmacyOwnerId", pharmacyOwnerId)
        put("pharmacyName", pharmacyName)
        put("pharmacyAddress", pharmacyAddress)
        put("pharmacyPhone", pharmacyPhone)
        put("pharmacyLat", pharmacyLat)
        put("pharmacyLng", pharmacyLng)
        put("medicineAvailability", medicineAvailability)
        put("medicineName", medicineName)
        put("medicinePrice", medicinePrice)
        put("quantityAvailable", quantityAvailable)
        put("responseMessage", responseMessage)
        put("estimatedDeliveryTime", estimatedDeliveryTime)
        put("deliveryFee", deliveryFee)
        put("deliveryStatus", deliveryStatus)
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class MedicineOrder(
    @DocumentId val id: String = "",
    val patientId: String = "",
    val pharmacyId: String = "",
    val pharmacyOwnerId: String = "",
    val requestId: String = "",
    val medicineItems: List<String> = emptyList(),
    val totalAmount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val paymentStatus: String = "PENDING",
    val deliveryMode: String = "pickup",
    val deliveryAddress: String = "",
    val deliveryStatus: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("patientId", patientId)
        put("pharmacyId", pharmacyId)
        put("pharmacyOwnerId", pharmacyOwnerId)
        put("requestId", requestId)
        put("medicineItems", medicineItems)
        put("totalAmount", totalAmount)
        put("deliveryFee", deliveryFee)
        put("paymentStatus", "PENDING")
        put("deliveryMode", deliveryMode)
        put("deliveryAddress", deliveryAddress)
        put("deliveryStatus", "PENDING")
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class PaymentSubmission(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String? = null,
    val requestId: String = "",
    val paymentProvider: String = "telebirr",
    val paymentAmount: Double = 50.0,
    val recipientNumber: String = "+251925694548",
    val paymentPhone: String = "",
    val paymentReference: String = "",
    val paymentScreenshotUrl: String = "",
    val verificationStatus: String = "PENDING",
    val adminId: String? = null,
    val rejectionReason: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val verifiedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        if (!orderId.isNullOrBlank()) put("orderId", orderId)
        put("requestId", requestId)
        put("paymentProvider", paymentProvider)
        put("paymentAmount", paymentAmount)
        put("recipientNumber", recipientNumber)
        put("paymentPhone", paymentPhone)
        put("paymentReference", paymentReference)
        put("paymentScreenshotUrl", paymentScreenshotUrl)
        put("verificationStatus", "PENDING")
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val patientId: String = "",
    val pharmacyId: String = "",
    val pharmacyOwnerId: String = "",
    val requestId: String = "",
    val pharmacyName: String = "",
    val patientName: String = "",
    val lastMessage: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderRole: String = "patient",
    val patientId: String = "",
    val pharmacyOwnerId: String = "",
    val message: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null,
    val readStatus: Boolean = false
)

@IgnoreExtraProperties
data class PharmacyRating(
    @DocumentId val id: String = "",
    val patientId: String = "",
    val pharmacyId: String = "",
    val pharmacyOwnerId: String = "",
    val requestId: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val isPublic: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val recipientRole: String = "patient",
    val senderId: String = "",
    val title: String = "",
    val message: String = "",
    val notificationType: String = "",
    val relatedOrderId: String? = null,
    val relatedRequestId: String? = null,
    val relatedPaymentId: String? = null,
    val isRead: Boolean = false,
    val actionType: String = "OPEN_HOME",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminLog(
    @DocumentId val id: String = "",
    val adminId: String = "",
    val action: String = "",
    val targetId: String = "",
    val description: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)
