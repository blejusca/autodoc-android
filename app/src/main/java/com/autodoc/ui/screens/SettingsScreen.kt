package com.autodoc.ui.screens

import com.autodoc.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodoc.BuildConfig
import com.autodoc.data.AppPlanManager
import com.autodoc.ui.AppColors
import androidx.compose.ui.res.stringResource
@Composable
fun SettingsScreen(
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    isProPlan: Boolean,
    onBuyPro: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.DeepBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsHeaderCard()

        PlanSettingsCard(
            isProPlan = isProPlan,
            onBuyPro = onBuyPro
        )

        BackupSettingsCard(
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup
        )

        AppInfoCard()
    }
}

@Composable
private fun SettingsHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Navy),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.loc_settings),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )

            Text(
                text = stringResource(R.string.loc_app_plan_backup_and_information),
                color = AppColors.Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PlanSettingsCard(
    isProPlan: Boolean,
    onBuyPro: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, if (isProPlan) AppColors.Gold else AppColors.Border),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.loc_app_plan),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )

            if (isProPlan) {
                Text(
                    text = stringResource(R.string.loc_pro_plan_active),
                    color = AppColors.Gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = stringResource(R.string.loc_you_have_access_to_unlimited_vehicles),
                    color = AppColors.MutedText
                )
            } else {
                Text(
                    text = stringResource(R.string.loc_free_plan_active),
                    color = AppColors.Gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "${stringResource(R.string.loc_limit)}: ${AppPlanManager.FREE_PLAN_MAX_CARS} ${stringResource(R.string.loc_vehicles_2)}.",
                    color = AppColors.MutedText
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Beneficii Pro
                Text(
                    text = stringResource(R.string.loc_what_you_get_with_pro),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(text = stringResource(R.string.loc_unlimited_vehicles), color = AppColors.MutedText, fontSize = 14.sp)
                Text(text = stringResource(R.string.loc_all_documents_for_every_vehicle), color = AppColors.MutedText, fontSize = 14.sp)
                Text(text = stringResource(R.string.loc_advanced_expiry_notifications), color = AppColors.MutedText, fontSize = 14.sp)
                Text(text = stringResource(R.string.loc_one_time_payment_no_subscription), color = AppColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onBuyPro,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = stringResource(R.string.loc_buy_pro_one_time_payment),
                        color = AppColors.Navy,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = stringResource(R.string.loc_the_price_is_shown_in_google_play_after_pressing),
                    color = AppColors.SoftText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun BackupSettingsCard(
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.loc_backup_data), color = Color.White, fontWeight = FontWeight.Bold)

            Button(onClick = onExportBackup) {
                Text(stringResource(R.string.loc_export_backup))
            }

            Button(onClick = onImportBackup) {
                Text(stringResource(R.string.loc_import_backup))
            }
        }
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("AutoDoc", color = Color.White, fontWeight = FontWeight.Bold)
            Text("${stringResource(R.string.loc_version)} ${BuildConfig.VERSION_NAME}", color = AppColors.SoftText)
        }
    }
}
