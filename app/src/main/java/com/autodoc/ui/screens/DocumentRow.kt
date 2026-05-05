package com.autodoc.ui.screens

import com.autodoc.R

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodoc.ui.AppColors
import com.autodoc.ui.CarUi
import com.autodoc.ui.DocumentSeverity
import com.autodoc.ui.DocumentUi
import com.autodoc.ui.documentStatusText
import com.autodoc.ui.formatDate
import com.autodoc.ui.localizedText
import com.autodoc.ui.parseDate
import com.autodoc.ui.parseDateToMillis
import com.autodoc.ui.severity
import com.autodoc.ui.shouldNotifyClient
import java.time.LocalDate
import androidx.compose.ui.res.stringResource

@Composable
fun DocumentRow(
    car: CarUi,
    document: DocumentUi,
    onDeleteDocument: (documentId: Int) -> Unit,
    onUpdateDocumentExpiry: (documentId: Int, expiryDateMillis: Long) -> Unit
) {
    val context = LocalContext.current
    val showEdit = remember { mutableStateOf(false) }
    val newDate = remember { mutableStateOf("") }
    val editDateError = remember { mutableStateOf("") }
    val showDeleteDocumentDialog = remember { mutableStateOf(false) }

    val shouldNotify = document.shouldNotifyClient()
    val hasPhone = car.ownerPhone.isNotBlank()
    val isManuallyNotified = document.manuallyNotified
    val canNotify = shouldNotify && hasPhone && !isManuallyNotified

    if (showDeleteDocumentDialog.value) {
        ConfirmDeleteDocumentDialog(
            title = stringResource(R.string.loc_delete_document),
            message = stringResource(R.string.loc_are_you_sure_you_want_to_delete_document_type, document.type),
            onConfirm = {
                showDeleteDocumentDialog.value = false
                onDeleteDocument(document.id)
            },
            onDismiss = {
                showDeleteDocumentDialog.value = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Navy),
        border = BorderStroke(1.dp, statusColor(document)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DocumentInfo(document = document)

            Divider(color = AppColors.Border)

            DocumentActionButtons(
                isEditing = showEdit.value,
                onToggleEdit = {
                    showEdit.value = !showEdit.value
                    editDateError.value = ""
                },
                onRequestDelete = {
                    showDeleteDocumentDialog.value = true
                }
            )

            WhatsAppNotifyButton(
                canNotify = canNotify,
                shouldNotify = shouldNotify,
                hasPhone = hasPhone,
                isManuallyNotified = isManuallyNotified,
                onClick = {
                    sendWhatsAppNotification(context, car, document)
                }
            )

            if (showEdit.value) {
                EditDocumentDateSection(
                    newDate = newDate.value,
                    errorMessage = editDateError.value,
                    onDateChange = { newDate.value = it },
                    onSave = {
                        val validationError = validateExpiryDate(newDate.value)
                        val millis = parseDateToMillis(newDate.value)
                        editDateError.value = validationError
                        if (validationError.isBlank() && millis != null) {
                            onUpdateDocumentExpiry(document.id, millis)
                            newDate.value = ""
                            editDateError.value = ""
                            showEdit.value = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DocumentInfo(document: DocumentUi) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = document.type,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            maxLines = 1
        )
        Text(
            text = documentStatusText(document.daysLeft),
            color = statusColor(document),
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            lineHeight = 19.sp
        )
        Text(
            text = "${stringResource(R.string.loc_expires_on)}: ${formatDate(document.expiryDateMillis)}",
            color = AppColors.MutedText,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DocumentActionButtons(
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onRequestDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onToggleEdit,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(42.dp)
        ) {
            Text(
                text = if (isEditing) stringResource(R.string.loc_close) else stringResource(R.string.loc_edit),
                color = AppColors.Navy,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
        Button(
            onClick = onRequestDelete,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(42.dp)
        ) {
            Text(
                text = stringResource(R.string.loc_delete),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WhatsAppNotifyButton(
    canNotify: Boolean,
    shouldNotify: Boolean,
    hasPhone: Boolean,
    isManuallyNotified: Boolean,
    onClick: () -> Unit
) {
    val text = when {
        isManuallyNotified -> stringResource(R.string.loc_client_notified)
        !shouldNotify -> stringResource(R.string.loc_no_notification_needed)
        !hasPhone -> stringResource(R.string.loc_client_phone_missing)
        else -> stringResource(R.string.loc_notify_on_whatsapp)
    }

    val containerColor = when {
        isManuallyNotified -> AppColors.Ok
        canNotify -> AppColors.Ok
        else -> Color(0xFF6B7280)
    }

    Button(
        onClick = onClick,
        enabled = canNotify,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = if (isManuallyNotified) AppColors.Ok else Color(0xFF6B7280)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(44.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun EditDocumentDateSection(
    newDate: String,
    errorMessage: String,
    onDateChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = AppColors.Danger,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
        }
        DatePickerDarkField(
            value = newDate,
            onChange = onDateChange,
            label = stringResource(R.string.loc_new_date)
        )
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Ok),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Text(
                text = stringResource(R.string.loc_save_new_date),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ConfirmDeleteDocumentDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger)
            ) {
                Text(text = stringResource(R.string.loc_yes_delete), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold)
            ) {
                Text(text = stringResource(R.string.loc_cancel), color = AppColors.Navy, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun validateExpiryDate(value: String): String {
    val date = parseDate(value)
    val today = LocalDate.now()
    val maxDate = today.plusYears(10)
    return when {
        value.trim().isBlank() -> localizedText("Data expirării este obligatorie.", "Expiry date is required.")
        date == null -> localizedText("Data expirării nu este validă. Folosește selectorul de dată sau formatul yyyy-MM-dd.", "Invalid expiry date. Use the date picker or yyyy-MM-dd format.")
        date.isBefore(today.minusYears(5)) -> localizedText("Data expirării este prea veche.", "Expiry date is too old.")
        date.isAfter(maxDate) -> localizedText("Data expirării este prea departe în viitor. Maximum 10 ani.", "Expiry date is too far in the future. Maximum 10 years.")
        else -> ""
    }
}

private fun statusColor(document: DocumentUi): Color {
    return when (document.severity()) {
        DocumentSeverity.EXPIRED -> AppColors.Danger
        DocumentSeverity.CRITICAL -> AppColors.Danger
        DocumentSeverity.SOON -> AppColors.Warning
        DocumentSeverity.OK -> AppColors.Ok
    }
}

private fun sendWhatsAppNotification(context: Context, car: CarUi, document: DocumentUi) {
    val phone = normalizePhoneForWhatsApp(car.ownerPhone)
    if (phone.isBlank()) return
    val message = buildWhatsAppMessage(car, document)
    val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

private fun buildWhatsAppMessage(car: CarUi, document: DocumentUi): String {
    val expiryDate = formatDate(document.expiryDateMillis)
    val status = documentStatusText(document.daysLeft)
    return if (localizedText("ro", "en") == "en") {
        val greeting = if (car.ownerName.isNotBlank()) "Hello, ${car.ownerName}," else "Hello,"
        """
$greeting

We are contacting you about the following vehicle document:

Document: ${document.type}
Vehicle: ${car.brand} ${car.model}
Registration number: ${car.plate}
Expiry date: $expiryDate
Status: $status

Please check and renew the document in time to avoid fines or legal issues.

Thank you,
CarGuard Business
""".trimIndent()
    } else {
        val greeting = if (car.ownerName.isNotBlank()) "Bună ziua, ${car.ownerName}," else "Bună ziua,"
        """
$greeting

Vă contactăm în legătură cu documentul auto:

Document: ${document.type}
Mașină: ${car.brand} ${car.model}
Număr înmatriculare: ${car.plate}
Data expirării: $expiryDate
Status: $status

Vă recomandăm să verificați și să reînnoiți documentul în timp util, pentru a evita amenzi sau probleme legale.

Mulțumim,
CarGuard Business
""".trimIndent()
    }
}

private fun normalizePhoneForWhatsApp(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return when {
        digits.isBlank() -> ""
        digits.startsWith("00") -> digits.drop(2)
        digits.startsWith("40") -> digits
        digits.startsWith("0") -> "40" + digits.drop(1)
        else -> digits
    }
}
