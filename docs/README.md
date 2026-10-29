# MediaDash Hub — Web & APK Dağıtımı

Bu dizin, **MediaDash Hub** uygulamasının Android TV ve Android Telefon cihazlarına ayrılmış bağımsız, birbirinden farklı ve özelleşmiş APK paketlerini doğrudan web üzerinden indirmeyi sağlayan resmi GitHub Pages dağıtım sitesini içerir.

## Cihaza Özel Bağımsız APK Paketleri

### 1. Android TV APK (`MediaDashHub-TV-v1.0.apk`)
- **Paket Kimliği (Application ID):** `com.aistudio.mediadashhub.tv`
- **Uygulama Adı:** `MediaDash Hub TV`
- **Ana Başlatıcı Aktivitesi:** `com.example.TvActivity` (Leanback Launcher & Standard Launcher)
- **Ekran Yönü:** Yatay (Landscape)
- **Özel Özellikler:**
  - 10-foot TV arayüzü ve D-Pad kumanda odak sistemi
  - Canlı saat, hava durumu ve MQTT ev otomasyon sensör paneli
  - 4K donanımsal video/ses alıcı oynatıcı
  - TV ekranında telefonların bağlanması için optik eşleşme QR kodu gösterimi
- **Kısıtlamalar:** Kamera izni, arka plan telefon iletim servisi veya mobil arayüz kalıntıları barındırmaz. `android.software.leanback` zorunludur.

### 2. Android Telefon APK (`MediaDashHub-Phone-v1.0.apk`)
- **Paket Kimliği (Application ID):** `com.aistudio.mediadashhub.phone`
- **Uygulama Adı:** `MediaDash Cast`
- **Ana Başlatıcı Aktivitesi:** `com.example.PhoneActivity`
- **Ekran Yönü:** Dikey (Portrait)
- **Özel Özellikler:**
  - TV ekranındaki QR kodu okumak için entegre optik kamera tarayıcı
  - Fotoğraf, video ve kayıpsız ses/müzik dosyaları için galeri seçici
  - TV hafızasını doldurmadan doğrudan telefon üzerinden canlı HTTP/TCP soket akışı
  - `PhoneStreamingForegroundService` ve WakeLock ile telefon ekranı kapandığında/kilitlendiğinde bile yayını kesintisiz sürdürme
- **Kısıtlamalar:** TV Leanback özellikleri veya TV dashboard arayüzü barındırmaz. `android.hardware.camera` ve dokunmatik ekran zorunludur.

## GitHub Pages Canlı Yayın Adresi
- **Web Sitesi:** https://aydannadya31.github.io/AndroTV/
- **TV APK Doğrudan İndir:** https://aydannadya31.github.io/AndroTV/MediaDashHub-TV-v1.0.apk
- **Telefon APK Doğrudan İndir:** https://aydannadya31.github.io/AndroTV/MediaDashHub-Phone-v1.0.apk
