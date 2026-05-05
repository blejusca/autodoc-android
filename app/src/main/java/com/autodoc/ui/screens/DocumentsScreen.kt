package com.autodoc.ui.screens

import com.autodoc.R

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodoc.ui.AppColors
import com.autodoc.ui.CarUi
import com.autodoc.ui.DocumentUi
import com.autodoc.ui.formatDate
import com.autodoc.ui.localizedText
import kotlin.math.abs
import androidx.compose.ui.res.stringResource

private enum class DocumentFilter {
    ALL,
    EXPIRED,
    URGENT,
    SOON,
    OK
}

private data class DocumentWithCar(
    val car: CarUi,
    val document: DocumentUi
)

@Composable
fun DocumentsScreen(
    cars: List<CarUi>,
    onMarkDocumentManuallyNotified: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val activeFilter = remember { mutableStateOf(DocumentFilter.ALL) }

    val documents = remember(cars) {
        cars.flatMap { car ->
            car.documents.map { document ->
                DocumentWithCar(car, document)
            }
        }.sortedBy { it.document.daysLeft }
    }

    val filteredDocuments = remember(documents, activeFilter.value) {
        when (activeFilter.value) {
            DocumentFilter.ALL -> documents
            DocumentFilter.EXPIRED -> documents.filter { it.document.daysLeft < 0 }
            DocumentFilter.URGENT -> documents.filter { it.document.daysLeft in 0..7 }
            DocumentFilter.SOON -> documents.filter { it.document.daysLeft in 8..30 }
            DocumentFilter.OK -> documents.filter { it.document.daysLeft > 30 }
        }
    }

    val expiredCount = documents.count { it.document.daysLeft < 0 }
    val urgentCount = documents.count { it.document.daysLeft in 0..7 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.DeepBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.Navy),
            border = BorderStroke(1.dp, AppColors.Border),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.loc_documents),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "${documents.size} total • $expiredCount ${stringResource(R.string.loc_expired_2)} • $urgentCount ${stringResource(R.string.loc_urgent_2)}",
                    color = AppColors.Gold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipButton(stringResource(R.string.loc_all), activeFilter.value == DocumentFilter.ALL, Modifier.weight(1f)) {
                activeFilter.value = DocumentFilter.ALL
            }

            FilterChipButton(stringResource(R.string.loc_expired), activeFilter.value == DocumentFilter.EXPIRED, Modifier.weight(1f)) {
                activeFilter.value = DocumentFilter.EXPIRED
            }

            FilterChipButton(stringResource(R.string.loc_urgent), activeFilter.value == DocumentFilter.URGENT, Modifier.weight(1f)) {
                activeFilter.value = DocumentFilter.URGENT
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipButton(stringResource(R.string.loc_soon), activeFilter.value == DocumentFilter.SOON, Modifier.weight(1f)) {
                activeFilter.value = DocumentFilter.SOON
            }

            FilterChipButton("OK", activeFilter.value == DocumentFilter.OK, Modifier.weight(1f)) {
                activeFilter.value = DocumentFilter.OK
            }
        }

        if (filteredDocuments.isEmpty()) {
            EmptyDocumentsCard(activeFilter.value)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredDocuments,
                    key = { it.document.id }
                ) { item ->
                    DocumentCard(
                        car = item.car,
                        document = item.document,
                        isNotified = item.document.manuallyNotified,
                        onSendWhatsApp = {
                            val opened = notifyClient(context, item.car, item.document)

                            if (opened) {
                                onMarkDocumentManuallyNotified(item.document.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(
    car: CarUi,
    document: DocumentUi,
    isNotified: Boolean,
    onSendWhatsApp: () -> Unit
) {
    val hasPhone = car.ownerPhone.isNotBlank()
    val needsNotification = document.daysLeft <= 7
    val statusColor = getStatusColor(document.daysLeft)
    val statusText = getStatusText(document.daysLeft)
    val expiryLineText = getExpiryLineText(document)
    val canNotify = hasPhone && needsNotification && !isNotified
    val isUrgentActive = document.daysLeft in 0..7 && !isNotified

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(
            width = if (isUrgentActive) 2.dp else 1.dp,
            color = if (isNotified) AppColors.Gold else statusColor
        ),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = document.type,
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                StatusBadge(
                    text = if (isNotified) stringResource(R.string.loc_notified) else statusText,
                    color = if (isNotified) AppColors.Gold else statusColor,
                    darkText = isNotified
                )
            }

            Text(
                text = "${car.brand} ${car.model}",
                color = AppColors.SoftText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = car.plate,
                color = AppColors.Gold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.Navy),
                border = BorderStroke(1.dp, AppColors.Border),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = expiryLineText,
                        color = if (document.daysLeft < 0) AppColors.Danger else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${stringResource(R.string.loc_client)}: ${car.ownerName.ifBlank { stringResource(R.string.loc_not_specified_2) }}",
                        color = AppColors.MutedText,
                        fontSize = 13.sp
                    )

                    Text(
                        text = if (hasPhone) "${stringResource(R.string.loc_phone)}: ${car.ownerPhone}" else stringResource(R.string.loc_phone_missing),
                        color = if (hasPhone) AppColors.MutedText else AppColors.Warning,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TextButton(
                onClick = onSendWhatsApp,
                enabled = canNotify,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = when {
                        isNotified -> AppColors.Gold
                        !needsNotification -> Color(0xFF4B5563)
                        hasPhone -> AppColors.Gold
                        else -> Color(0xFF4B5563)
                    },
                    disabledContainerColor = when {
                        isNotified -> AppColors.Gold
                        else -> Color(0xFF4B5563)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = when {
                        isNotified -> stringResource(R.string.loc_notified_2)
                        !needsNotification -> stringResource(R.string.loc_no_notification_needed)
                        hasPhone -> stringResource(R.string.loc_notify_client)
                        else -> stringResource(R.string.loc_phone_missing)
                    },
                    color = if (isNotified || (needsNotification && hasPhone)) AppColors.Navy else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FilterChipButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(42.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AppColors.Gold else AppColors.CardBg
        ),
        border = if (selected) null else BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(50.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = if (selected) AppColors.Navy else AppColors.SoftText,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    color: Color,
    darkText: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            color = if (darkText) AppColors.Navy else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyDocumentsCard(filter: DocumentFilter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.loc_no_documents_match_the_selected_filter),
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${stringResource(R.string.loc_active_filter)}: ${filter.name}",
                color = AppColors.SoftText,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private fun notifyClient(
    context: Context,
    car: CarUi,
    document: DocumentUi
): Boolean {
    if (car.ownerPhone.isBlank()) {
        Toast.makeText(context, context.localizedText("Client fără telefon.", "Client has no phone number."), Toast.LENGTH_LONG).show()
        return false
    }

    if (document.daysLeft > 7) {
        Toast.makeText(context, context.localizedText("Documentul nu necesită notificare.", "This document does not require notification."), Toast.LENGTH_LONG).show()
        return false
    }

    val phone = normalizePhone(car.ownerPhone)

    if (phone.isBlank()) {
        Toast.makeText(context, context.localizedText("Număr de telefon invalid.", "Invalid phone number."), Toast.LENGTH_LONG).show()
        return false
    }

    val message = buildClientMessage(car, document)
    val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")

    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.localizedText("WhatsApp indisponibil.", "WhatsApp is unavailable."), Toast.LENGTH_LONG).show()
        false
    }
}

private fun buildClientMessage(
    car: CarUi,
    document: DocumentUi
): String {
    val expiryMessage = if (document.daysLeft < 0) {
        localizedText("a expirat la data de ${formatDate(document.expiryDateMillis)}", "expired on ${formatDate(document.expiryDateMillis)}")
    } else {
        localizedText("expiră la data de ${formatDate(document.expiryDateMillis)}", "expires on ${formatDate(document.expiryDateMillis)}")
    }

    return if (localizedText("ro", "en") == "en") {
        """
Hello,

This is a reminder that the vehicle document ${document.type} for ${car.brand} ${car.model}, registration number ${car.plate}, $expiryMessage.

Status: ${getStatusText(document.daysLeft)}

Please check it in time.

Thank you,
CarGuard Business
""".trimIndent()
    } else {
        """
Bună ziua,

Vă reamintim că documentul auto ${document.type} pentru mașina ${car.brand} ${car.model}, număr ${car.plate}, $expiryMessage.

Status: ${getStatusText(document.daysLeft)}

Vă recomandăm să îl verificați din timp.

Mulțumim,
CarGuard Business
""".trimIndent()
    }
}

private fun normalizePhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }

    return when {
        digits.isBlank() -> ""
        digits.startsWith("00") -> digits.drop(2)
        digits.startsWith("40") -> digits
        digits.startsWith("0") -> "40" + digits.drop(1)
        else -> digits
    }
}

private fun getExpiryLineText(document: DocumentUi): String {
    return when {
        document.daysLeft < 0 -> "${localizedText("Expirat la", "Expired on")}: ${formatDate(document.expiryDateMillis)}"
        document.daysLeft == 0 -> "${localizedText("Expiră azi", "Expires today")}: ${formatDate(document.expiryDateMillis)}"
        else -> "${localizedText("Expiră la", "Expires on")}: ${formatDate(document.expiryDateMillis)}"
    }
}

private fun getStatusText(daysLeft: Int): String {
    return when {
        daysLeft < 0 -> {
            val days = abs(daysLeft)
            if (days == 1) localizedText("Expirat de 1 zi", "Expired 1 day ago") else localizedText("Expirat de $days zile", "Expired $days days ago")
        }

        daysLeft == 0 -> localizedText("Expiră azi", "Expires today")
        daysLeft in 1..7 -> localizedText("Expiră în $daysLeft zile", "Expires in $daysLeft days")
        daysLeft in 8..30 -> localizedText("Curând", "Soon")
        else -> "OK"
    }
}

private fun getStatusColor(daysLeft: Int): Color {
    return when {
        daysLeft < 0 -> AppColors.Danger
        daysLeft in 0..7 -> AppColors.Danger
        daysLeft in 8..30 -> AppColors.Warning
        else -> AppColors.Ok
    }
}