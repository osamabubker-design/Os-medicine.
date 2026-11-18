package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.repositories.PaymentRepository
import com.example.data.repositories.PharmacyRepository
import com.example.data.repositories.PrescriptionRepository
import com.example.data.repositories.UserRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OsMedicineRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_authenticatedPatient_createsAndObservesUser() = runBlocking {
        val email = "patient_${UUID.randomUUID().toString().take(8)}@test.com"
        val uid = signInTestUser(email)
        val userRepo = UserRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            userRepo.ensureUserProfile(fullName = "Abebe Bikila", email = email, phoneNumber = "0911223344")
        }

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            userRepo.observeCurrentUser().first { it != null }
        }
        assertNotNull(observed)
        assertEquals(uid, observed?.userId)
        assertEquals("patient", observed?.role)
    }

    @Test
    fun registerPharmacy_validPayload_createsPendingPharmacyAndPreventsDuplicate() = runBlocking {
        val email = "pharmacy_${UUID.randomUUID().toString().take(8)}@test.com"
        signInTestUser(email)
        val pharmacyRepo = PharmacyRepository(firestore)

        val pharmacyId = withTimeout(DEFAULT_TIMEOUT_MS) {
            pharmacyRepo.registerPharmacy(
                pharmacyName = "Addis Care Pharmacy",
                pharmacistName = "Dr. Hana Tesfaye",
                phoneNumber = "0911556677",
                email = email,
                address = "Bole Medhanialem, Addis Ababa",
                latitude = 9.005401,
                longitude = 38.763611,
                businessLicenseNumber = "EFDA-AA-9021"
            )
        }
        assertTrue(pharmacyId.isNotBlank())

        val myPharmacy = withTimeout(FLOW_TIMEOUT_MS) {
            pharmacyRepo.observeMyPharmacy().first { it != null }
        }
        assertEquals("PENDING", myPharmacy?.approvalStatus)
        assertEquals("PENDING", myPharmacy?.licenseVerificationStatus)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                pharmacyRepo.registerPharmacy(
                    pharmacyName = "Duplicate Pharmacy",
                    pharmacistName = "Dr. Hana",
                    phoneNumber = "0911556677",
                    email = email,
                    address = "Bole",
                    latitude = 9.005401,
                    longitude = 38.763611,
                    businessLicenseNumber = "EFDA-AA-9999"
                )
            }
            fail("Expected duplicate pharmacy registration to throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("already registered", ignoreCase = true) == true)
        }
    }

    @Test
    fun createPrescriptionRequest_andCrossUserIsolation_enforced() = runBlocking {
        val aliceEmail = "alice_${UUID.randomUUID().toString().take(8)}@test.com"
        val bobEmail = "bob_${UUID.randomUUID().toString().take(8)}@test.com"

        signInTestUser(aliceEmail)
        val rxRepo = PrescriptionRepository(firestore)
        val reqId = withTimeout(DEFAULT_TIMEOUT_MS) {
            rxRepo.createPrescriptionRequest(
                patientName = "Alice",
                patientPhone = "0912345678",
                prescriptionImageUrl = "https://firebasestorage.googleapis.com/v0/b/demo/o/rx.jpg",
                medicineName = "Amoxicillin 500mg",
                deliveryMode = "pickup",
                deliveryAddress = "Bole, Addis Ababa",
                latitude = 9.005401,
                longitude = 38.763611,
                nearbyApprovedPharmacies = emptyList()
            )
        }
        assertTrue(reqId.isNotBlank())

        val ownRequests = withTimeout(FLOW_TIMEOUT_MS) {
            rxRepo.observeMyPrescriptionRequests().first { list -> list.any { it.id == reqId } }
        }
        assertTrue(ownRequests.any { it.id == reqId })

        // Cross-user direct get by Bob must fail with PERMISSION_DENIED
        signInTestUser(bobEmail)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("prescriptionRequests").document(reqId).get().await()
            }
            fail("Expected cross-user read to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fs = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fs)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fs?.code)
        }
    }

    @Test
    fun submitPaymentProof_validSubmission_createsPendingPayment() = runBlocking {
        val email = "payer_${UUID.randomUUID().toString().take(8)}@test.com"
        signInTestUser(email)
        val rxRepo = PrescriptionRepository(firestore)
        val payRepo = PaymentRepository(firestore)

        val reqId = withTimeout(DEFAULT_TIMEOUT_MS) {
            rxRepo.createPrescriptionRequest(
                patientName = "Dawit",
                patientPhone = "0922334455",
                prescriptionImageUrl = "https://firebasestorage.googleapis.com/v0/b/demo/o/rx2.jpg",
                medicineName = "Paracetamol",
                deliveryMode = "delivery",
                deliveryAddress = "Kazanchis",
                latitude = 9.01,
                longitude = 38.76,
                nearbyApprovedPharmacies = emptyList()
            )
        }

        val uniqueRef = "TXN-TB-${UUID.randomUUID().toString().take(8)}"
        val payId = withTimeout(DEFAULT_TIMEOUT_MS) {
            payRepo.submitPaymentProof(
                requestId = reqId,
                provider = "telebirr",
                paymentPhone = "0922334455",
                paymentReference = uniqueRef,
                screenshotUrl = "https://firebasestorage.googleapis.com/v0/b/demo/o/receipt.jpg"
            )
        }
        assertTrue(payId.isNotBlank())

        val payments = withTimeout(FLOW_TIMEOUT_MS) {
            payRepo.observeMyPayments().first { list -> list.any { it.id == payId } }
        }
        val created = payments.first { it.id == payId }
        assertEquals("PENDING", created.verificationStatus)
        assertEquals("+251925694548", created.recipientNumber)
    }

    @Test
    fun observeMyPrescriptionRequests_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val rxRepo = PrescriptionRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                rxRepo.observeMyPrescriptionRequests().first()
            }
            fail("Expected unauthenticated observer to throw")
        } catch (e: Exception) {
            // Repository either throws IllegalStateException ("Please sign in with Google first.")
            // or Firestore PERMISSION_DENIED when querying unauthenticated
            val fs = e.unwrapFirestoreException()
            assertTrue(
                fs?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    e is IllegalStateException
            )
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
