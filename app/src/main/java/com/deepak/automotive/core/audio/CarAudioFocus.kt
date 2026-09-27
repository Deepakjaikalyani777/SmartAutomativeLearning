package com.deepak.automotive.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/**
 * Audio usages that AAOS routes to different "audio contexts" and buses (media, navigation,
 * voice command, call...). The car audio service decides the outcome using its focus
 * interaction matrix: REJECT, EXCLUSIVE (the other stops/pauses) or CONCURRENT (the other ducks).
 */
enum class CarAudioUsage(val usage: Int, val label: String) {
    MEDIA(AudioAttributes.USAGE_MEDIA, "Music"),
    NAVIGATION(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE, "Nav prompt"),
    VOICE(AudioAttributes.USAGE_ASSISTANT, "Voice assistant"),
    CALL(AudioAttributes.USAGE_VOICE_COMMUNICATION, "Phone call"),
    SAFETY(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION, "Chime / alert"),
}

enum class Interaction { EXCLUSIVE, CONCURRENT_DUCK, REJECT }

object FocusMatrix {
    /** What happens to [holder] when [requester] asks for focus (simplified AOSP defaults). */
    fun interaction(holder: CarAudioUsage, requester: CarAudioUsage): Interaction = when {
        holder == CarAudioUsage.CALL && requester == CarAudioUsage.MEDIA -> Interaction.REJECT
        holder == CarAudioUsage.CALL && requester == CarAudioUsage.VOICE -> Interaction.REJECT
        requester == CarAudioUsage.NAVIGATION || requester == CarAudioUsage.SAFETY -> Interaction.CONCURRENT_DUCK
        else -> Interaction.EXCLUSIVE
    }
}

/** Thin wrapper that performs a real focus request through AudioManager. */
class AudioFocusRequester(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var current: AudioFocusRequest? = null

    fun request(usage: CarAudioUsage, onChange: (Int) -> Unit): Int {
        abandon()
        val transient = usage != CarAudioUsage.MEDIA
        val request = AudioFocusRequest.Builder(
            if (transient) AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK else AudioManager.AUDIOFOCUS_GAIN
        )
            .setAudioAttributes(AudioAttributes.Builder().setUsage(usage.usage).build())
            .setOnAudioFocusChangeListener(onChange)
            .build()
        current = request
        return audioManager.requestAudioFocus(request)
    }

    fun abandon() {
        current?.let { audioManager.abandonAudioFocusRequest(it) }
        current = null
    }
}
