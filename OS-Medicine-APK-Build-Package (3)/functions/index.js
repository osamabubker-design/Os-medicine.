const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

// Matches the provisioned named Firestore database ID
const DATABASE_ID = "ai-studio-android-osmedici-5089c39f-6c8b-47c5-a948-77e908c0da5f";
const db = getFirestore(DATABASE_ID);

async function sendPushToUser(userId, title, body, dataPayload = {}) {
  try {
    const userDoc = await db.collection("users").doc(userId).get();
    const token = userDoc.exists ? userDoc.data().fcmToken : null;
    if (!token) return;
    await getMessaging().send({
      token,
      notification: { title, body },
      data: dataPayload,
      android: {
        priority: "high",
        notification: { channelId: "os_medicine_alerts" },
      },
    });
  } catch (err) {
    console.error("FCM push delivery error:", err);
  }
}

// Dispatch FCM push whenever a notification document is created
exports.onNotificationCreated = onDocumentCreated(
  { document: "notifications/{notificationId}", database: DATABASE_ID },
  async (event) => {
    const data = event.data?.data();
    if (!data) return;
    // Privacy: keep sensitive prescription/payment details out of lock-screen notifications
    await sendPushToUser(
      data.recipientId,
      data.title || "OS Medicine Update",
      data.message || "You have a new update.",
      {
        notificationId: event.params.notificationId,
        notificationType: data.notificationType || "",
        relatedRequestId: data.relatedRequestId || "",
        relatedPaymentId: data.relatedPaymentId || "",
        relatedOrderId: data.relatedOrderId || "",
        actionType: data.actionType || "",
      }
    );
  }
);

// Notify patient when admin verifies or rejects a Telebirr/Chapa/CBE Birr payment
exports.onPaymentStatusUpdated = onDocumentUpdated(
  { document: "payments/{paymentId}", database: DATABASE_ID },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after || before.verificationStatus === after.verificationStatus) return;

    const approved = after.verificationStatus === "APPROVED";
    const rejected = after.verificationStatus === "REJECTED";
    if (!approved && !rejected) return;

    if (approved && after.requestId) {
      await db.collection("prescriptionRequests").doc(after.requestId).update({
        paymentUnlocked: true,
        requestStatus: "UNLOCKED",
        paymentId: event.params.paymentId,
      });
    }

    await db.collection("notifications").add({
      recipientId: after.userId,
      recipientRole: "patient",
      senderId: after.adminId || "system",
      title: approved ? "Your Telebirr payment has been approved" : "Your Telebirr payment was rejected",
      message: approved
        ? "Location and Google Maps directions are now unlocked."
        : (after.rejectionReason || "Please review your payment reference and try again."),
      notificationType: approved ? "PAYMENT_APPROVED" : "PAYMENT_REJECTED",
      relatedRequestId: after.requestId || "",
      relatedPaymentId: event.params.paymentId,
      relatedOrderId: after.orderId || "",
      isRead: false,
      actionType: "OPEN_REQUEST",
      createdAt: FieldValue.serverTimestamp(),
    });
  }
);
