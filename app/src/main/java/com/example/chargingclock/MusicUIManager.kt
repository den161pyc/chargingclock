package com.example.chargingclock

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.SystemClock
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView

class MusicUIManager(
    private val tvTrackTitle: TextView,
    private val tvTrackArtist: TextView,
    private val ivAlbumArt: ImageView,
    private val seekBarMusic: SeekBar,
    private val btnPlayPause: ImageButton,
    private val handler: Handler
) {
    var isTrackingTouch = false

    // Runnable для обновления прогресс-бара
    private val musicProgressRunnable = object : Runnable {
        override fun run() {
            updateMusicProgress()
            if (MusicService.currentController?.playbackState?.state == PlaybackState.STATE_PLAYING) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun updateUI(controller: MediaController?) {
        if (controller == null) {
            tvTrackTitle.text = "Play Music"
            tvTrackArtist.text = "Waiting..."
            seekBarMusic.progress = 0
            seekBarMusic.max = 100
            ivAlbumArt.setImageResource(R.drawable.ic_music_note)
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Track"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Artist"
        val bitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        tvTrackTitle.text = title
        tvTrackArtist.text = artist

        if (bitmap != null) {
            ivAlbumArt.setImageBitmap(bitmap)
        } else {
            try { ivAlbumArt.setImageResource(R.drawable.ic_music_note) } catch (e: Exception) {}
        }

        if (playbackState?.state == PlaybackState.STATE_PLAYING) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            handler.removeCallbacks(musicProgressRunnable)
            handler.post(musicProgressRunnable)
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            handler.removeCallbacks(musicProgressRunnable)
        }

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        if (duration > 0) seekBarMusic.max = duration.toInt()

        updateMusicProgress()
    }

    private fun updateMusicProgress() {
        val controller = MusicService.currentController
        if (!isTrackingTouch && controller != null) {
            val playbackState = controller.playbackState
            if (playbackState != null) {
                var currentPos = playbackState.position
                if (playbackState.state == PlaybackState.STATE_PLAYING) {
                    val lastUpdateTime = playbackState.lastPositionUpdateTime
                    if (lastUpdateTime > 0) {
                        val timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime
                        val speed = playbackState.playbackSpeed
                        currentPos += (timeDelta * speed).toLong()
                    }
                }
                if (currentPos > seekBarMusic.max) currentPos = seekBarMusic.max.toLong()
                if (currentPos < 0) currentPos = 0
                seekBarMusic.progress = currentPos.toInt()
            }
        }
    }

    fun stopUpdates() {
        handler.removeCallbacks(musicProgressRunnable)
    }

    fun startUpdates() {
        if (MusicService.currentController?.playbackState?.state == PlaybackState.STATE_PLAYING) {
            handler.post(musicProgressRunnable)
        }
    }
}