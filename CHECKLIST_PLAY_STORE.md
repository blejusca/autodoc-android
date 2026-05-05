# AutoDoc — Checklist Google Play Store

## ✅ Modificari aplicate automat in aceasta versiune

### Critice (rezolvate)
- [x] Integrat Google Play Billing Library 7.0.0
- [x] `BillingManager.kt` creat - gestioneaza cumpararea, acknowledgement si verificarea achizitiei
- [x] `AppPlanManager.kt` rescris - statusul Pro vine din Google Play, NU din SharedPreferences
- [x] `AutoDocViewModel.kt` - `setProPlan(true)` inlocuit cu `launchPurchaseFlow()`
- [x] `SettingsScreen.kt` - toggle Pro eliminat, buton de cumparare real
- [x] `AndroidManifest.xml` - adaugate permisiunile INTERNET si com.android.vending.BILLING
- [x] `proguard-rules.pro` - reguli ProGuard pentru Billing Library, Room, WorkManager

### Importante (rezolvate)
- [x] Validarea email internationala - accepta acum .fr, .uk, .de, .nl si orice TLD valid
- [x] Validarea telefon internationala - accepta orice numar cu 8-15 cifre
- [x] `FREE_PLAN_MAX_CARS` duplicat eliminat din DashboardScreen - foloseste `AppPlanManager.FREE_PLAN_MAX_CARS`
- [x] Versiunea aplicatiei afisata din `BuildConfig.VERSION_NAME` (nu mai e hardcodata)
- [x] `buildConfig = true` activat in build.gradle.kts

---

## ⚠️ Ce trebuie sa faci TU manual inainte de publicare

### 1. Creeaza produsul in Google Play Console
```
Play Console → Aplicatia ta → Monetizare → Produse in-app
→ Creeaza produs → Tip: Produs cu plata unica (one-time)
→ ID produs: autodoc_pro_lifetime   ← OBLIGATORIU sa fie exact asa
→ Nume: AutoDoc Pro
→ Descriere: Deblocheaza masini nelimitate
→ Pret: seteaza pretul dorit per tara
→ Salveaza si Activeaza
```

### 2. Configureaza un keystore de release
```bash
keytool -genkey -v -keystore autodoc-release.jks \
  -alias autodoc -keyalg RSA -keysize 2048 -validity 10000
```
Pastreaza keystore-ul si parola in siguranta. Daca le pierzi, nu mai poti publica update-uri.

In `local.properties` (NU in git):
```
KEYSTORE_PATH=../autodoc-release.jks
KEYSTORE_PASSWORD=parola_ta
KEY_ALIAS=autodoc
KEY_PASSWORD=parola_ta
```

### 3. Creeaza Privacy Policy
- Aplicatia colecteaza: nume, telefon, email client (optional)
- Datele raman LOCAL pe dispozitiv (nu se trimit pe server)
- Poti genera una simpla la: https://app.privacypolicies.com
- URL-ul trebuie introdus in Play Console

### 4. Pregateste asseturile pentru Play Store
- Icona aplicatie: 512×512 px PNG (fara transparenta)
- Screenshot-uri: minim 2, recomandat 4-8
  - Dashboard cu masini
  - Ecran documente
  - Notificare expirare
  - Ecran cumparare Pro
- Feature Graphic (optional dar recomandat): 1024×500 px
- Descriere scurta: max 80 caractere
- Descriere lunga: max 4000 caractere

### 5. Seteaza categoria aplicatiei
- Categorie: Productivitate sau Auto si vehicule

### 6. Testeaza cumpararea INAINTE de publicare
- Adauga contul tau Google ca "Tester licenta" in Play Console
- Instaleaza versiunea release (nu debug)
- Cumparatura in modul test nu costa bani reali
- Verifica: butonul Pro deschide Google Play, cumpararea se finalizeaza, statusul Pro se activeaza

---

## 📋 Flux de publicare

1. Genereaza APK/AAB release: `./gradlew bundleRelease`
2. Semneaza cu keystore-ul de release
3. Play Console → Release → Production → Creeaza release noua
4. Incarca .aab
5. Completeaza toate sectiunile (descriere, screenshots, privacy policy)
6. Trimite spre review (2-7 zile pentru prima versiune)

---

## 🔑 Note tehnice importante

**ID produs Billing:** `autodoc_pro_lifetime` (definit in `BillingManager.kt` constanta `PRODUCT_ID_PRO`)

**Testare Billing in debug:** Google Play Billing nu functioneaza pe emulator fara cont Google real si pe aplicatii nesemnate. Testeaza intotdeauna pe device real cu versiune release.

**Daca cumparatura ramane "Pending":** Este normal pentru platile prin transfer bancar sau carduri ce necesita autorizare. Aplicatia gestioneaza corect acest caz.
