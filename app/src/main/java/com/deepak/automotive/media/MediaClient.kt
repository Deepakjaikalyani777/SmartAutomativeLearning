package com.deepak.automotive.media

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Connects to our own [CarMediaBrowserService] exactly the way the car's Media Center does.
 * Useful to learn (and to unit/UI-test) the browse + control contract without a car.
 */
class MediaClient(private val context: Context) {

    data class Node(val id: String, val title: String, val subtitle: String?, val browsable: Boolean)

    private val _tree = MutableStateFlow<Map<String, List<Node>>>(emptyMap())
    val tree: StateFlow<Map<String, List<Node>>> = _tree.asStateFlow()

    private val _metadata = MutableStateFlow<MediaMetadataCompat?>(null)
    val metadata: StateFlow<MediaMetadataCompat?> = _metadata.asStateFlow()

    private val _playback = MutableStateFlow<PlaybackStateCompat?>(null)
    val playback: StateFlow<PlaybackStateCompat?> = _playback.asStateFlow()

    private var controller: MediaControllerCompat? = null

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(m: MediaMetadataCompat?) { _metadata.value = m }
        override fun onPlaybackStateChanged(s: PlaybackStateCompat?) { _playback.value = s }
    }

    private val browser: MediaBrowserCompat = MediaBrowserCompat(
        context,
        ComponentName(context, CarMediaBrowserService::class.java),
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                controller = MediaControllerCompat(context, browser.sessionToken).also {
                    it.registerCallback(controllerCallback)
                    _metadata.value = it.metadata
                    _playback.value = it.playbackState
                }
                load(browser.root)
            }
        },
        null,
    )

    fun connect() { if (!browser.isConnected) browser.connect() }

    fun disconnect() {
        controller?.unregisterCallback(controllerCallback)
        browser.disconnect()
    }

    fun load(parentId: String) {
        browser.subscribe(parentId, object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
                val nodes = children.map {
                    Node(it.mediaId ?: "", it.description.title.toString(), it.description.subtitle?.toString(), it.isBrowsable)
                }
                _tree.value = _tree.value + (parentId to nodes)
                nodes.filter { it.browsable }.forEach { load(it.id) }
            }
        })
    }

    val rootId: String get() = MediaCatalog.ROOT_ID

    fun play(mediaId: String) = controller?.transportControls?.playFromMediaId(mediaId, null)
    fun togglePlayPause() {
        val playing = _playback.value?.state == PlaybackStateCompat.STATE_PLAYING
        if (playing) controller?.transportControls?.pause() else controller?.transportControls?.play()
    }
    fun next() = controller?.transportControls?.skipToNext()
    fun previous() = controller?.transportControls?.skipToPrevious()
}
