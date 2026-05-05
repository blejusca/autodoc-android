package com.autodoc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodoc.R
import com.autodoc.data.AppPlanManager
import com.autodoc.ui.AppColors
import com.autodoc.ui.CarUi
import com.autodoc.ui.DocumentSeverity
import com.autodoc.ui.DocumentUi
import com.autodoc.ui.severity

private val FREE_PLAN_MAX_CARS = AppPlanManager.FREE_PLAN_MAX_CARS

private enum class DashboardFilter {
    ALL,
    EXPIRED,
    SOON,
    OK
}

private enum class DashboardSort {
    URGENTE,
    MARCA,
    DOCUMENTE
}

private data class DashboardStats(
    val expiredCount: Int,
    val soonCount: Int,
    val okCount: Int,
    val totalDocuments: Int
)

@Composable
fun DashboardScreen(
    cars: List<CarUi>,
    isProPlan: Boolean,
    onActivatePro: () -> Unit,
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
    ) -> Unit,
    onUpdateCar: (
        carId: Int,
        brand: String,
        model: String,
        plate: String,
        year: Int,
        engine: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        ownerNotes: String
    ) -> Unit,
    onAddDocument: (
        carId: Int,
        type: String,
        expiryDateMillis: Long,
        reminderDaysBefore: Int
    ) -> Unit,
    onDeleteDocument: (documentId: Int) -> Unit,
    onUpdateDocumentExpiry: (documentId: Int, expiryDateMillis: Long) -> Unit,
    onDeleteCar: (carId: Int) -> Unit,
    onExportCarPdf: (CarUi) -> Unit
) {
    val showAddCar = remember { mutableStateOf(false) }
    val showProDialog = remember { mutableStateOf(false) }
    val searchInput = remember { mutableStateOf("") }
    val activeSearch = remember { mutableStateOf("") }
    val activeFilter = remember { mutableStateOf(DashboardFilter.ALL) }
    val activeSort = remember { mutableStateOf(DashboardSort.URGENTE) }
    val expandedCars = remember { mutableStateMapOf<Int, Boolean>() }
    val focusManager = LocalFocusManager.current

    val stats = remember(cars) {
        calculateDashboardStats(cars)
    }

    val filteredCars = remember(
        cars,
        activeSearch.value,
        activeFilter.value,
        activeSort.value
    ) {
        filterAndSortCars(
            cars = cars,
            searchQuery = activeSearch.value,
            filter = activeFilter.value,
            sort = activeSort.value
        )
    }

    if (showProDialog.value) {
        ConfirmDashboardProDialog(
            onConfirm = {
                showProDialog.value = false
                onActivatePro()
            },
            onDismiss = {
                showProDialog.value = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.DeepBg)
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Header(
                carsCount = cars.size,
                documentsCount = stats.totalDocuments
            )
        }

        if (!isProPlan) {
            item {
                FreePlanBanner(
                    carsCount = cars.size,
                    maxCars = FREE_PLAN_MAX_CARS,
                    onUpgradeClick = {
                        showProDialog.value = true
                    }
                )
            }
        }

        item {
            PrimaryActionButton(
                expanded = showAddCar.value,
                onClick = { showAddCar.value = !showAddCar.value }
            )
        }

        if (showAddCar.value) {
            item {
                AddCarForm(
                    onAddCar = { brand, model, plate, year, engine, ownerName, ownerPhone, ownerEmail, ownerNotes ->
                        onAddCar(
                            brand,
                            model,
                            plate,
                            year,
                            engine,
                            ownerName,
                            ownerPhone,
                            ownerEmail,
                            ownerNotes
                        )
                        searchInput.value = ""
                        activeSearch.value = ""
                        activeFilter.value = DashboardFilter.ALL
                        activeSort.value = DashboardSort.URGENTE
                        showAddCar.value = false
                        focusManager.clearFocus()
                    }
                )
            }
        }

        item {
            SearchBar(
                value = searchInput.value,
                onChange = { searchInput.value = it },
                onSearch = {
                    activeSearch.value = searchInput.value
                    focusManager.clearFocus()
                },
                onReset = {
                    searchInput.value = ""
                    activeSearch.value = ""
                    activeFilter.value = DashboardFilter.ALL
                    activeSort.value = DashboardSort.URGENTE
                    focusManager.clearFocus()
                }
            )
        }

        item {
            SummaryCards(
                expiredCount = stats.expiredCount,
                soonCount = stats.soonCount,
                okCount = stats.okCount,
                totalDocuments = stats.totalDocuments,
                activeFilter = activeFilter.value,
                onFilterChange = { selectedFilter -> activeFilter.value = selectedFilter }
            )
        }

        item {
            SortButtons(
                activeSort = activeSort.value,
                resultCount = filteredCars.size,
                onSortChange = { selectedSort -> activeSort.value = selectedSort }
            )
        }

        if (filteredCars.isEmpty()) {
            item {
                EmptyCarsCard()
            }
        } else {
            items(filteredCars, key = { it.id }) { car ->
                PremiumCarCard(
                    car = car,
                    expanded = expandedCars[car.id] == true,
                    onToggle = { expandedCars[car.id] = !(expandedCars[car.id] ?: false) },
                    onUpdateCar = onUpdateCar,
                    onAddDocument = onAddDocument,
                    onDeleteDocument = onDeleteDocument,
                    onUpdateDocumentExpiry = onUpdateDocumentExpiry,
                    onDeleteCar = onDeleteCar,
                    onExportCarPdf = onExportCarPdf
                )
            }
        }
    }
}

@Composable
private fun FreePlanBanner(
    carsCount: Int,
    maxCars: Int,
    onUpgradeClick: () -> Unit
) {
    val safeCarsCount = carsCount.coerceAtLeast(0)
    val usedText = "${safeCarsCount.coerceAtMost(maxCars)}/$maxCars ${stringResource(R.string.loc_vehicles_used)}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Gold),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Plan Free",
                color = AppColors.Gold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = usedText,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.loc_activate_pro_for_unlimited_vehicles_and_unrestri),
                color = AppColors.SoftText,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )

            Button(
                onClick = onUpgradeClick,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.loc_activate_pro),
                    color = AppColors.Navy,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun ConfirmDashboardProDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.loc_activate_pro_2),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                stringResource(R.string.loc_unlock_unlimited_vehicles_with_a_one_time_paymen)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold)
            ) {
                Text(
                    text = stringResource(R.string.loc_yes_activate),
                    color = AppColors.Navy,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger)
            ) {
                Text(
                    text = stringResource(R.string.loc_cancel),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

private fun calculateDashboardStats(cars: List<CarUi>): DashboardStats {
    val allDocuments = cars.flatMap { it.documents }

    return DashboardStats(
        expiredCount = allDocuments.count { it.severity() == DocumentSeverity.EXPIRED },
        soonCount = allDocuments.count { it.severity() == DocumentSeverity.SOON },
        okCount = allDocuments.count { it.severity() == DocumentSeverity.OK },
        totalDocuments = allDocuments.size
    )
}

private fun filterAndSortCars(
    cars: List<CarUi>,
    searchQuery: String,
    filter: DashboardFilter,
    sort: DashboardSort
): List<CarUi> {
    val query = searchQuery.trim().lowercase()

    return cars
        .filter { car ->
            car.matchesSearch(query) && car.matchesFilter(filter)
        }
        .sortedWith(carComparator(sort))
}

private fun CarUi.matchesSearch(query: String): Boolean {
    if (query.isBlank()) {
        return true
    }

    return brand.lowercase().contains(query) ||
            model.lowercase().contains(query) ||
            plate.lowercase().contains(query) ||
            ownerName.lowercase().contains(query) ||
            ownerPhone.lowercase().contains(query) ||
            ownerEmail.lowercase().contains(query)
}

private fun CarUi.matchesFilter(filter: DashboardFilter): Boolean {
    return when (filter) {
        DashboardFilter.ALL -> true
        DashboardFilter.EXPIRED -> documents.any { it.severity() == DocumentSeverity.EXPIRED }
        DashboardFilter.SOON -> documents.any { it.severity() == DocumentSeverity.SOON }
        DashboardFilter.OK -> documents.isNotEmpty() &&
                documents.all { it.severity() == DocumentSeverity.OK }
    }
}

private fun carComparator(sort: DashboardSort): Comparator<CarUi> {
    return when (sort) {
        DashboardSort.MARCA ->
            compareBy<CarUi> { it.brand.lowercase() }
                .thenBy { it.model.lowercase() }

        DashboardSort.DOCUMENTE ->
            compareByDescending<CarUi> { it.documents.size }
                .thenBy { it.brand.lowercase() }
                .thenBy { it.model.lowercase() }

        DashboardSort.URGENTE ->
            compareBy<CarUi> { car -> car.documents.urgencyDaysLeft() }
                .thenBy { it.brand.lowercase() }
                .thenBy { it.model.lowercase() }
    }
}

private fun List<DocumentUi>.urgencyDaysLeft(): Int {
    return minOfOrNull { it.daysLeft } ?: Int.MAX_VALUE
}

@Composable
private fun Header(
    carsCount: Int,
    documentsCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Navy),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.size(50.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                border = BorderStroke(1.dp, AppColors.Gold),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "◆",
                        color = AppColors.Gold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    maxLines = 1
                )

                Text(
                    text = "$carsCount ${stringResource(R.string.loc_vehicles_2)} • $documentsCount ${stringResource(R.string.loc_documents_2)}",
                    color = AppColors.Gold,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Text(
                    text = stringResource(R.string.loc_vehicle_documents_clients_and_expiry_tracking),
                    color = AppColors.SoftText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PrimaryActionButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (expanded) AppColors.CardBg else AppColors.Gold
        ),
        border = if (expanded) BorderStroke(1.dp, AppColors.Border) else null,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(
            text = if (expanded) stringResource(R.string.loc_close_form) else stringResource(R.string.loc_add_vehicle),
            color = if (expanded) AppColors.Gold else AppColors.Navy,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun SearchBar(
    value: String,
    onChange: (String) -> Unit,
    onSearch: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(AppColors.FieldBg)
                    .border(BorderStroke(1.dp, Color.Transparent), RoundedCornerShape(18.dp)),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = AppColors.Gold,
                            modifier = Modifier.size(21.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isBlank()) {
                                Text(
                                    text = stringResource(R.string.loc_search_by_vehicle_plate_or_client),
                                    color = AppColors.SoftText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSearch,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold),
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Text(
                        text = stringResource(R.string.loc_search),
                        color = AppColors.Navy,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Navy),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, AppColors.Border),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Text(
                        text = stringResource(R.string.loc_reset),
                        color = AppColors.Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCards(
    expiredCount: Int,
    soonCount: Int,
    okCount: Int,
    totalDocuments: Int,
    activeFilter: DashboardFilter,
    onFilterChange: (DashboardFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.loc_document_status),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                title = stringResource(R.string.loc_expired),
                value = expiredCount.toString(),
                color = AppColors.Danger,
                selected = activeFilter == DashboardFilter.EXPIRED,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onFilterChange(DashboardFilter.EXPIRED) }
            )
            SummaryCard(
                title = stringResource(R.string.loc_soon),
                value = soonCount.toString(),
                color = AppColors.Warning,
                selected = activeFilter == DashboardFilter.SOON,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onFilterChange(DashboardFilter.SOON) }
            )
            SummaryCard(
                title = "OK",
                value = okCount.toString(),
                color = AppColors.Ok,
                selected = activeFilter == DashboardFilter.OK,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onFilterChange(DashboardFilter.OK) }
            )
            SummaryCard(
                title = "Total",
                value = totalDocuments.toString(),
                color = AppColors.Gold,
                selected = activeFilter == DashboardFilter.ALL,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onFilterChange(DashboardFilter.ALL) }
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(68.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AppColors.Navy else AppColors.CardBg
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) AppColors.Gold else AppColors.Border
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 19.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )

            Text(
                text = title,
                color = AppColors.SoftText,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SortButtons(
    activeSort: DashboardSort,
    resultCount: Int,
    onSortChange: (DashboardSort) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.loc_vehicles),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "$resultCount ${stringResource(R.string.loc_results)}",
                color = AppColors.SoftText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortButton(
                text = stringResource(R.string.loc_priority),
                selected = activeSort == DashboardSort.URGENTE,
                modifier = Modifier.weight(1f)
            ) {
                onSortChange(DashboardSort.URGENTE)
            }

            SortButton(
                text = stringResource(R.string.loc_brand),
                selected = activeSort == DashboardSort.MARCA,
                modifier = Modifier.weight(1f)
            ) {
                onSortChange(DashboardSort.MARCA)
            }

            SortButton(
                text = stringResource(R.string.loc_documents),
                selected = activeSort == DashboardSort.DOCUMENTE,
                modifier = Modifier.weight(1f)
            ) {
                onSortChange(DashboardSort.DOCUMENTE)
            }
        }
    }
}

@Composable
private fun SortButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AppColors.Gold else Color.Transparent
        ),
        border = BorderStroke(1.dp, AppColors.Gold),
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
                color = if (selected) AppColors.Navy else AppColors.Gold,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun EmptyCarsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "▣",
                color = AppColors.SoftText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.loc_no_vehicles_match_the_selected_filter),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                )

                Text(
                    text = stringResource(R.string.loc_reset_the_search_or_add_a_new_vehicle),
                    color = AppColors.SoftText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}