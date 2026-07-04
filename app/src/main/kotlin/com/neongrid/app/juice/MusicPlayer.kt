package com.neongrid.app.juice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Procedural synthwave background music: one Am-F-C-G cycle at 110 BPM
 * (bass, pad, arpeggio with echo, kick/snare/hat), synthesized once into a
 * static AudioTrack and looped gaplessly. Zero assets, zero licensing.
 * Mirrors the DSP in web-demo/index.html.
 */
class MusicPlayer {

    private val sampleRate = 44100
    @Volatile private var track: AudioTrack? = null
    @Volatile private var wantPlaying = false

    var enabled = true
        set(value) {
            field = value
            if (value && wantPlaying) play() else pauseInternal()
        }

    init {
        thread(isDaemon = true, name = "music-synth") {
            runCatching {
                val pcm = render()
                val t = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                t.write(pcm, 0, pcm.size)
                t.setLoopPoints(0, pcm.size, -1)
                t.setVolume(0.5f)
                track = t
                if (wantPlaying && enabled) t.play()
            }
        }
    }

    /** Call from ON_START. */
    fun play() {
        wantPlaying = true
        if (!enabled) return
        runCatching {
            val t = track ?: return
            if (t.playState != AudioTrack.PLAYSTATE_PLAYING) t.play()
        }
    }

    /** Call from ON_STOP. */
    fun pause() {
        wantPlaying = false
        pauseInternal()
    }

    private fun pauseInternal() {
        runCatching { track?.pause() }
    }

    private fun render(): ShortArray {
        val bpm = 110.0
        val beat = 60.0 / bpm
        val bar = beat * 4
        val total = bar * 4
        val n = floor(total * sampleRate).toInt()
        val out = ShortArray(n)
        val roots = doubleArrayOf(55.0, 43.65, 65.41, 49.0) // A1 F1 C2 G1
        val triads = arrayOf(intArrayOf(0, 3, 7), intArrayOf(0, 4, 7), intArrayOf(0, 4, 7), intArrayOf(0, 4, 7))
        fun st(s: Int) = 2.0.pow(s / 12.0)
        val rng = java.util.Random(42) // deterministic noise for snare/hat

        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val barIdx = (floor(t / bar).toInt()) % 4
            val tb = t % bar
            val root = roots[barIdx]
            val tri = triads[barIdx]
            var v = 0.0

            // BASS: 8th-note pulses, square + sub sine
            val eighth = tb % (beat / 2)
            val eIdx = floor(tb / (beat / 2)).toInt()
            val bEnv = exp(-eighth * 9) * 0.8
            val bFreq = root * (if (eIdx % 4 == 3) st(tri[2]) else 1.0)
            v += (sign(sin(2 * PI * bFreq * 2 * t)) * 0.35 + sin(2 * PI * bFreq * t)) * bEnv * 0.16

            // PAD: sustained triad, triangle, swell per bar
            val pEnv = min(tb / 0.5, 1.0) * min((bar - tb) / 0.3, 1.0)
            for (s in tri) {
                val f = root * 4 * st(s)
                v += asin(sin(2 * PI * f * t)) * (2 / PI) * pEnv * 0.035
            }

            // ARP: 16ths over chord tones two octaves up, with echo ghost
            val six = beat / 4
            val aIdx = floor(tb / six).toInt()
            val aT = tb % six
            val seq = intArrayOf(0, tri[1], tri[2], 12)
            val aEnv = exp(-aT * 14)
            v += sin(2 * PI * root * 8 * st(seq[aIdx % 4]) * t) * aEnv * 0.10
            val aIdx2 = (aIdx + 13) % 16
            v += sin(2 * PI * root * 8 * st(seq[aIdx2 % 4]) * t) * aEnv * 0.045

            // KICK: every beat, pitch drop
            val kT = tb % beat
            v += sin(2 * PI * (40 + 90 * exp(-kT * 28)) * kT) * exp(-kT * 14) * 0.5

            // SNARE: beats 2 & 4
            val beatIdx = floor(tb / beat).toInt()
            val sT = tb - beatIdx * beat
            if (beatIdx % 2 == 1) {
                v += ((rng.nextDouble() * 2 - 1) * 0.5 + sin(2 * PI * 190 * sT) * 0.3) * exp(-sT * 22) * 0.30
            }

            // HAT: offbeat 8ths
            val hT = (tb + beat / 4) % (beat / 2)
            v += (rng.nextDouble() * 2 - 1) * exp(-hT * 90) * 0.12

            out[i] = (v.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.9).toInt().toShort()
        }
        return out
    }
}
