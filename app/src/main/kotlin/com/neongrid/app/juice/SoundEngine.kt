package com.neongrid.app.juice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Zero-asset placeholder SFX: short synthesized tones played via AudioTrack.
 * M4 replaces this with SoundPool + real designed synthwave samples; the
 * public API (place/clear/combo pitch escalation) stays the same.
 */
class SoundEngine {

    var enabled = true
    private val sampleRate = 44100

    /** Soft click on placement. */
    fun place() = play(freq = 220f, durMs = 45, volume = 0.25f, kind = Kind.PLUCK)

    /** Quick tick on piece rotation. */
    fun rotate() = play(freq = 440f, durMs = 35, volume = 0.15f, kind = Kind.PLUCK)

    /**
     * Catchy clear: a bright rising arpeggio in key with the music (A minor),
     * warm triangle pluck + octave shimmer. Higher combos start the riff
     * higher up the scale — a melodic ladder that rewards chaining.
     */
    fun clear(lines: Int, combo: Int) {
        val scale = intArrayOf(0, 3, 7, 12, 15, 19, 24, 27, 31) // Am arpeggio degrees
        val start = (combo - 1).coerceIn(0, 3)
        val notes = 4 + min(lines, 2)
        for (k in 0 until notes) {
            val deg = scale[min(start + k, scale.size - 1)]
            val f = 440f * 2f.pow(deg / 12f)
            play(freq = f, durMs = 220, volume = 0.28f, kind = Kind.CHIME, delayMs = 50L * k)
        }
        play(freq = 110f, durMs = 180, volume = 0.35f, kind = Kind.BOOM)
        if (lines >= 3) {
            play(freq = 880f, durMs = 400, volume = 0.18f, kind = Kind.SWEEP, delayMs = 50L * notes)
        }
    }

    fun comboBreak() = play(freq = 260f, durMs = 200, volume = 0.3f, kind = Kind.FALL)

    fun allClear() = play(freq = 523f, durMs = 350, volume = 0.45f, kind = Kind.SWEEP)

    fun gameOver() = play(freq = 160f, durMs = 500, volume = 0.45f, kind = Kind.FALL)

    private enum class Kind { PLUCK, SWEEP, BOOM, FALL, STAB, CRUNCH, CHIME }

    private fun play(freq: Float, durMs: Int, volume: Float, kind: Kind, delayMs: Long = 0) {
        if (!enabled) return
        thread(isDaemon = true) {
            runCatching {
                if (delayMs > 0) Thread.sleep(delayMs)
                val samples = sampleRate * durMs / 1000
                val buf = ShortArray(samples)
                val rng = java.util.Random()
                for (i in 0 until samples) {
                    val t = i.toFloat() / sampleRate
                    val progress = i.toFloat() / samples
                    val f = when (kind) {
                        Kind.PLUCK -> freq
                        Kind.SWEEP -> freq * (1f + 0.6f * progress)
                        Kind.BOOM -> freq * (1f - 0.4f * progress)
                        Kind.FALL -> freq * (1f - 0.5f * progress)
                        Kind.STAB -> freq * (1f + 0.15f * progress)
                        Kind.CRUNCH -> freq
                        Kind.CHIME -> freq
                    }
                    val envelope = when (kind) {
                        Kind.PLUCK -> (1f - progress).pow(2)
                        Kind.SWEEP -> sin(progress * PI).toFloat()
                        Kind.BOOM -> (1f - progress).pow(1.5f)
                        Kind.FALL -> (1f - progress)
                        Kind.STAB -> (1f - progress).pow(3)
                        Kind.CRUNCH -> (1f - progress).pow(2)
                        Kind.CHIME -> (1f - progress).pow(2)
                    }
                    val v = when (kind) {
                        // Sawtooth stab — harsh, metallic attack.
                        Kind.STAB -> {
                            val phase = (f * t) % 1.0
                            (2.0 * phase - 1.0) * envelope * volume
                        }
                        // White-noise crunch — the impact texture.
                        Kind.CRUNCH -> (rng.nextDouble() * 2 - 1) * envelope * volume
                        // Warm triangle body + sine octave shimmer.
                        Kind.CHIME -> {
                            val tri = 2.0 / PI * kotlin.math.asin(sin(2.0 * PI * f * t))
                            (tri + sin(2.0 * PI * f * 2 * t) * 0.3) * envelope * volume
                        }
                        else -> sin(2.0 * PI * f * t) * envelope * volume
                    }
                    buf[i] = (v * Short.MAX_VALUE).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(buf.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buf, 0, buf.size)
                track.play()
                Thread.sleep(durMs.toLong() + 60)
                track.release()
            }
        }
    }
}
