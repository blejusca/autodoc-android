package com.autodoc.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.autodoc.data.entity.CarEntity
import com.autodoc.data.entity.DocumentEntity
import com.autodoc.notification.AutoDocNotificationScheduler
import com.autodoc.ui.normalizeDocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupManager {

    private const val BACKUP_VERSION = 1

    private const val AUTO_BACKUP_FILE_NAME = "autodoc_backup.json"
    private const val PRE_IMPORT_BACKUP_FILE_NAME = "autodoc_pre_import_backup.json"
    private const val MANUAL_BACKUP_FILE_NAME = "autodoc_backup.json"

    suspend fun saveBackupToFile(context: Context): File {
        return withContext(Dispatchers.IO) {
            val db = DatabaseProvider.getDatabase(context)

            val cars = db.carDao().getAllCarsSync()
            val documents = db.documentDao().getAllDocumentsSync()

            val json = buildJson(cars, documents)

            val file = File(
                context.getExternalFilesDir(null),
                AUTO_BACKUP_FILE_NAME
            )

            file.writeText(json)
            file
        }
    }

    suspend fun savePreImportSafetyBackup(context: Context): File {
        return withContext(Dispatchers.IO) {
            val db = DatabaseProvider.getDatabase(context)

            val cars = db.carDao().getAllCarsSync()
            val documents = db.documentDao().getAllDocumentsSync()

            val json = buildJson(cars, documents)

            val file = File(
                context.getExternalFilesDir(null),
                PRE_IMPORT_BACKUP_FILE_NAME
            )

            file.writeText(json)
            file
        }
    }

    suspend fun saveBackupToDownloads(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val db = DatabaseProvider.getDatabase(context)

                val cars = db.carDao().getAllCarsSync()
                val documents = db.documentDao().getAllDocumentsSync()

                val json = buildJson(cars, documents)
                val fileName = MANUAL_BACKUP_FILE_NAME

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver

                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/json")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val uri = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    ) ?: return@withContext false

                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    } ?: return@withContext false

                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)

                    true
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )

                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }

                    val file = File(downloadsDir, fileName)
                    file.writeText(json)

                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun importBackupFromUri(
        context: Context,
        uri: Uri,
        scheduler: AutoDocNotificationScheduler
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val db = DatabaseProvider.getDatabase(context)

                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext false

                val jsonText = inputStream.bufferedReader().use { it.readText() }

                val root = JSONObject(jsonText)

                if (!root.has("cars") || !root.has("documents")) {
                    return@withContext false
                }

                val backupVersion = root.optInt("version", BACKUP_VERSION)
                if (backupVersion > BACKUP_VERSION) {
                    return@withContext false
                }

                val carsArray = root.getJSONArray("cars")
                val documentsArray = root.getJSONArray("documents")

                val parsedCars = parseCarsArray(carsArray)
                val parsedDocuments = parseDocumentsArray(documentsArray)

                if (parsedCars.isEmpty()) {
                    return@withContext false
                }

                savePreImportSafetyBackup(context)

                db.documentDao().deleteAll()
                db.carDao().deleteAll()

                val carIdMap = mutableMapOf<Int, Long>()
                val importedCarsByNewId = mutableMapOf<Int, CarEntity>()

                parsedCars.forEach { parsedCar ->
                    val newId = db.carDao().insert(parsedCar.car)

                    parsedCar.oldIds.forEach { oldId ->
                        carIdMap[oldId] = newId
                    }

                    importedCarsByNewId[newId.toInt()] = parsedCar.car.copy(id = newId.toInt())
                }

                parsedDocuments.forEach { parsedDocument ->
                    val newCarId = carIdMap[parsedDocument.oldCarId]?.toInt()
                        ?: return@forEach

                    val documentToInsert = parsedDocument.document.copy(
                        id = 0,
                        carId = newCarId
                    )

                    val newDocumentId = db.documentDao().insert(documentToInsert).toInt()

                    val importedCar = importedCarsByNewId[newCarId]
                    val carName = if (importedCar != null) {
                        "${importedCar.brand} ${importedCar.model} - ${importedCar.plate}"
                    } else {
                        "Masina necunoscuta"
                    }

                    scheduler.schedule(
                        documentId = newDocumentId,
                        type = documentToInsert.type,
                        carName = carName,
                        expiry = documentToInsert.expiryDate,
                        daysBefore = documentToInsert.reminderDaysBefore
                    )
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun parseCarsArray(carsArray: JSONArray): List<ParsedCar> {
        val carsByPlate = linkedMapOf<String, ParsedCar>()

        for (i in 0 until carsArray.length()) {
            val obj = carsArray.getJSONObject(i)

            val oldId = obj.optInt("id", 0)
            val brand = obj.optString("brand", "").trim()
            val model = obj.optString("model", "").trim()
            val plate = obj.optString("plate", "").trim().uppercase()

            if (oldId <= 0 || brand.isBlank() || model.isBlank() || plate.isBlank()) {
                continue
            }

            val existingParsedCar = carsByPlate[plate]

            if (existingParsedCar != null) {
                existingParsedCar.oldIds.add(oldId)
                continue
            }

            val car = CarEntity(
                id = 0,
                brand = brand,
                model = model,
                plate = plate,
                year = obj.optInt("year", 0),
                engine = obj.optString("engine", "").trim().ifBlank { "Nespecificat" },
                ownerName = obj.optString("ownerName", "").trim(),
                ownerPhone = obj.optString("ownerPhone", "").trim(),
                ownerEmail = obj.optString("ownerEmail", "").trim(),
                ownerNotes = obj.optString("ownerNotes", "").trim()
            )

            carsByPlate[plate] = ParsedCar(
                oldIds = mutableListOf(oldId),
                car = car
            )
        }

        return carsByPlate.values.toList()
    }

    private fun parseDocumentsArray(documentsArray: JSONArray): List<ParsedDocument> {
        val result = mutableListOf<ParsedDocument>()
        val documentKeys = mutableSetOf<String>()

        for (i in 0 until documentsArray.length()) {
            val obj = documentsArray.getJSONObject(i)

            val oldCarId = obj.optInt("carId", 0)
            val cleanType = normalizeDocumentType(obj.optString("type", ""))
            val expiryDate = obj.optLong("expiryDate", 0L)
            val reminderDaysBefore = obj
                .optInt("reminderDaysBefore", 7)
                .coerceAtLeast(0)

            if (oldCarId <= 0 || cleanType.isBlank() || expiryDate <= 0L) {
                continue
            }

            val uniqueDocumentKey = "$oldCarId|$cleanType"

            if (documentKeys.contains(uniqueDocumentKey)) {
                continue
            }

            documentKeys.add(uniqueDocumentKey)

            val document = DocumentEntity(
                id = 0,
                carId = 0,
                type = cleanType,
                expiryDate = expiryDate,
                reminderDaysBefore = reminderDaysBefore,
                notifiedExpired = obj.optBoolean("notifiedExpired", false),
                notifiedToday = obj.optBoolean("notifiedToday", false),
                notifiedTomorrow = obj.optBoolean("notifiedTomorrow", false),
                notifiedReminder = obj.optBoolean("notifiedReminder", false),
                manuallyNotified = obj.optBoolean("manuallyNotified", false)
            )

            result.add(
                ParsedDocument(
                    oldCarId = oldCarId,
                    document = document
                )
            )
        }

        return result
    }

    private fun buildJson(
        cars: List<CarEntity>,
        documents: List<DocumentEntity>
    ): String {
        val root = JSONObject()

        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val carsArray = JSONArray()
        cars.forEach { car ->
            val obj = JSONObject()
            obj.put("id", car.id)
            obj.put("brand", car.brand)
            obj.put("model", car.model)
            obj.put("plate", car.plate)
            obj.put("year", car.year)
            obj.put("engine", car.engine)
            obj.put("ownerName", car.ownerName)
            obj.put("ownerPhone", car.ownerPhone)
            obj.put("ownerEmail", car.ownerEmail)
            obj.put("ownerNotes", car.ownerNotes)
            carsArray.put(obj)
        }

        val documentsArray = JSONArray()
        documents.forEach { document ->
            val obj = JSONObject()
            obj.put("id", document.id)
            obj.put("carId", document.carId)
            obj.put("type", document.type)
            obj.put("expiryDate", document.expiryDate)
            obj.put("reminderDaysBefore", document.reminderDaysBefore)
            obj.put("notifiedExpired", document.notifiedExpired)
            obj.put("notifiedToday", document.notifiedToday)
            obj.put("notifiedTomorrow", document.notifiedTomorrow)
            obj.put("notifiedReminder", document.notifiedReminder)
            obj.put("manuallyNotified", document.manuallyNotified)
            documentsArray.put(obj)
        }

        root.put("cars", carsArray)
        root.put("documents", documentsArray)

        return root.toString(2)
    }

    private data class ParsedCar(
        val oldIds: MutableList<Int>,
        val car: CarEntity
    )

    private data class ParsedDocument(
        val oldCarId: Int,
        val document: DocumentEntity
    )
}