package com.autodoc.ui.screens

import com.autodoc.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodoc.ui.AppColors
import com.autodoc.ui.validators.validateCarInput
import androidx.compose.ui.res.stringResource

@Composable
fun AddCarForm(
    onAddCar: (
        brand: String,
        model: String,
        plate: String,
        year: Int,
        engine: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        ownerNotes: String
    ) -> Unit
) {
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

    val brand = remember { mutableStateOf("") }
    val model = remember { mutableStateOf("") }
    val plate = remember { mutableStateOf("") }
    val year = remember { mutableStateOf("") }
    val engine = remember { mutableStateOf("") }
    val ownerName = remember { mutableStateOf("") }
    val ownerPhone = remember { mutableStateOf("") }
    val ownerEmail = remember { mutableStateOf("") }
    val ownerNotes = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf("") }

    PremiumLightCard {
        Text(
            text = stringResource(R.string.loc_new_vehicle),
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontSize = 23.sp
        )

        if (errorMessage.value.isNotBlank()) {
            Text(
                text = errorMessage.value,
                color = AppColors.Danger,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        FormSectionTitle(stringResource(R.string.loc_vehicle_details))
        LightField(brand.value, { brand.value = it }, stringResource(R.string.loc_brand))
        LightField(model.value, { model.value = it }, stringResource(R.string.loc_model))
        LightField(plate.value, { plate.value = it.uppercase() }, stringResource(R.string.loc_registration_number))
        LightField(year.value, { year.value = it.filter { c -> c.isDigit() } }, stringResource(R.string.loc_manufacturing_year))
        LightField(engine.value, { engine.value = it }, stringResource(R.string.loc_engine))

        Spacer(modifier = Modifier.height(2.dp))

        FormSectionTitle(stringResource(R.string.loc_client_owner_details))
        LightField(ownerName.value, { ownerName.value = it }, stringResource(R.string.loc_client_owner_name))
        LightField(ownerPhone.value, { ownerPhone.value = it }, stringResource(R.string.loc_client_phone))
        LightField(ownerEmail.value, { ownerEmail.value = it }, stringResource(R.string.loc_client_email))
        LightField(ownerNotes.value, { ownerNotes.value = it }, stringResource(R.string.loc_client_notes))

        Button(
            onClick = {
                val validationError = validateCarInput(
                    brand = brand.value,
                    model = model.value,
                    plate = plate.value,
                    yearText = year.value,
                    engine = engine.value,
                    ownerName = ownerName.value,
                    ownerPhone = ownerPhone.value,
                    ownerEmail = ownerEmail.value
                )

                errorMessage.value = validationError

                if (validationError.isBlank()) {
                    onAddCar(
                        brand.value.trim(),
                        model.value.trim(),
                        plate.value.trim().uppercase(),
                        year.value.trim().toIntOrNull() ?: 0,
                        engine.value.trim(),
                        ownerName.value.trim(),
                        ownerPhone.value.trim(),
                        ownerEmail.value.trim(),
                        ownerNotes.value.trim()
                    )

                    brand.value = ""
                    model.value = ""
                    plate.value = ""
                    year.value = ""
                    engine.value = ""
                    ownerName.value = ""
                    ownerPhone.value = ""
                    ownerEmail.value = ""
                    ownerNotes.value = ""
                    errorMessage.value = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.loc_save_vehicle),
                color = AppColors.Navy,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun FormSectionTitle(text: String) {
    Text(
        text = text,
        color = AppColors.Gold,
        fontWeight = FontWeight.Black,
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun LightField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        label = {
            Text(
                text = label,
                maxLines = 1,
                softWrap = false,
                fontSize = 13.sp
            )
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = Color.White,
            fontSize = 14.sp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = AppColors.Gold,
            unfocusedLabelColor = AppColors.SoftText,
            focusedBorderColor = AppColors.Border,
            unfocusedBorderColor = AppColors.Border,
            cursorColor = AppColors.Gold,
            focusedContainerColor = AppColors.FieldBg,
            unfocusedContainerColor = AppColors.FieldBg
        )
    )
}

@Composable
private fun PremiumLightCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}