package com.example.chargingclock

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

class MusicService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this // Сохраняем ссылку на сервис
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        sessionManager?.addOnActiveSessionsChangedListener(sessionListener, ComponentName(this, MusicService::class.java))
        refreshControllers()
    }

    override fun onListenerDisconnected() {
        sessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
        instance = null
        super.onListenerDisconnected()
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateController(controllers)
    }

    // Публичный метод для принудительного обновления
    fun refreshControllers() {
        try {
            val controllers = sessionManager?.getActiveSessions(ComponentName(this, MusicService::class.java))
            updateController(controllers)
        } catch (e: SecurityException) {
            // Нет прав
        }
    }

    private fun updateController(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) return

        // 1. Ищем тот, который играет или буферизируется
        var selectedController = controllers.firstOrNull {
            val state = it.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        }

        // 2. Если никто не играет, берем ПЕРВЫЙ активный (чтобы можно было нажать Play)
        if (selectedController == null) {
            selectedController = controllers.firstOrNull()
        }

        // Если контроллер сменился или был null
        if (currentController?.sessionToken != selectedController?.sessionToken) {
            // Отписываемся от старого
            currentController?.unregisterCallback(callback)

            currentController = selectedController

            // Подписываемся на новый
            currentController?.registerCallback(callback)

            // Принудительно вызываем обновление UI сразу
            val metadata = currentController?.metadata
            val state = currentController?.playbackState
            updateUI?.invoke(currentController)
        } else {
            // Контроллер тот же, просто обновим UI на всякий случай
            updateUI?.invoke(currentController)
        }
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateUI?.invoke(currentController)
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateUI?.invoke(currentController)
        }
    }

    companion object {
        var instance: MusicService? = null
        var sessionManager: MediaSessionManager? = null
        var currentController: MediaController? = null
        var updateUI: ((MediaController?) -> Unit)? = null
    }
}