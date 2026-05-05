package com.autodoc.ui.screens

import com.autodoc.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodoc.ui.AppColors
import com.autodoc.ui.DocumentUi
import com.autodoc.ui.formatDate
import com.autodoc.ui.localizedText
import com.autodoc.ui.normalizeDocumentType
import com.autodoc.ui.parseDate
import com.autodoc.ui.parseDateToMillis
import java.time.LocalDate
import androidx.compose.ui.res.stringResource

private val standardDocumentTypes = listOf(
    "ITP",
    "RCA",
    "CASCO",
    "Rovinieta",
    "Revizie"
)

@Composable
fun AddDocumentForm(
    carId: Int,
    existingDocuments: List<DocumentUi>,
    onAddDocument: (
        carId: Int,
        type: String,
        expiryDateMillis: Long,
        reminderDaysBefore: Int
    ) -> Unit
) {
    val existingTypes = existingDocuments
        .map { normalizeDocumentType(it.type) }
        .toSet()

    val firstAvailableType = standardDocumentTypes
        .firstOrNull { it !in existingTypes }
        ?: ""

    val type = remember(carId, existingTypes.size) {
        mutableStateOf(firstAvailableType)
    }

    val expiryDateText = remember(carId, existingTypes.size) {
        mutableStateOf("")
    }

    val reminderDays = remember(carId, existingTypes.size) {
        mutableStateOf("7")
    }

    val errorMessage = remember(carId, existingTypes.size) {
        mutableStateOf("")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.loc_new_document),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        if (errorMessage.value.isNotBlank()) {
            Text(
                text = errorMessage.value,
                color = AppColors.Danger,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (firstAvailableType.isBlank()) {
            Text(
                text = stringResource(R.string.loc_all_standard_document_types_have_already_been_ad),
                color = AppColors.Warning,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
            return@Column
        }

        DocumentTypeButtons(
            existingTypes = existingTypes,
            onTypeSelected = { selectedType ->
                type.value = selectedType
            }
        )

        DarkField(
            value = type.value,
            onChange = {
                type.value = normalizeDocumentType(it)
            },
            label = stringResource(R.string.loc_document_type)
        )

        if (normalizeDocumentType(type.value) in existingTypes) {
            Text(
                text = stringResource(R.string.loc_this_document_already_exists_for_the_selected_ve),
                color = AppColors.Warning,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        DatePickerDarkField(
            value = expiryDateText.value,
            onChange = {
                expiryDateText.value = it
            },
            label = stringResource(R.string.loc_expiry_date)
        )

        DarkField(
            value = reminderDays.value,
            onChange = {
                reminderDays.value = it.filter { c -> c.isDigit() }
            },
            label = stringResource(R.string.loc_notify_days_before_expiry)
        )

        Button(
            onClick = {
                val cleanType = normalizeDocumentType(type.value)
                val expiryMillis = parseDateToMillis(expiryDateText.value)

                val validationError = validateDocumentInput(
                    type = cleanType,
                    expiryDateText = expiryDateText.value,
                    reminderDaysText = reminderDays.value,
                    existingTypes = existingTypes
                )

                errorMessage.value = validationError

                if (validationError.isBlank() && expiryMillis != null) {
                    onAddDocument(
                        carId,
                        cleanType,
                        expiryMillis,
                        reminderDays.value.toIntOrNull()?.coerceIn(0, 365) ?: 7
                    )

                    val nextType = standardDocumentTypes.firstOrNull {
                        it !in existingTypes && it != cleanType
                    } ?: ""

                    type.value = nextType
                    expiryDateText.value = ""
                    reminderDays.value = "7"
                    errorMessage.value = ""
                }
            },
            enabled = normalizeDocumentType(type.value).isNotBlank() &&
                    normalizeDocumentType(type.value) !in existingTypes,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Gold,
                disabledContainerColor = Color(0xFF6B7280)
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.loc_save_document),
                color = AppColors.Navy,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DocumentTypeButtons(
    existingTypes: Set<String>,
    onTypeSelected: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallDocButton("ITP", enabled = "ITP" !in existingTypes) {
            onTypeSelected("ITP")
        }
        SmallDocButton("RCA", enabled = "RCA" !in existingTypes) {
            onTypeSelected("RCA")
        }
        SmallDocButton("CASCO", enabled = "CASCO" !in existingTypes) {
            onTypeSelected("CASCO")
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallDocButton("Rovinieta", enabled = "Rovinieta" !in existingTypes) {
            onTypeSelected("Rovinieta")
        }
        SmallDocButton("Revizie", enabled = "Revizie" !in existingTypes) {
            onTypeSelected("Revizie")
        }
    }
}

@Composable
private fun SmallDocButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Gold,
            disabledContainerColor = Color(0xFF4B5563)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) AppColors.Navy else AppColors.MutedText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DarkField(
    value: String,
    onChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = {
            Text(text = label)
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = AppColors.Gold,
            unfocusedLabelColor = AppColors.MutedText,
            focusedBorderColor = AppColors.Border,
            unfocusedBorderColor = AppColors.Border,
            cursorColor = AppColors.Gold,
            focusedContainerColor = AppColors.FieldBg,
            unfocusedContainerColor = AppColors.FieldBg
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDarkField(
    value: String,
    onChange: (String) -> Unit,
    label: String
) {
    val showPicker = remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = {
            Text(text = label)
        },
        placeholder = {
            Text(text = stringResource(R.string.loc_choose_date))
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = AppColors.Gold,
            unfocusedLabelColor = AppColors.MutedText,
            focusedBorderColor = AppColors.Border,
            unfocusedBorderColor = AppColors.Border,
            cursorColor = AppColors.Gold,
            focusedContainerColor = AppColors.FieldBg,
            unfocusedContainerColor = AppColors.FieldBg
        )
    )

    Button(
        onClick = {
            showPicker.value = true
        },
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (value.isBlank()) stringResource(R.string.loc_choose_date) else "${stringResource(R.string.loc_change_date)}: $value",
            color = AppColors.Navy,
            fontWeight = FontWeight.Bold
        )
    }

    if (showPicker.value) {
        val initialMillis = parseDateToMillis(value)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = {
                showPicker.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis

                        if (selectedMillis != null) {
                            onChange(formatDate(selectedMillis))
                        }

                        showPicker.value = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.loc_choose),
                        color = AppColors.Gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPicker.value = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.loc_cancel),
                        color = AppColors.Navy
                    )
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun validateDocumentInput(
    type: String,
    expiryDateText: String,
    reminderDaysText: String,
    existingTypes: Set<String>
): String {
    val errors = mutableListOf<String>()
    val cleanType = normalizeDocumentType(type)
    val reminderDays = reminderDaysText.trim().toIntOrNull()

    if (cleanType.isBlank()) {
        errors.add(localizedText("Tipul documentului este obligatoriu.", "Document type is required."))
    } else if (cleanType !in standardDocumentTypes) {
        errors.add(localizedText("Tipul documentului nu este valid. Alege ITP, RCA, CASCO, Rovinieta sau Revizie.", "Invalid document type. Choose ITP, RCA, CASCO, Road tax or Service."))
    } else if (cleanType in existingTypes) {
        errors.add(localizedText("Acest document există deja pentru mașina selectată.", "This document already exists for the selected vehicle."))
    }

    val dateError = validateExpiryDate(expiryDateText)
    if (dateError.isNotBlank()) {
        errors.add(dateError)
    }

    if (reminderDaysText.trim().isBlank()) {
        errors.add(localizedText("Numărul de zile pentru notificare este obligatoriu.", "Notification days are required."))
    } else if (reminderDays == null) {
        errors.add(localizedText("Numărul de zile pentru notificare trebuie să fie numeric.", "Notification days must be numeric."))
    } else if (reminderDays !in 0..365) {
        errors.add(localizedText("Notificarea trebuie setată între 0 și 365 zile înainte.", "Notification must be set between 0 and 365 days before expiry."))
    }

    return errors.joinToString(separator = "\n")
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