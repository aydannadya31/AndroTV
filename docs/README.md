# MediaDash Hub — Web & APK Dağıtımı

Bu dizin, **MediaDash Hub** Android uygulamasının hem Android TV hem de Android Telefonlara doğrudan web üzerinden indirilmesini sağlayan resmi GitHub Pages dağıtım sitesini içerir.

## Canlı Dağıtım Dosyaları

- `index.html`: Modern, responsive ve zümrüt yeşili temalı web indirme sayfası.
- `MediaDashHub-v1.0.apk`: Derlenmiş, imzalanmış 23 MB boyutundaki hazır Android kurulum paketi (Android TV & Mobil universal APK).

## GitHub Pages Canlı Yayına Alma Adımları

1. Reponuzu GitHub'a gönderin (AI Studio üst menüsünden **Push to GitHub** butonuna basın veya terminalden push yapın).
2. GitHub reponuzda **Settings (Ayarlar) -> Pages** sekmesine gidin.
3. **Build and deployment** bölümünde:
   - **Source:** `Deploy from a branch` seçin.
   - **Branch:** `main` (veya `master`) ve klasör olarak **/docs** seçin.
   - **Save** butonuna tıklayın.
4. Siteniz 1 dakika içinde şu adreste canlıya alınır:
   `https://<kullanici-adi>.github.io/<repo-adi>/`
