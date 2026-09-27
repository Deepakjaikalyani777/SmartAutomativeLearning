package com.deepak.automotive.media

import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat

/**
 * Media apps in cars DON'T draw their own UI.
 *
 *  Car Media Center (OEM UI) ──MediaBrowser──▶ this service : "what can I browse?"
 *  Car Media Center          ──MediaController──▶ MediaSession : "play / pause / next"
 *  Steering-wheel buttons    ──KeyEvent──▶ active MediaSession
 *
 * The same service works in Android Auto (phone projection) and Android Automotive OS.
 */
class CarMediaBrowserService : MediaBrowserServiceCompat() {

    private lateinit var session: MediaSessionCompat
    private var current: Track? = null

    override fun onCreate() {
        super.onCreate()
        session = MediaSessionCompat(this, "AutomotiveAcademy").apply {
            setCallback(SessionCallback())
            isActive = true
        }
        sessionToken = session.sessionToken
        updateState(PlaybackStateCompat.STATE_NONE, 0)
    }

    override fun onDestroy() {
        session.release()
        super.onDestroy()
    }

    /**
     * Called by every client that wants to connect. SECURITY: this is where you check the
     * caller's package/signature (e.g. allow only the system media center + Android Auto).
     */
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        val extras = Bundle().apply {
            // Content-style hints tell the car UI to render categories as grid and songs as list.
            putInt(CONTENT_STYLE_BROWSABLE, CONTENT_STYLE_GRID)
            putInt(CONTENT_STYLE_PLAYABLE, CONTENT_STYLE_LIST)
        }
        return BrowserRoot(MediaCatalog.ROOT_ID, extras)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaItem>>) {
        val items = if (parentId == MediaCatalog.ROOT_ID) {
            MediaCatalog.categories.map { c ->
                MediaItem(
                    MediaDescriptionCompat.Builder().setMediaId(c.id).setTitle(c.title).build(),
                    MediaItem.FLAG_BROWSABLE,
                )
            }
        } else {
            MediaCatalog.categories.firstOrNull { it.id == parentId }?.tracks.orEmpty().map { t ->
                MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId(t.id).setTitle(t.title).setSubtitle(t.artist).build(),
                    MediaItem.FLAG_PLAYABLE,
                )
            }
        }
        result.sendResult(items.toMutableList())
    }

    private inner class SessionCallback : MediaSessionCompat.Callback() {
        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            MediaCatalog.track(mediaId)?.let { play(it) }
        }

        override fun onPlay() = play(current ?: MediaCatalog.allTracks.first())

        override fun onPause() = updateState(PlaybackStateCompat.STATE_PAUSED, position())

        override fun onSkipToNext() = skip(+1)
        override fun onSkipToPrevious() = skip(-1)
    }

    private fun skip(delta: Int) {
        val all = MediaCatalog.allTracks
        val i = all.indexOf(current).coerceAtLeast(0)
        play(all[(i + delta + all.size) % all.size])
    }

    private fun play(track: Track) {
        // A real player would request AUDIOFOCUS_GAIN with USAGE_MEDIA here and
        // start a foreground service with a MediaStyle notification.
        current = track
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.id)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationMs)
                .build()
        )
        updateState(PlaybackStateCompat.STATE_PLAYING, 0)
    }

    private fun position(): Long {
        val s = session.controller.playbackState ?: return 0
        if (s.state != PlaybackStateCompat.STATE_PLAYING) return s.position
        return s.position + (SystemClock.elapsedRealtime() - s.lastPositionUpdateTime)
    }

    private fun updateState(state: Int, positionMs: Long) {
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                // Only advertise actions you support: the car UI shows exactly these buttons.
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                // Speed 1f lets clients extrapolate the seek bar without polling us.
                .setState(state, positionMs, if (state == PlaybackStateCompat.STATE_PLAYING) 1f else 0f)
                .build()
        )
    }

    companion object {
        const val CONTENT_STYLE_BROWSABLE = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        const val CONTENT_STYLE_PLAYABLE = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        const val CONTENT_STYLE_LIST = 1
        const val CONTENT_STYLE_GRID = 2
    }
}
