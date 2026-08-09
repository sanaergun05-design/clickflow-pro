# ClickFlow Pro

ClickFlow Pro, Android Accessibility Service kullanarak seçilen ekran koordinatlarına otomatik dokunma yapan modern bir Kotlin uygulamasıdır. Proje, Android Studio'ya bağlı değildir; AndroidIDE veya Termux + Android SDK ile telefondan APK üretilebilecek tam bir Android Gradle projesidir.

> **Termux ile derleme:** Aşağıdaki "Alternatif yöntem: Termux" bölümüne bakın.
> **Gizlilik politikası taslağı:** `docs/privacy-policy.md` — Play Console'a yüklemeden önce
> tarih/e-posta alanlarını doldurup bir web sayfasında (GitHub Pages, Notion vb.) yayınlayıp
> linkini Play Console > App content > Privacy policy alanına ekleyin.
> **Play Store mağaza ikonu (512×512):** `play-store-icon-512.png` — bu dosya APK içine
> girmez, yalnızca Play Console listeleme sayfasına ayrıca yüklenir.
> **Uygulama kimliği:** `applicationId = com.clickflowpro.app` — paket adı `com.example`
> öneki olmadan güncellendi, artık prod'a uygun.

## Özellikler

- 1–50 CPS arasında tıklama hızı ayarı
- Milisaniye bazında tıklama aralığı ayarı
- Birden fazla tıklama noktası
- X/Y koordinatlarını elle düzenleme
- Tıklama noktalarının görsel önizlemesi
- Profil oluşturma ve profil ayarlarını cihazda saklama
- Başlat/durdur kontrolü
- Erişilebilirlik servisi üzerinden gerçek `dispatchGesture` dokunuşları
- Uygulama dışında görünen küçük floating kontrol paneli
- Panel üzerinden başlatma, durdurma ve kapatma
- Toplam tıklama sayacı
- Açık/koyu tema
- Servis kesildiğinde ve uygulama kapandığında güvenli durdurma

## Derleme gereksinimleri

Bu proje Android Studio'ya bağlı değildir. Android Studio yalnızca isteğe bağlı grafik arayüzdür.

Gerekli araçlar:

- Java/JDK 11 veya daha yeni bir JDK
- Android SDK
- Android SDK Platform 35
- Android SDK Build-Tools 35.x
- Android SDK Platform-Tools
- İnternet bağlantısı: ilk derlemede Gradle ve Maven bağımlılıklarını indirmek için

Proje sürümleri:

```text
compileSdk = 35
targetSdk = 35
minSdk = 26
Java/Kotlin JVM = 11
Android Gradle Plugin = 7.4.2
Kotlin = 1.8.22
Compose Compiler = 1.4.8
Gradle Wrapper = 7.5.1
```

Gradle Wrapper dosyaları projeye dahil edilmiştir:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

İlk derlemede Wrapper, Gradle 7.5.1'i internetten otomatik indirir. Bu nedenle telefonda ayrıca Gradle kurmak zorunda değilsiniz.

Android SDK yolu cihazdan cihaza değiştiği için `local.properties` ZIP'e dahil edilmez. AndroidIDE bu dosyayı çoğunlukla otomatik oluşturur. Komut satırında gerekirse SDK yolunu `ANDROID_HOME` veya `ANDROID_SDK_ROOT` ile verin ya da proje klasöründe `local.properties` oluşturun:

```properties
sdk.dir=/telefonunuzdaki/android-sdk-yolu
```

`local.properties` cihaz özeldir ve başka telefona kopyalanmamalıdır.

### AndroidIDE Java 8 uyarısı

AndroidIDE içinde görünen Java sürümü, uygulamanın `sourceCompatibility` ayarından farklıdır. Bu proje kodu Java 11 bytecode üretir ve Gradle'ın kendisi de JDK 11 veya daha yeni bir JDK ile çalışmalıdır. AndroidIDE Java 8 seçiliyse proje senkronizasyonu başlamadan anlaşılır bir hata verir.

AndroidIDE'de şu adımları izleyin:

1. AndroidIDE ayarlarını açın.
2. **Build & Run**, **Gradle** veya **JDK/Java** bölümünü bulun. AndroidIDE sürümüne göre menü adı değişebilir.
3. Gradle JDK olarak **JDK 11** veya daha yeni bir sürüm seçin.
4. JDK 11 listede yoksa AndroidIDE'nin önerdiği/bundled JDK 11 paketini kurun veya cihazda JDK 11 kurulu olduğundan emin olun.
5. Projeyi kapatıp yeniden açın veya **Sync/Reload Gradle Project** çalıştırın.
6. AndroidIDE terminalinde doğrulayın:

   ```bash
   java -version
   ./gradlew --version
   ```

   İlk komut Java `11` veya daha yeni bir sürüm göstermelidir. Gradle çıktısındaki `JVM` satırı da `11` veya daha yeni olmalıdır.

Projeye sabit `org.gradle.java.home` yolu eklenmemiştir; AndroidIDE'deki JDK yolu her telefonda farklıdır. Bu nedenle Java 8 kullanan AndroidIDE'yi proje dosyası tek başına Java 11'e çeviremez. Java 8 seçili kalırsa `settings.gradle.kts` açıkça JDK 11+ seçilmesini isteyen hata gösterecektir.

## APK oluşturmak için telefonda şu adımları izle

### Önerilen yöntem: AndroidIDE

1. AndroidIDE uygulamasını güvenilir resmi kaynağından telefonunuza kurun.
2. Bu projeyi ZIP olarak telefonunuza indirin.
3. ZIP'i telefonun dahili depolamasına çıkarın.
4. Çıkarılan klasörün içinde doğrudan `settings.gradle.kts`, `gradlew` ve `app` bulunduğunu kontrol edin.
5. AndroidIDE içinde **Open project** veya **Import project** seçeneğine dokunun.
6. ZIP'ten çıkardığınız `android-auto-clicker` klasörünü seçin.
7. AndroidIDE ilk açılışta Gradle 7.5.1 ve Maven bağımlılıklarını internetten indirsin.
8. SDK kurulumu istenirse Android SDK Platform 35 ve Build-Tools 35.x'i kurun.
9. Proje senkronizasyonunun tamamlanmasını bekleyin.
10. AndroidIDE terminalini açıp proje klasöründe şu komutu çalıştırın:

    ```bash
    chmod +x gradlew
    ./gradlew assembleDebug
    ```

11. APK şu konumda oluşur:

    ```text
    app/build/outputs/apk/debug/app-debug.apk
    ```

12. AndroidIDE içinden APK'yı **Install/Run** ile yükleyin veya dosya yöneticisiyle APK'ya dokunarak kurun.
13. Uygulamayı açın.
14. `Accessibility access needed` kartındaki **Enable** düğmesine dokunun.
15. Android ayarlarında **ClickFlow Pro** erişilebilirlik servisini etkinleştirin.
16. Uygulamaya dönüp tıklama ayarlarını yapın ve başlatın.

AndroidIDE'de `Permission denied` veya benzeri bir Wrapper izni hatası çıkarsa:

```bash
chmod +x gradlew
./gradlew assembleDebug
```

### Alternatif yöntem: Termux

Termux ile APK build etmek mümkündür; ancak Android SDK kurulumu AndroidIDE'ye göre daha zahmetlidir.

1. Termux'u güvenilir resmi kaynaktan kurun.
2. Termux içinde JDK, Git, Wget ve Unzip kurun:

    ```bash
    pkg update
    pkg install openjdk-17 git wget unzip
    ```

3. Android SDK Command-line Tools'u Android geliştirici sitesinden telefon mimarinize uygun olarak indirin.
4. Command-line Tools'u bir Android SDK klasörüne çıkarın.
5. `ANDROID_SDK_ROOT` ve `PATH` değişkenlerini SDK klasörünüze göre ayarlayın.
6. SDK paketlerini kurun:

    ```bash
    sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
    ```

7. Proje ZIP'ini Termux'un erişebileceği konuma çıkarın ve proje klasörüne girin:

    ```bash
    cd android-auto-clicker
    chmod +x gradlew
    ./gradlew assembleDebug
    ```

8. APK `app/build/outputs/apk/debug/app-debug.apk` konumunda olur.

Termux'ta SDK paketi, JDK sürümü veya cihaz mimarisiyle ilgili bir hata alırsanız AndroidIDE yöntemini kullanın. Termux, Android SDK kurulmadan tek başına APK üretemez.

## Android Studio ile açma

1. Android Studio Giraffe veya daha yeni bir sürüm kurun.
2. Android Studio içinden **Open** seçerek `android-auto-clicker` klasörünü açın.
3. Gradle senkronizasyonunun tamamlanmasını bekleyin.
4. Android SDK Platform 35, Build-Tools 35.x ve JDK 11 veya daha yeni bir sürümün kurulu olduğundan emin olun.
5. Bir Android cihaz veya API 26+ emülatör bağlayın.
6. `app` çalıştırma konfigürasyonu ile uygulamayı başlatın.

Projenin gerçek Android Gradle projesi olduğunu doğrulayan temel dosyalar `settings.gradle.kts`, kök `build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml` ve `gradle/wrapper/` altında bulunur.

## APK oluşturma

Android Studio kullanıyorsanız:

**Build > Generate App Bundles or APKs > Generate APKs**

Terminalde Android SDK kurulu bir ortamda:

```bash
./gradlew assembleDebug
```

APK çıktısı:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Debug APK test amaçlıdır. Release APK için imzalama yapılandırması ekleyip:

```bash
./gradlew assembleRelease
```

Release build için ayrıca bir signing key ve `signingConfigs` yapılandırması gerekir. Bu dosyaya özel anahtar veya parola eklenmemiştir.

## Replit üzerinde APK oluşturma durumu

Bu Replit ortamında Gradle Wrapper başarıyla çalıştı. Ancak ortamda Android SDK Platform 35 kurulu olmadığı için `assembleDebug` adımı tamamlanamadı:

```text
SDK location not found
```

Bu, Gradle Wrapper eksikliği değil, Android SDK'nın bu ortama kurulu olmamasıyla ilgilidir. Replit üzerinde SDK kurulumu ve APK üretimi garanti edilmediği için burada APK dosyası üretilmiş gibi kabul edilmemelidir. Proje, SDK kurulabilen AndroidIDE/Termux veya bilgisayar ortamında derlenmeye hazırdır.

## ZIP olarak dışa aktarma

ZIP'e yalnızca `android-auto-clicker` klasörünü dahil edin. ZIP açıldığında üst klasörde doğrudan aşağıdakiler bulunmalıdır:

```text
android-auto-clicker/
├── app/
├── gradle/
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── build.gradle.kts
└── gradle.properties
```

Şunları ZIP'e dahil etmeyin:

- `app/build/`
- `.gradle/`
- `local.properties`
- `.idea/`

Terminalden dışa aktarmak için:

```bash
zip -r android-auto-clicker.zip android-auto-clicker \
  -x "android-auto-clicker/.gradle/*" \
  -x "android-auto-clicker/**/build/*" \
  -x "android-auto-clicker/local.properties" \
  -x "android-auto-clicker/.idea/*"
```

Replit dosya panelinde klasörü seçip **Download/Export as ZIP** seçeneği varsa aynı klasörü ZIP olarak indirebilirsiniz.

## İlk kullanım

1. Uygulamayı açın.
2. `Accessibility access needed` kartındaki **Enable** düğmesine dokunun.
3. Android ayarlarında **ClickFlow Pro** erişilebilirlik servisini etkinleştirin ve sistem onayını verin.
4. Uygulamaya dönün; izin kartı kaybolur ve **Start clicking** aktifleşir.
5. CPS veya milisaniye aralığını ayarlayın.
6. `Tap points` bölümünden nokta ekleyip X/Y koordinatlarını girin.
7. Başlatın. Floating panel başka uygulamaların üzerinde görünür.

## Manifest ve Accessibility Service

Accessibility Service yapılandırması `app/src/main/AndroidManifest.xml` ve `app/src/main/res/xml/accessibility_service_config.xml` dosyalarındadır.

Manifest'te aşağıdaki kritik yapılandırmalar mevcuttur:

- `android.permission.BIND_ACCESSIBILITY_SERVICE`
- `android.accessibilityservice.AccessibilityService` intent action
- `android:canPerformGestures="true"`
- `android:accessibilityFeedbackType="feedbackGeneric"`
- `android:settingsActivity="com.clickflowpro.app.MainActivity"`

Android 8+ cihazlarda floating panel için `TYPE_ACCESSIBILITY_OVERLAY` kullanılır. Kullanıcının ayrıca genel “diğer uygulamaların üzerinde göster” izni vermesi gerekmez; Accessibility Service'in kendisi Android ayarlarından etkinleştirilmelidir.

## Güvenlik ve uyumluluk

- Uygulama yalnızca kullanıcı Android ayarlarından Accessibility Service'i etkinleştirdikten sonra dokunma gönderebilir.
- Servis `START_NOT_STICKY` çalışır; cihaz/uygulama süreci beklenmedik şekilde kapanırsa otomatik olarak yeniden başlamaz.
- `onInterrupt`, `onUnbind` ve `onDestroy` içinde zamanlayıcılar ve floating panel temizlenir.
- Otomatik dokunuşlar Android 7.0+ gesture API'si ile yapılır. Projenin minimum Android sürümü API 26'dır.
- Accessibility Service, uygulama dışında dokunma gönderebildiği için yalnızca güvendiğiniz cihazlarda ve uygulamalarda kullanın.

## Proje yapısı

```text
app/src/main/java/com/clickflowpro/app/
├── MainActivity.kt
├── ClickFlowApp.kt
├── data/ProfileStore.kt
├── model/ClickPoint.kt
├── model/ClickProfile.kt
├── util/LocaleHelper.kt
├── util/OnboardingPrefs.kt
└── service/
    ├── AutoClickAccessibilityService.kt
    ├── ClickerContract.kt
    ├── PointPickerOverlay.kt
    └── FloatingControlPanel.kt
```