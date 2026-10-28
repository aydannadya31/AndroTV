package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MediaDashDatabase
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaSource
import com.example.data.local.MediaType
import com.example.data.local.SmartSensorEntity
import com.example.data.repository.MediaRepository
import com.example.data.repository.SmartHomeRepository
import com.example.player.MediaPlayerState
import com.example.player.PlaybackStatus
import com.example.service.CastHubManager
import com.example.service.CastPayload
import com.example.service.MediaScannerService
import com.example.service.MqttNetworkService
import com.example.service.PairingSession
import com.example.service.PairingState
import com.example.service.SocketTransmissionService
import com.example.service.TransferProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WeatherInfo(
    val temp: String = "19°C",
    val condition: String = "Parçalı Bulutlu",
    val humidity: String = "%52",
    val aqi: String = "AQI 32 (İyi)",
    val wind: String = "14 km/s"
)

enum class MediaTabFilter(val title: String) {
    ALL("Tüm Medya"),
    MOVIES("Filmler"),
    SERIES("Diziler"),
    MUSIC("Müzik"),
    NETWORK("Yerel Ağ / SMB"),
    PHONE_CAST("Telefondan Yansıtılanlar")
}

enum class DeviceMode {
    TV_HUB,
    PHONE_SENDER
}

data class DashboardUiState(
    val timeString: String = "20:45",
    val dateString: String = "28 Ekim Çarşamba",
    val weather: WeatherInfo = WeatherInfo(),
    val sensors: List<SmartSensorEntity> = emptyList(),
    val resumeItems: List<MediaItemEntity> = emptyList(),
    val displayedMedia: List<MediaItemEntity> = emptyList(),
    val selectedTab: MediaTabFilter = MediaTabFilter.ALL,
    val playerState: MediaPlayerState = MediaPlayerState(),
    val isScanning: Boolean = false,
    val isCinemaModeActive: Boolean = false,
    val deviceMode: DeviceMode = DeviceMode.TV_HUB,
    val tvIpAddress: String = "192.168.1.100",
    val tvPairCode: String = "482910",
    val castNotification: String? = null,
    val isPhoneCasting: Boolean = false,
    val targetTvIp: String = "192.168.1.100",
    val activePhoneSession: PairingSession? = null,
    val pairingState: PairingState = PairingState.UNPAIRED,
    val activeTransferProgress: TransferProgress? = null,
    val pairingErrorMessage: String? = null
)

class MainDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MediaDashDatabase.getDatabase(application)
    private val scannerService = MediaScannerService(application)
    private val mqttService = MqttNetworkService()
    val castHubManager = CastHubManager(application)
    val socketTransmissionService = SocketTransmissionService(application)

    private val mediaRepository = MediaRepository(database.mediaDao(), scannerService)
    private val smartHomeRepository = SmartHomeRepository(database.sensorDao(), mqttService)

    private val _selectedTab = MutableStateFlow(MediaTabFilter.ALL)
    private val _playerState = MutableStateFlow(MediaPlayerState())
    private val _isScanning = MutableStateFlow(false)
    private val _isCinemaModeActive = MutableStateFlow(false)

    private val _deviceMode = MutableStateFlow(DeviceMode.TV_HUB)
    private val _castNotification = MutableStateFlow<String?>(null)
    private val _isPhoneCasting = MutableStateFlow(false)
    private val _targetTvIp = MutableStateFlow(castHubManager.getLocalIpAddress())

    private val _pairingState = MutableStateFlow(PairingState.UNPAIRED)
    private val _pairingErrorMessage = MutableStateFlow<String?>(null)
    private val _activeTransferProgress = MutableStateFlow<TransferProgress?>(null)

    private val _timeString = MutableStateFlow("")
    private val _dateString = MutableStateFlow("")

    private var playbackJob: Job? = null

    init {
        updateTime()
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                updateTime()
            }
        }

        viewModelScope.launch {
            mediaRepository.syncMediaLibrary()
            smartHomeRepository.initializeSensors()
            smartHomeRepository.startMqttSubscription(this)
        }

        // TV HTTP & Raw Socket Alıcılarını Başlat
        castHubManager.startTvReceiver(viewModelScope)
        socketTransmissionService.startTvSocketServer(viewModelScope)

        // Socket üzerinden tamamlanan medyaları dinle
        viewModelScope.launch {
            socketTransmissionService.completedMediaFlow.collect { mediaItem ->
                val id = mediaRepository.addMediaItem(mediaItem)
                val savedItem = mediaItem.copy(id = id)
                _castNotification.value = "🔒 Güvenli Socket: \"${mediaItem.title}\" TV'de açıldı!"
                onMediaSelected(savedItem)
                delay(7000)
                _castNotification.value = null
            }
        }

        // Canlı veri iletim ilerlemesini dinle
        viewModelScope.launch {
            socketTransmissionService.transferProgressFlow.collect { progress ->
                _activeTransferProgress.value = progress
                if (progress.isComplete) {
                    delay(3000)
                    _activeTransferProgress.value = null
                }
            }
        }

        // HTTP Cast alıcısını dinle
        viewModelScope.launch {
            castHubManager.incomingCastFlow.collect { payload ->
                val item = castHubManager.toMediaItem(payload)
                val id = mediaRepository.addMediaItem(item)
                val savedItem = item.copy(id = id)
                _castNotification.value = "📱 ${payload.senderName} TV'ye yansıttı: ${payload.title}"
                onMediaSelected(savedItem)
                delay(7000)
                _castNotification.value = null
            }
        }
    }

    private fun updateTime() {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale("tr", "TR"))
        val dateFormat = SimpleDateFormat("d MMMM EEEE", Locale("tr", "TR"))
        _timeString.value = timeFormat.format(now)
        _dateString.value = dateFormat.format(now)
    }

    private val mediaStateFlow = combine(
        mediaRepository.allMedia,
        mediaRepository.resumeMedia,
        _selectedTab
    ) { allMedia, resumeMedia, tab ->
        val filtered = when (tab) {
            MediaTabFilter.ALL -> allMedia
            MediaTabFilter.MOVIES -> allMedia.filter { it.type == MediaType.MOVIE }
            MediaTabFilter.SERIES -> allMedia.filter { it.type == MediaType.SERIES }
            MediaTabFilter.MUSIC -> allMedia.filter { it.type == MediaType.MUSIC }
            MediaTabFilter.NETWORK -> allMedia.filter { it.source == MediaSource.SMB_SHARE || it.source == MediaSource.NAS_SERVER }
            MediaTabFilter.PHONE_CAST -> allMedia.filter { it.source == MediaSource.PHONE_CAST }
        }
        Triple(allMedia, resumeMedia, filtered)
    }

    private val controlStateFlow = combine(
        _playerState,
        _isScanning,
        _isCinemaModeActive
    ) { player, scanning, cinemaMode ->
        Triple(player, scanning, cinemaMode)
    }

    private val timeStateFlow = combine(
        _timeString,
        _dateString
    ) { time, date ->
        Pair(time, date)
    }

    private val castStateFlow = combine(
        _deviceMode,
        _castNotification,
        _isPhoneCasting,
        _targetTvIp
    ) { mode, notification, casting, targetIp ->
        listOf(mode, notification, casting, targetIp)
    }

    private val pairingAndTransferFlow = combine(
        socketTransmissionService.currentPin,
        socketTransmissionService.activePhoneSession,
        _pairingState,
        _activeTransferProgress,
        _pairingErrorMessage
    ) { pin, session, pairState, progress, errorMsg ->
        PairingAndTransferData(pin, session, pairState, progress, errorMsg)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        mediaStateFlow,
        smartHomeRepository.allSensors,
        controlStateFlow,
        timeStateFlow,
        combine(castStateFlow, pairingAndTransferFlow) { castList, ptData -> Pair(castList, ptData) }
    ) { mediaTriple, sensors, controlTriple, timePair, compoundPair ->
        val (_, resumeMedia, filtered) = mediaTriple
        val (player, scanning, cinemaMode) = controlTriple
        val (time, date) = timePair
        val castList = compoundPair.first
        val ptData = compoundPair.second

        val mode = castList[0] as DeviceMode
        val notification = castList[1] as String?
        val casting = castList[2] as Boolean
        val targetIp = castList[3] as String

        DashboardUiState(
            timeString = time,
            dateString = date,
            sensors = sensors,
            resumeItems = resumeMedia,
            displayedMedia = filtered,
            selectedTab = _selectedTab.value,
            playerState = player,
            isScanning = scanning,
            isCinemaModeActive = cinemaMode,
            deviceMode = mode,
            tvIpAddress = castHubManager.getLocalIpAddress(),
            tvPairCode = ptData.pin,
            castNotification = notification,
            isPhoneCasting = casting,
            targetTvIp = targetIp,
            activePhoneSession = ptData.session,
            pairingState = ptData.pairState,
            activeTransferProgress = ptData.progress,
            pairingErrorMessage = ptData.errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun onTabSelected(tab: MediaTabFilter) {
        _selectedTab.value = tab
    }

    fun setDeviceMode(mode: DeviceMode) {
        _deviceMode.value = mode
    }

    fun toggleDeviceMode() {
        _deviceMode.value = if (_deviceMode.value == DeviceMode.TV_HUB) DeviceMode.PHONE_SENDER else DeviceMode.TV_HUB
    }

    fun updateTargetTvIp(ip: String) {
        _targetTvIp.value = ip
    }

    fun regenerateTvPin() {
        socketTransmissionService.regeneratePin()
    }

    /**
     * Telefon Tarafı: PIN Kodu ile TV Eşleştirme İsteği Gönderir
     */
    fun pairWithTv(enteredPin: String) {
        viewModelScope.launch {
            _pairingState.value = PairingState.PAIRING_IN_PROGRESS
            _pairingErrorMessage.value = null

            val result = socketTransmissionService.pairWithTv(_targetTvIp.value, enteredPin)
            if (result.isSuccess) {
                _pairingState.value = PairingState.PAIRED
                _castNotification.value = "🔒 TV ile güvenli eşleşme sağlandı!"
            } else {
                _pairingState.value = PairingState.REJECTED
                _pairingErrorMessage.value = result.exceptionOrNull()?.message ?: "Eşleşme başarısız oldu."
            }
            delay(4000)
            _castNotification.value = null
        }
    }

    fun unpairPhone() {
        socketTransmissionService.unpairPhone()
        _pairingState.value = PairingState.UNPAIRED
    }

    /**
     * Socket Tabanlı Güvenli Parçalı Medya İletimi (Ses, Resim, Video)
     */
    fun streamMediaOverSocket(title: String, type: MediaType, uriOrData: String) {
        val session = socketTransmissionService.activePhoneSession.value
        viewModelScope.launch {
            _isPhoneCasting.value = true

            // Eğer cihaz henüz eşleşmediyse önce hızlı oturum aç
            val activeSession = session ?: socketTransmissionService.pairWithTv(_targetTvIp.value, uiState.value.tvPairCode).getOrNull()

            if (activeSession != null) {
                val success = socketTransmissionService.streamMediaOverSocket(
                    tvIp = _targetTvIp.value,
                    session = activeSession,
                    fileName = title,
                    mediaType = type,
                    fileSizeEstimate = if (type == MediaType.PHOTO) 3L * 1024 * 1024 else if (type == MediaType.MUSIC) 18L * 1024 * 1024 else 45L * 1024 * 1024,
                    onProgressUpdate = { progress ->
                        _activeTransferProgress.value = progress
                    }
                )
                if (success) {
                    _castNotification.value = "✅ \"$title\" soket üzerinden TV'ye iletildi!"
                } else {
                    _castNotification.value = "⚠️ Socket iletiminde hata oluştu."
                }
            } else {
                _castNotification.value = "⚠️ Lütfen önce TV PIN kodu ile eşleşin."
            }

            delay(4000)
            _isPhoneCasting.value = false
            _castNotification.value = null
        }
    }

    fun dismissCastNotification() {
        _castNotification.value = null
    }

    fun onMediaSelected(media: MediaItemEntity) {
        val startPosition = media.lastPositionMs
        _playerState.value = MediaPlayerState(
            activeItem = media,
            status = PlaybackStatus.PLAYING,
            currentPositionMs = startPosition,
            durationMs = media.durationMs
        )
        startPlaybackSimulation()
    }

    fun togglePlayPause() {
        val current = _playerState.value
        if (current.status == PlaybackStatus.PLAYING) {
            _playerState.value = current.copy(status = PlaybackStatus.PAUSED)
            playbackJob?.cancel()
        } else if (current.status == PlaybackStatus.PAUSED || current.status == PlaybackStatus.IDLE) {
            _playerState.value = current.copy(status = PlaybackStatus.PLAYING)
            startPlaybackSimulation()
        }
    }

    fun seekRelative(deltaMs: Long) {
        val current = _playerState.value
        val newPos = (current.currentPositionMs + deltaMs).coerceIn(0L, current.durationMs)
        _playerState.value = current.copy(currentPositionMs = newPos)
    }

    fun closePlayer() {
        val current = _playerState.value
        val item = current.activeItem
        if (item != null) {
            viewModelScope.launch {
                mediaRepository.updateProgress(item.id, current.currentPositionMs)
            }
        }
        playbackJob?.cancel()
        _playerState.value = MediaPlayerState()
    }

    private fun startPlaybackSimulation() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val current = _playerState.value
                if (current.status == PlaybackStatus.PLAYING) {
                    val nextPos = (current.currentPositionMs + 1000L).coerceAtMost(current.durationMs)
                    _playerState.value = current.copy(currentPositionMs = nextPos)
                    if (nextPos >= current.durationMs) {
                        _playerState.value = current.copy(status = PlaybackStatus.COMPLETED)
                        break
                    }
                }
            }
        }
    }

    fun onToggleSensor(sensor: SmartSensorEntity) {
        viewModelScope.launch {
            smartHomeRepository.toggleSwitch(sensor.entityId, sensor.isActive)
        }
    }

    fun toggleCinemaMode() {
        viewModelScope.launch {
            _isCinemaModeActive.value = !_isCinemaModeActive.value
            smartHomeRepository.triggerCinemaMode()
        }
    }

    fun triggerScan() {
        viewModelScope.launch {
            _isScanning.value = true
            mediaRepository.refreshScan()
            delay(1200)
            _isScanning.value = false
        }
    }
}

private data class PairingAndTransferData(
    val pin: String,
    val session: PairingSession?,
    val pairState: PairingState,
    val progress: TransferProgress?,
    val errorMsg: String?
)
