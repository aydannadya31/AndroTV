# MediaDash Hub (AndroTV)

Android TV ve Android telefonlar için **akıllı ev gösterge paneli + yerel medya merkezi**.
TV ekranında ev otomasyonu verilerini gösterir, telefondaki video/müzik/fotoğrafları yerel soket üzerinden TV'ye yansıtır.

**Canlı dağıtım sitesi:** https://aydannadya31.github.io/AndroTV/
**APK indirme:** https://aydannadya31.github.io/AndroTV/MediaDashHub-v1.0.apk (23 MB, Android 7.0+ / API 24+)

## Özellikler

- Split dashboard: sol panelde canlı saat, hava durumu ve MQTT sensörleri; sağ panelde yerel medya afiş akışı
- Soket tabanlı medya yansıtma (yerel TCP `:8095`), parçalı akış ile büyük dosya desteği
- QR kod + 6 haneli PIN ile güvenli eşleştirme ve token tabanlı yetkilendirme
- Room ile yerel medya/sensör önbelleği, Compose tabanlı TV odak motoru (D-pad uyumlu)
- Firebase AI (Gemini) ve App Check desteği

## Depo yapısı

| Yol | İçerik |
| --- | --- |
| `app/src/main/java/com/example/ui/` | Ekranlar, bileşenler ve tema |
| `app/src/main/java/com/example/service/` | Medya tarama, Cast hub, MQTT, soket ve eşleştirme servisleri |
| `app/src/main/java/com/example/data/` | Room veritabanı, DAO'lar ve repository'ler |
| `docs/` | GitHub Pages dağıtım sitesi + derlenmiş APK |
| `.aistudio/` | AI Studio proje planı |

## Derleme

Gerekenler: JDK 11+, Android SDK (compileSdk 36), Gradle 8.x (Android Studio ile de açılabilir).

```bash
gradle :app:assembleRelease
```

İmzalama ve gizli anahtarlar ortam değişkenlerinden gelir:

- `KEYSTORE_PATH` → keystore dosya yolu (varsayılan `my-upload-key.jks`)
- `STORE_PASSWORD`, `KEY_ALIAS` (`upload`), `KEY_PASSWORD`
- `GEMINI_API_KEY` → uygulamanın Gemini API çağrıları için; `.env.example` içindeki yorum satırlarını açıp doldurun

`.env` ve `debug.keystore` `.gitignore` içindedir; commit edilmez.

## Dağıtım

`docs/` klasörü GitHub Pages tarafından `main` dalından yayınlanır. Site ayarları:
`Settings → Pages → Deploy from a branch → main /docs`.

## Lisans

Bu depoda açık bir lisans dosyası bulunmuyor.
