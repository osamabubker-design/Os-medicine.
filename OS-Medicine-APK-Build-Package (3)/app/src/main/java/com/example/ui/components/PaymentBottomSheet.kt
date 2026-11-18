package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repositories.PaymentRepository
import com.example.data.repositories.UploadState
import com.example.ui.theme.LocalOsExtraColors
import com.example.ui.theme.OsCbePurple
import com.example.ui.theme.OsPrimary
import com.example.ui.theme.OsSuccess
import com.example.ui.theme.OsTelebirrBlue
import com.example.ui.theme.OsTelebirrSurface
import com.example.utils.GeoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentBottomSheet(
    defaultPhone: String,
    uploadState: UploadState,
    isSubmitting: Boolean,
    submittedReference: String?,
    onUploadScreenshot: (Uri) -> Unit,
    onSubmitPayment: (provider: String, phone: String, reference: String, screenshotUrl: String) -> Unit,
    onDismiss: () -> Unit,
    onCopiedToast: () -> Unit
) {
    val context = LocalContext.current
    val extras = LocalOsExtraColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedProvider by remember { mutableStateOf("telebirr") }
    var sendingPhone by remember { mutableStateOf(defaultPhone) }
    var txReference by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            validationError = null
            onUploadScreenshot(uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            if (submittedReference != null) {
                // Confirmation state matching Screenshot_20261109_010218
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(OsSuccess),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Payment submitted",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Screenshot Sent to Admin!",
                        style = MaterialTheme.typography.titleLarge,
                        color = OsSuccess,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Reference: $submittedReference · Payee: ${PaymentRepository.OFFICIAL_TELEBIRR_NUMBER}",
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.muted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Admin will verify your 50 ETB payment and unlock the pharmacy location & Google Maps directions shortly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = OsPrimary),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Payment selection & proof submission matching Screenshot_20261109_010206
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(OsPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = OsPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Unlock Pharmacy Location & Google Maps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Transfer 50 ETB to telebirr: ${PaymentRepository.OFFICIAL_TELEBIRR_NUMBER} & upload screenshot proof",
                            style = MaterialTheme.typography.bodySmall,
                            color = extras.muted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select Payment Method",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethodTile(
                        title = "telebirr",
                        subtitle = "Ethio Telecom",
                        icon = Icons.Filled.PhoneAndroid,
                        accent = OsTelebirrBlue,
                        selected = selectedProvider == "telebirr",
                        modifier = Modifier.weight(1f).testTag("pay_opt_telebirr"),
                        onClick = { selectedProvider = "telebirr" }
                    )
                    PaymentMethodTile(
                        title = "Chapa",
                        subtitle = "Cards & Birr",
                        icon = Icons.Filled.CreditCard,
                        accent = OsSuccess,
                        selected = selectedProvider == "chapa",
                        modifier = Modifier.weight(1f).testTag("pay_opt_chapa"),
                        onClick = { selectedProvider = "chapa" }
                    )
                    PaymentMethodTile(
                        title = "CBE Birr",
                        subtitle = "Commercial Bank",
                        icon = Icons.Filled.AccountBalance,
                        accent = OsCbePurple,
                        selected = selectedProvider == "cbe_birr",
                        modifier = Modifier.weight(1f).testTag("pay_opt_cbe"),
                        onClick = { selectedProvider = "cbe_birr" }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = OsTelebirrSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OsTelebirrBlue.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(OsTelebirrBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhoneAndroid,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "telebirr Recipient: ${PaymentRepository.OFFICIAL_TELEBIRR_NUMBER}",
                                    color = OsTelebirrBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Receiver: OS Medicine Ethiopia · Fee: 50 ETB",
                                    color = Color(0xFF334155),
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                GeoUtils.copyToClipboard(
                                    context,
                                    "OS Medicine Telebirr",
                                    PaymentRepository.OFFICIAL_TELEBIRR_NUMBER
                                )
                                onCopiedToast()
                            },
                            modifier = Modifier.testTag("copy_telebirr_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Telebirr number",
                                tint = OsTelebirrBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = sendingPhone,
                    onValueChange = { sendingPhone = it; validationError = null },
                    label = { Text("Your Sending Phone Number") },
                    placeholder = { Text("09xx xxx xxx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_phone_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = txReference,
                    onValueChange = { txReference = it; validationError = null },
                    label = { Text("Transaction Reference ID") },
                    placeholder = { Text("e.g. TXN-TB-68893") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_ref_input")
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Payment Transfer Screenshot (Sent to Admin)",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.5.dp,
                            if (uploadState is UploadState.Success) OsSuccess else extras.border,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(16.dp)
                        .testTag("payment_screenshot_picker"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Payment screenshot preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = "Attach screenshot",
                                tint = OsPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = when (uploadState) {
                                is UploadState.Uploading -> "Uploading proof (${uploadState.progressPercent}%)..."
                                is UploadState.Success -> "Screenshot uploaded & verified"
                                is UploadState.Error -> "Tap to retry screenshot upload"
                                UploadState.Idle -> "Tap to attach telebirr transfer screenshot"
                            },
                            color = if (uploadState is UploadState.Error) extras.danger else OsPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (uploadState is UploadState.Uploading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uploadState.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (uploadState is UploadState.Error) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uploadState.message,
                        color = extras.danger,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (selectedImageUri != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { selectedImageUri?.let(onUploadScreenshot) },
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry Screenshot Upload")
                        }
                    }
                }

                validationError?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = err,
                        color = extras.danger,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val uploadedUrl = (uploadState as? UploadState.Success)?.downloadUrl
                        when {
                            !GeoUtils.isValidEthiopianPhone(sendingPhone) ->
                                validationError = "Enter a valid Ethiopian phone number (e.g. 0911223344)."
                            txReference.trim().length < 3 ->
                                validationError = "Enter your Telebirr/Chapa/CBE Birr transaction reference."
                            uploadedUrl.isNullOrBlank() ->
                                validationError = "Please attach and upload your payment transfer screenshot first."
                            else -> onSubmitPayment(selectedProvider, sendingPhone.trim(), txReference.trim(), uploadedUrl)
                        }
                    },
                    enabled = !isSubmitting && uploadState !is UploadState.Uploading,
                    colors = ButtonDefaults.buttonColors(containerColor = OsPrimary),
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_payment_proof_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSubmitting) "Submitting..." else "Send Screenshot to Admin for Approval",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PaymentMethodTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val extras = LocalOsExtraColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else extras.border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = extras.muted,
            textAlign = TextAlign.Center
        )
    }
}
