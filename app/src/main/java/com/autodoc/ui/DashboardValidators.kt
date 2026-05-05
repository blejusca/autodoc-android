package com.autodoc.ui.validators

import java.time.LocalDate
import com.autodoc.ui.localizedText

fun validateCarInput(
    brand: String,
    model: String,
    plate: String,
    yearText: String,
    engine: String,
    ownerName: String,
    ownerPhone: String,
    ownerEmail: String
): String {
    val errors = mutableListOf<String>()

    val cleanBrand = brand.trim()
    val cleanModel = model.trim()
    val cleanPlate = plate.trim().uppercase()
    val cleanYear = yearText.trim()
    val cleanEngine = engine.trim()
    val cleanOwnerName = ownerName.trim()
    val cleanOwnerPhone = ownerPhone.trim()
    val cleanOwnerEmail = ownerEmail.trim()

    val currentYear = LocalDate.now().year
    val maxAllowedYear = currentYear + 1
    val parsedYear = cleanYear.toIntOrNull()

    if (cleanBrand.isBlank()) {
        errors.add(localizedText("Marca este obligatorie.", "Brand is required."))
    } else if (!isValidBrand(cleanBrand)) {
        errors.add(localizedText("Marca nu este validă. Folosește doar litere, spații sau cratimă, maximum 25 caractere.", "Invalid brand. Use only letters, spaces or hyphen, maximum 25 characters."))
    }

    if (cleanModel.isBlank()) {
        errors.add(localizedText("Modelul este obligatoriu.", "Model is required."))
    } else if (!isValidModel(cleanModel)) {
        errors.add(localizedText("Modelul nu este valid. Folosește litere, cifre, spații sau cratimă, maximum 25 caractere.", "Invalid model. Use letters, digits, spaces or hyphen, maximum 25 characters."))
    }

    if (cleanPlate.isBlank()) {
        errors.add(localizedText("Numărul de înmatriculare este obligatoriu.", "Registration number is required."))
    } else if (!isValidPlate(cleanPlate)) {
        errors.add(localizedText("Numărul de înmatriculare nu este valid. Folosește 3-12 caractere: litere, cifre, spații sau cratimă.", "Invalid registration number. Use 3-12 characters: letters, digits, spaces or hyphen."))
    }

    if (cleanYear.isBlank()) {
        errors.add(localizedText("Anul fabricației este obligatoriu.", "Manufacturing year is required."))
    } else if (parsedYear == null) {
        errors.add(localizedText("Anul fabricației trebuie să fie numeric.", "Manufacturing year must be numeric."))
    } else if (parsedYear !in 1950..maxAllowedYear) {
        errors.add(localizedText("Anul fabricației trebuie să fie între 1950 și $maxAllowedYear.", "Manufacturing year must be between 1950 and $maxAllowedYear."))
    }

    if (cleanEngine.isNotBlank() && !isValidEngine(cleanEngine)) {
        errors.add(localizedText("Motorizarea nu este validă. Exemple acceptate: 1.6 TDI, 2.0 benzină, electric, hybrid.", "Invalid engine. Accepted examples: 1.6 TDI, 2.0 petrol, electric, hybrid."))
    }

    if (cleanOwnerName.isNotBlank() && !isReasonableName(cleanOwnerName)) {
        errors.add(localizedText("Numele clientului nu este valid. Folosește minimum 2 caractere și fără cifre.", "Invalid client name. Use at least 2 characters and no digits."))
    }

    // Validare telefon internationala - accepta 8-15 cifre
    if (cleanOwnerPhone.isNotBlank() && !isValidPhone(cleanOwnerPhone)) {
        errors.add(localizedText("Telefonul clientului nu este valid. Trebuie să conțină între 8 și 15 cifre.", "Invalid client phone. It must contain between 8 and 15 digits."))
    }

    // Validare email permisiva internationala - orice TLD de minimum 2 caractere
    if (cleanOwnerEmail.isNotBlank() && !isValidEmail(cleanOwnerEmail)) {
        errors.add(localizedText("Emailul clientului nu este valid sau pare scris greșit.", "Invalid client email or likely misspelled."))
    }

    return errors.joinToString(separator = "\n")
}

private fun isValidBrand(value: String): Boolean {
    return Regex("""^[A-Za-zÀ-ž -]{2,25}$""").matches(value.trim())
}

private fun isValidModel(value: String): Boolean {
    return Regex("""^[A-Za-zÀ-ž0-9 -]{1,25}$""").matches(value.trim())
}

private fun isValidPlate(value: String): Boolean {
    return Regex("""^[A-Z0-9 -]{3,12}$""").matches(value.trim().uppercase())
}

private fun isValidEngine(value: String): Boolean {
    val cleanValue = value.trim().lowercase()

    if (cleanValue in listOf("electric", "hibrid", "hybrid", "benzina", "diesel", "gpl", "gaz")) {
        return true
    }

    val pattern = Regex("""^[0-9](\.[0-9])? ?[A-Za-zÀ-ž0-9 -]{0,15}$""")

    return pattern.matches(cleanValue) && cleanValue.length <= 20
}

/**
 * Validare telefon internationala.
 * Accepta orice numar cu 8-15 cifre dupa eliminarea separatorilor standard.
 * Acopera Romania, Danemarca, Germania, UK, Franta si orice alta tara.
 */
private fun isValidPhone(value: String): Boolean {
    val digits = value.trim()
        .replace(" ", "")
        .replace("-", "")
        .replace("(", "")
        .replace(")", "")
        .replace("+", "")
        .filter { it.isDigit() }

    return digits.length in 8..15
}

/**
 * Validare email internationala permisiva.
 * Accepta orice TLD de minimum 2 caractere - acopera .ro, .dk, .com, .fr, .uk, .de, .nl etc.
 * Blocheaza doar typo-urile evidente (ex: .coom, @yaoo.).
 */
private fun isValidEmail(value: String): Boolean {
    val email = value.trim().lowercase()
    val emailPattern = Regex("""^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")

    if (!emailPattern.matches(email)) {
        return false
    }

    val domain = email.substringAfter("@", missingDelimiterValue = "")
    val tld = domain.substringAfterLast(".", missingDelimiterValue = "")

    // TLD minim 2 caractere
    if (tld.length < 2) return false

    // Nu accepta domenii cu puncte consecutive
    if (domain.contains("..")) return false

    // Blocheaza doar typo-uri comune evidente
    val suspiciousParts = listOf(
        "..",
        ".,",
        ",.",
        ".coom",
        ".comm",
        "@yaoo.",
        "@yahooo.",
        "@yahho.",
        "@gmai.",
        "@gmial.",
        "@hotmial.",
        "@outlok."
    )

    return suspiciousParts.none { email.contains(it) }
}

private fun isReasonableName(value: String): Boolean {
    return Regex("""^[A-Za-zÀ-ž -]{2,40}$""").matches(value.trim())
}
