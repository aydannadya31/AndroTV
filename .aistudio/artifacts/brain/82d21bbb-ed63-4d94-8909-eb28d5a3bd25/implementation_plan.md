# MediaDash Hub - Android TV Akıllı Gösterge & Yerel Medya Merkezi

MediaDash Hub, Android TV kutuları için oturma odasını hem anlık ev otomasyonu/çevre verilerini sunan akıllı bir gösterge paneline (Smart Dash) hem de yerel ağ ve dahili depolamadaki medyaları yöneten, D-pad kumanda odaklı yüksek performanslı bir medya merkezine (MedyaKutusu) dönüştüren bütünleşik bir sistem mimarisidir.

### Kullanıcı İncelemesi ve Alınan Kritik Kararlar

> [!IMPORTANT]
> Kullanıcı ile 1. Aşamada netleştirilen tercihler ve mimari kararlar:
> - **Akıllı Ev & Sensör Entegrasyonu:** MQTT protokolü ve yerel ağ soket altyapısı (Home Assistant uyumlu JSON veri akışı ile).
> - **Depolama & Medya Kaynakları:** Hibrit model; hem dahili/USB yerel depolama hem de yerel ağ (SMB / HTTP paylaşım) kaynaklarının taranması.
> - **D-pad Odaklama & Animasyon:** D-pad ile odaklanılan kartlarda yumuşak ölçekleme (`scale(1.08f)`) ve zümrüt yeşili (`#10B981` / `#4CAF50`) parıltı/çerçeve vurgusu.
> - **Bağımlılık Kısıtı:** Projeye hiçbir dış bağımlılık eklenmeden, mevcut Compose Foundation, Room Database, Coroutines ve OkHttp/Moshi altyapısı ile saf, yüksek performanslı TV UI deneyimi inşa edilecektir.

---

### 1. Genel Bakış ve Temel Konsept

- **Ne Yapar:** TV ekranını yatayda iki ana bölüme ayıran (Split-Dashboard) bir deneyim sunar. Sol panelde saat, hava durumu, MQTT tabanlı ev sensörleri (sıcaklık, nem, aydınlatma, güvenlik durumları) yer alırken; sağ panelde kategorize edilmiş medya afişleri, son izlenenlere hızlı devam etme (resume) ve oynatıcı entegrasyonu bulunur.
- **Hedef Kitle:** Evinde Android TV / Google TV kutusu kullanan, ev otomasyonu (Home Assistant / MQTT) ile ilgilenen ve yerel medyasını (NAS / USB) tek merkezden yönetmek isteyen kullanıcılar.
- **Temel Değer:** İki ayrı uygulama açma ihtiyacını ortadan kaldırarak tek ekranda hem ev durumunu kontrol etme hem de sinema/müzik deneyimine kesintisiz erişme konforu.

---

### 2. Kullanıcı Deneyimi ve Görsel Tasarım

- **Split-Dashboard Düzeni (35% Sol / 65% Sağ):**
  - **Sol Panel (Smart Dash):**
    - Saat, tarih, hava durumu ve hava kalitesi kartı.
    - MQTT/Home Assistant sensör durumu widget'ları (Oturma Odası Sıcaklık, Nem, Işık Anahtarları, Enerji Tüketimi).
    - Hızlı ev eylem butonları (Tüm Işıkları Kapat, Sinema Modu, Havalandırma).
  - **Sağ Panel (MedyaKutusu):**
    - "İzlemeye Devam Et" (Resume) öne çıkan afiş şeridi.
    - Kategori sekmeleri (Filmler, Diziler, Müzik, Ağ Paylaşımları).
    - 4K / HDR format etiketleri, süre ve izlenme yüzdesi göstergeleri.
- **TV D-pad Odak Yönetimi (Focus Management):**
  - TV kumandasında yukarı, aşağı, sağ ve sol tuşlarına basıldığında odaklar doğal grid/liste hiyerarşisinde akar.
  - Odaklanan öğe `animateFloatAsState` ile %8 oranında büyür (`1.08f`), zümrüt yeşili sınır çizgisi (`BorderStroke(2.dp, Color(0xFF10B981))`) ve arkasında derinlikli gölge oluşturur.
  - Tıklama (D-pad Center / Enter) ile anında aksiyon tetiklenir veya medya detay/oynatma paneli açılır.
- **Renk Paleti & Atmosfer:**
  - Arka Plan: Koyu Gece Mavisi / Grafit (`#0B0F19`, `#111827`)
  - Kart Yüzeyleri: Yarı saydam koyu cam efekti (`#1F2937` / `#161E2E`)
  - Vurgu Rengi: Zümrüt Yeşili (`#10B981` / `#4CAF50`)
  - İkincil Renk: Mavi / Camgöbeği (`#06B6D4`)
  - Tipografi: Geniş ekran TV mesafesinden (2-3 metre) rahat okunabilir büyük yazı tipi hiyerarşisi.

---

### 3. Temel Ürün Kararları ve Mimari Tercihler

- **Karar 1: Compose TV kütüphanesi yerine Compose Foundation Focus API Kullanımı**
  - *Yaklaşım:* Kullanıcının "Yeni bağımlılık ekleme" kuralına sadık kalarak, `Modifier.focusable()`, `Modifier.onFocusChanged()`, `FocusRequester` ve `animateFloatAsState` ile Android TV'ye tam uyumlu akıcı bir odak motoru geliştirilmesi.
  - *Neden:* Mevcut Gradle yapısını bozmadan sıfır harici paketle en yüksek uyumluluk ve performans.
- **Karar 2: Room SQLite Veritabanı ile Çift Yönlü İndeksleme**
  - *Yaklaşım:* Medya öğeleri (`MediaItemEntity`) ve Akıllı Ev Sensörleri (`SmartSensorEntity`) Room ile yerel olarak önbelleklenir.
  - *Neden:* TV kutusu internete bağlı olmasa dahi yerel medya ve son bilinen sensör verileri sıfır gecikmeyle ekrana gelir.
- **Karar 3: Hibrit Ağ & Medya Dizinleyici Servisi**
  - *Yaklaşım:* Kotlin Coroutines tabanlı arka plan tarayıcısı hem yerel depolama dizinlerini hem de yapılandırılan ağ paylaşımlarını asenkron olarak tarar ve Room veritabanına indeksler.
- **Karar 4: MQTT ve Soket İletişim Katmanı**
  - *Yaklaşım:* Java NIO / OkHttp WebSocket ve yerel soket altyapısıyla hafif MQTT paket/JSON akışı ayrıştırıcısı.

---

### 4. Teknik Mimari ve Veri Stratejisi

```
┌────────────────────────────────────────────────────────────────────────┐
│                        MediaDash Hub Android TV                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
               ┌────────────────────┴───────────────────┐
               ▼                                        ▼
   ┌───────────────────────┐                ┌───────────────────────┐
   │ Smart Dash (Sol Panel)│                │ MediaKutusu (Sağ Panel│
   │  - Saat & Hava Durumu │                │  - Resume / İzlemeye  │
   │  - MQTT Sensörler     │                │    Devam Et Vitrini   │
   │  - Ev Hızlı Aksiyonlar│                │  - Kategori Rafları   │
   └───────────┬───────────┘                └───────────┬───────────┘
               │                                        │
               └────────────────────┬───────────────────┘
                                    │
               ▼────────────────────┴───────────────────▼
               │       MainDashboardViewModel           │
               │  - uiState: StateFlow<DashboardUiState>│
               │  - currentFocusedSection / D-pad Nav   │
               └────────────────────┬───────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
┌───────────────┐           ┌───────────────┐           ┌───────────────┐
│ Room Veri     │           │ Medya Tarayıcı│           │ MQTT / Soket  │
│ Katmanı       │           │ Servisi       │           │ Entegrasyonu  │
│ - MediaDao    │           │ - Local File  │           │ - SensorFeed  │
│ - SensorDao   │           │   Scanner     │           │ - Home Assist │
│ - AppDatabase │           │ - Network SMB │           │   Action Hub  │
└───────────────┘           └───────────────┘           └───────────────┘
```

#### Modül ve Dizin Yapısı:
- `com.example.ui.components`: TV odaklanabilir kartlar (`TvFocusableCard`), sensör widget'ları, medya poster kartları.
- `com.example.ui.screens`: `MainDashboardScreen`, sol panel (`SmartDashPanel`), sağ panel (`MediaKutusuPanel`).
- `com.example.ui.theme`: Android TV renk paleti, tipografi ve D-pad odak stilleri.
- `com.example.data.local`: Room Entities (`MediaItem`, `SensorDevice`), DAOs ve `MediaDashDatabase`.
- `com.example.data.repository`: `MediaRepository` ve `SmartHomeRepository`.
- `com.example.service`: `MediaScannerService` ve `MqttNetworkService`.
- `com.example.player`: Medya oynatma durumu ve donanımsal video yüzeyi kontrolcüsü.

---

### 5. Doğrulama ve Test Adımları
1. Kod değişikliklerinin mevcut bağımlılıkları koruyarak hatasız derlenmesi (`compile_applet`).
2. TV D-pad yön tuşları simülasyonu ve odak kaybı yaşanmamasının garanti edilmesi.
3. Room veritabanı ksp kod üretiminin doğrulanması.
4. UI'da donma olmadan arka plan indeksleme akışının test edilmesi.
