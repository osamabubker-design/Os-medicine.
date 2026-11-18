package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.OsMedicineMessagingService
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.firebase.signOut
import com.example.data.repositories.AiMedicineRepository
import com.example.data.repositories.ChatRepository
import com.example.data.repositories.NotificationRepository
import com.example.data.repositories.PaymentRepository
import com.example.data.repositories.PharmacyRepository
import com.example.data.repositories.PrescriptionRepository
import com.example.data.repositories.StorageRepository
import com.example.data.repositories.UserRepository
import com.example.ui.components.AiAssistantSheet
import com.example.ui.components.NotificationsSheet
import com.example.ui.components.OsMedicineHeader
import com.example.ui.components.PaymentBottomSheet
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PatientScreen
import com.example.ui.screens.PharmacyScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.OsMedicineTheme
import com.example.ui.theme.OsPrimary
import com.example.ui.viewmodel.ActionUiState
import com.example.ui.viewmodel.MainNavTab
import com.example.ui.viewmodel.OsMedicineViewModel
import com.example.utils.Localization
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(this, intent)

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val credentialManager = remember(context) { CredentialManager.create(context) }

            val authUser by remember { Firebase.auth.authStateFlow() }
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

            var authBusy by remember { mutableStateOf(false) }
            var authError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                attemptAutoSignIn(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = { authError = null },
                    onUnauthenticated = {},
                    scope = scope
                )
            }

            if (authUser == null) {
                OsMedicineTheme(darkTheme = false, isAmharic = false) {
                    SignInScreen(
                        isLoading = authBusy,
                        errorMessage = authError,
                        onGoogleSignIn = {
                            authBusy = true
                            authError = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    authBusy = false
                                    authError = null
                                },
                                onAuthError = { msg ->
                                    authBusy = false
                                    authError = msg
                                },
                                onAuthCancelled = {
                                    authBusy = false
                                    authError = "Google Sign-In was cancelled. Please select a Google account on your device/emulator to continue."
                                },
                                scope = scope
                            )
                        }
                    )
                }
            } else {
                val uid = authUser!!.uid
                val appContext = context.applicationContext
                val firestoreDb = remember(appContext) {
                    FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
                }
                val viewModel: OsMedicineViewModel = viewModel(
                    key = uid,
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return OsMedicineViewModel(
                                currentUserId = uid,
                                userRepo = UserRepository(firestoreDb),
                                pharmacyRepo = PharmacyRepository(firestoreDb),
                                prescriptionRepo = PrescriptionRepository(firestoreDb),
                                paymentRepo = PaymentRepository(firestoreDb),
                                chatRepo = ChatRepository(firestoreDb),
                                notificationRepo = NotificationRepository(firestoreDb),
                                storageRepo = StorageRepository(appContext),
                                aiRepo = AiMedicineRepository()
                            ) as T
                        }
                    }
                )

                var switchAccountMessage by remember { mutableStateOf<String?>(null) }

                OsMedicineAppContent(
                    viewModel = viewModel,
                    externalMessage = switchAccountMessage,
                    onConsumeExternalMessage = { switchAccountMessage = null },
                    onSignOut = {
                        signOut(credentialManager = credentialManager, scope = scope)
                    },
                    onSwitchAccount = {
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = { viewModel.verifyAdminAccess() },
                            onAuthError = { msg -> switchAccountMessage = msg },
                            onAuthCancelled = { switchAccountMessage = "Account switch cancelled." },
                            scope = scope
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun OsMedicineAppContent(
    viewModel: OsMedicineViewModel,
    externalMessage: String?,
    onConsumeExternalMessage: () -> Unit,
    onSignOut: () -> Unit,
    onSwitchAccount: () -> Unit
) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val adminSessionVerified by viewModel.adminSessionVerified.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val paymentUploadState by viewModel.paymentUploadState.collectAsStateWithLifecycle()

    val currentUser by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val approvedPharmacies by viewModel.approvedPharmacies.collectAsStateWithLifecycle()
    val myPharmacy by viewModel.myPharmacy.collectAsStateWithLifecycle()
    val approvedMedicines by viewModel.approvedMedicines.collectAsStateWithLifecycle()
    val myMedicines by viewModel.myPharmacyMedicines.collectAsStateWithLifecycle()
    val myRequests by viewModel.myPrescriptionRequests.collectAsStateWithLifecycle()
    val incomingRequests by viewModel.incomingPharmacyRequests.collectAsStateWithLifecycle()
    val patientResponses by viewModel.patientResponses.collectAsStateWithLifecycle()
    val ownResponses by viewModel.pharmacyOwnResponses.collectAsStateWithLifecycle()
    val myPayments by viewModel.myPayments.collectAsStateWithLifecycle()
    val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
    val activeChatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val notifications by viewModel.userNotifications.collectAsStateWithLifecycle()
    val adminNotifications by viewModel.adminNotifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()

    val adminPharmacies by viewModel.adminAllPharmacies.collectAsStateWithLifecycle()
    val adminRequests by viewModel.adminAllRequests.collectAsStateWithLifecycle()
    val adminPayments by viewModel.adminAllPayments.collectAsStateWithLifecycle()
    val adminLogs by viewModel.adminLogs.collectAsStateWithLifecycle()

    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val aiLoading by viewModel.aiLoading.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()

    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var paymentTargetRequestId by remember { mutableStateOf<String?>(null) }
    var submittedPaymentRef by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = Localization.forLang(language)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val seenNotifIds = remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(notifications) {
        val firstRun = seenNotifIds.value.isEmpty()
        val unread = notifications.filter { !it.isRead }
        if (!firstRun) {
            unread.filter { it.id !in seenNotifIds.value }.forEach { notif ->
                OsMedicineMessagingService.showLocalNotification(
                    context = context,
                    title = notif.title,
                    body = notif.message,
                    notificationId = notif.id.hashCode()
                )
            }
        }
        seenNotifIds.value = notifications.map { it.id }.toSet()
    }

    LaunchedEffect(externalMessage) {
        if (!externalMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(externalMessage)
            onConsumeExternalMessage()
        }
    }

    LaunchedEffect(actionState) {
        when (val st = actionState) {
            is ActionUiState.Success -> {
                snackbarHostState.showSnackbar(st.message)
                viewModel.clearActionState()
            }
            is ActionUiState.Error -> {
                snackbarHostState.showSnackbar(st.message)
                viewModel.clearActionState()
            }
            else -> Unit
        }
    }

    BackHandler(enabled = activeTab != MainNavTab.HOME) {
        viewModel.selectTab(MainNavTab.HOME)
    }

    val combinedNotifications = remember(notifications, adminNotifications, isAdmin) {
        if (isAdmin) (notifications + adminNotifications).distinctBy { it.id }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        else notifications
    }
    val totalUnread = if (isAdmin) combinedNotifications.count { !it.isRead } else unreadCount

    OsMedicineTheme(darkTheme = darkTheme, isAmharic = language == "am") {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                OsMedicineHeader(
                    langToggleLabel = strings.langToggle,
                    isDarkTheme = darkTheme,
                    unreadNotifications = totalUnread,
                    onLogoClick = { viewModel.selectTab(MainNavTab.HOME) },
                    onNotificationsClick = { showNotificationsSheet = true },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onToggleTheme = { viewModel.toggleDarkTheme() }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    val items = listOf(
                        Triple(MainNavTab.HOME, strings.navHome, Icons.Filled.Home to Icons.Outlined.Home),
                        Triple(MainNavTab.PATIENT, strings.navPatient, Icons.Filled.Person to Icons.Outlined.Person),
                        Triple(MainNavTab.PHARMACY, strings.navPharmacy, Icons.Filled.LocalPharmacy to Icons.Outlined.LocalPharmacy),
                        Triple(MainNavTab.ADMIN, strings.navAdmin, Icons.Filled.AdminPanelSettings to Icons.Outlined.AdminPanelSettings)
                    )
                    items.forEach { (tab, label, icons) ->
                        val selected = activeTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) icons.first else icons.second,
                                    contentDescription = label
                                )
                            },
                            label = { Text(label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OsPrimary,
                                selectedTextColor = OsPrimary,
                                indicatorColor = OsPrimary.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    MainNavTab.HOME -> HomeScreen(
                        strings = strings,
                        approvedPharmacies = approvedPharmacies,
                        onNavigateTab = { viewModel.selectTab(it) },
                        onOpenAiHelper = { showAiSheet = true }
                    )

                    MainNavTab.PATIENT -> PatientScreen(
                        strings = strings,
                        currentUser = currentUser,
                        approvedPharmacies = approvedPharmacies,
                        approvedMedicines = approvedMedicines,
                        myRequests = myRequests,
                        patientResponses = patientResponses,
                        myPayments = myPayments,
                        uploadState = uploadState,
                        activeChatId = activeChatId,
                        activeChatMessages = activeChatMessages,
                        onUploadPrescription = { viewModel.uploadPrescriptionPhoto(it) },
                        onAnalyzeRxWithAi = { uri ->
                            showAiSheet = true
                            viewModel.analyzePrescriptionWithAi(uri)
                        },
                        onSubmitRequest = { name, phone, imgUrl, med, mode, addr, lat, lng, cb ->
                            viewModel.submitPrescriptionRequest(
                                patientName = name,
                                patientPhone = phone,
                                prescriptionImageUrl = imgUrl,
                                medicineName = med,
                                deliveryMode = mode,
                                deliveryAddress = addr,
                                latitude = lat,
                                longitude = lng,
                                onSubmitted = cb
                            )
                        },
                        onOpenPaymentForRequest = { reqId ->
                            viewModel.resetPaymentUpload()
                            submittedPaymentRef = null
                            paymentTargetRequestId = reqId
                        },
                        onOpenChat = { reqId, phId -> viewModel.openChat(reqId, phId) },
                        onSendChatMessage = { reqId, patId, patName, phId, ownerId, phName, text ->
                            viewModel.sendChatMessage(
                                reqId, patId, patName, phId, ownerId, phName, "patient", text
                            )
                        },
                        onRatePharmacy = { ph, reqId, score, comment ->
                            viewModel.ratePharmacy(ph, reqId, score, comment)
                        }
                    )

                    MainNavTab.PHARMACY -> PharmacyScreen(
                        strings = strings,
                        myPharmacy = myPharmacy,
                        incomingRequests = incomingRequests,
                        ownResponses = ownResponses,
                        myMedicines = myMedicines,
                        activeChatId = activeChatId,
                        activeChatMessages = activeChatMessages,
                        onRegisterPharmacy = { name, pharmacist, phone, email, addr, lat, lng, lic ->
                            viewModel.registerPharmacy(name, pharmacist, phone, email, addr, lat, lng, lic)
                        },
                        onUpdatePharmacyProfile = { id, name, pharmacist, phone, addr, avail, lat, lng ->
                            viewModel.updatePharmacyProfile(id, name, pharmacist, phone, addr, avail, lat, lng)
                        },
                        onRespondToRequest = { req, ph, avail ->
                            viewModel.respondToPrescription(req, ph, avail)
                        },
                        onUpdateResponseDetails = { id, med, price, qty, fee, status ->
                            viewModel.updateResponseDetails(id, med, price, qty, fee, status)
                        },
                        onSaveMedicine = { viewModel.saveMedicineInventory(it) },
                        onOpenChat = { reqId, phId -> viewModel.openChat(reqId, phId) },
                        onSendChatMessage = { reqId, patId, patName, phId, ownerId, phName, text ->
                            viewModel.sendChatMessage(
                                reqId, patId, patName, phId, ownerId, phName, "pharmacy", text
                            )
                        },
                        onSignOut = onSignOut
                    )

                    MainNavTab.ADMIN -> AdminScreen(
                        strings = strings,
                        isAdminAuthorized = isAdmin,
                        adminSessionVerified = adminSessionVerified,
                        allPharmacies = adminPharmacies,
                        allRequests = adminRequests,
                        allPayments = adminPayments,
                        adminLogs = adminLogs,
                        onVerifyAdminAccess = { viewModel.verifyAdminAccess() },
                        onLockAdminSession = { viewModel.lockAdminSession() },
                        onSwitchGoogleAccount = onSwitchAccount,
                        onVerifyPayment = { payment, approve, reason ->
                            viewModel.adminVerifyPayment(payment, approve, reason)
                        },
                        onUpdatePharmacyStatus = { ph, approval, license ->
                            viewModel.adminUpdatePharmacy(ph, approval, license)
                        }
                    )
                }
            }
        }

        paymentTargetRequestId?.let { targetReqId ->
            PaymentBottomSheet(
                defaultPhone = currentUser?.phoneNumber.orEmpty(),
                uploadState = paymentUploadState,
                isSubmitting = actionState is ActionUiState.Loading,
                submittedReference = submittedPaymentRef,
                onUploadScreenshot = { viewModel.uploadPaymentScreenshot(it) },
                onSubmitPayment = { provider, phone, ref, screenshotUrl ->
                    viewModel.submitTelebirrPayment(
                        requestId = targetReqId,
                        provider = provider,
                        paymentPhone = phone,
                        paymentReference = ref,
                        screenshotUrl = screenshotUrl,
                        onSuccess = {
                            submittedPaymentRef = ref
                        }
                    )
                },
                onDismiss = {
                    paymentTargetRequestId = null
                    submittedPaymentRef = null
                    viewModel.resetPaymentUpload()
                },
                onCopiedToast = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Copied telebirr number ${PaymentRepository.OFFICIAL_TELEBIRR_NUMBER}")
                    }
                }
            )
        }

        if (showNotificationsSheet) {
            NotificationsSheet(
                strings = strings,
                notifications = combinedNotifications,
                onNotificationClick = { notif ->
                    showNotificationsSheet = false
                    viewModel.markNotificationRead(notif)
                },
                onClearRead = { viewModel.clearReadNotifications() },
                onDismiss = { showNotificationsSheet = false }
            )
        }

        if (showAiSheet) {
            AiAssistantSheet(
                strings = strings,
                messages = aiMessages,
                isLoading = aiLoading,
                errorMessage = aiError,
                onSendMessage = { viewModel.sendAiMessage(it) },
                onDismiss = { showAiSheet = false }
            )
        }
    }
}
