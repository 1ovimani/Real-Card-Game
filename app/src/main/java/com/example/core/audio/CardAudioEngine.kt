package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.core.model.AppSettings
import com.example.core.model.MusicMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class CardSfx {
    CARD_DEAL,
    CARD_PLACE,
    CARD_FLIP,
    CARD_SHUFFLE,
    CARD_COLLECT,
    UI_CLICK,
    BID_ANNOUNCE,
    TRUMP_REVEAL,
    TRICK_WIN,
    ROUND_WIN,
    MATCH_WIN,
    MATCH_LOSS,
    TURN_CHIME
}

/**
 * Self-contained, 100% offline original studio audio synthesizer & haptic engine.
 * Synthesizes crisp PCM WAV buffers for playing-card acoustics and harmonic cues
 * into Android SoundPool, plus a warm ambient background music loop via AudioTrack.
 */
class CardAudioEngine(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<CardSfx, Int>()
    @Volatile
    private var currentSettings: AppSettings = AppSettings()
    @Volatile
    private var initialized = false

    private var musicJob: Job? = null
    private var activeMood: MusicMood? = null

    fun initialize() {
        if (initialized) return
        initialized = true
        scope.launch {
            try {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val pool = SoundPool.Builder()
                    .setMaxStreams(8)
                    .setAudioAttributes(attrs)
                    .build()
                soundPool = pool

                val cacheDir = File(context.cacheDir, "wcm_sfx").apply { mkdirs() }
                loadSynthSound(pool, cacheDir, CardSfx.CARD_DEAL) { synthCardSnap(durationMs = 75, crispness = 0.85f) }
                loadSynthSound(pool, cacheDir, CardSfx.CARD_PLACE) { synthCardPlace() }
                loadSynthSound(pool, cacheDir, CardSfx.CARD_FLIP) { synthCardSnap(durationMs = 95, crispness = 0.65f) }
                loadSynthSound(pool, cacheDir, CardSfx.CARD_SHUFFLE) { synthShuffleRiffle() }
                loadSynthSound(pool, cacheDir, CardSfx.CARD_COLLECT) { synthCardCollect() }
                loadSynthSound(pool, cacheDir, CardSfx.UI_CLICK) { synthWoodClick() }
                loadSynthSound(pool, cacheDir, CardSfx.BID_ANNOUNCE) { synthChord(doubleArrayOf(392.0, 493.88, 587.33), 210) }
                loadSynthSound(pool, cacheDir, CardSfx.TRUMP_REVEAL) { synthArpeggio(doubleArrayOf(329.63, 440.0, 554.37, 659.25), 420) }
                loadSynthSound(pool, cacheDir, CardSfx.TRICK_WIN) { synthArpeggio(doubleArrayOf(523.25, 659.25), 220) }
                loadSynthSound(pool, cacheDir, CardSfx.ROUND_WIN) { synthArpeggio(doubleArrayOf(440.0, 554.37, 659.25, 880.0), 480) }
                loadSynthSound(pool, cacheDir, CardSfx.MATCH_WIN) { synthArpeggio(doubleArrayOf(523.25, 659.25, 783.99, 1046.5), 620) }
                loadSynthSound(pool, cacheDir, CardSfx.MATCH_LOSS) { synthArpeggio(doubleArrayOf(392.0, 349.23, 293.66), 440) }
                loadSynthSound(pool, cacheDir, CardSfx.TURN_CHIME) { synthChord(doubleArrayOf(659.25, 987.77), 150) }
            } catch (_: Throwable) {
                // Gracefully degrade on headless/test devices without audio hardware
            }
        }
    }

    fun updateSettings(settings: AppSettings) {
        val prevMusicEnabled = currentSettings.musicEnabled && !currentSettings.muteAll
        val prevMood = currentSettings.musicMood
        currentSettings = settings
        val shouldPlayMusic = settings.musicEnabled && !settings.muteAll && settings.masterVolume > 0.01f && settings.musicVolume > 0.01f
        if (shouldPlayMusic && (!prevMusicEnabled || prevMood != settings.musicMood || musicJob == null)) {
            startAmbientMusic(settings.musicMood)
        } else if (!shouldPlayMusic) {
            stopAmbientMusic()
        }
    }

    fun play(sfx: CardSfx) {
        val s = currentSettings
        if (s.muteAll) return
        val isCardSound = sfx in setOf(
            CardSfx.CARD_DEAL, CardSfx.CARD_PLACE, CardSfx.CARD_FLIP,
            CardSfx.CARD_SHUFFLE, CardSfx.CARD_COLLECT
        )
        if (isCardSound && !s.cardSoundsEnabled) return

        val vol = (s.masterVolume * s.sfxVolume).coerceIn(0f, 1f)
        if (vol <= 0.01f) return

        val id = soundIds[sfx] ?: return
        try {
            soundPool?.play(id, vol, vol, 1, 0, 1.0f)
        } catch (_: Throwable) {}

        if (s.vibrationEnabled && (sfx == CardSfx.CARD_PLACE || sfx == CardSfx.TRUMP_REVEAL || sfx == CardSfx.MATCH_WIN)) {
            triggerHaptic(if (sfx == CardSfx.MATCH_WIN) 45L else 18L)
        }
    }

    fun triggerHaptic(durationMs: Long = 16L) {
        if (!currentSettings.vibrationEnabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Throwable) {}
    }

    private fun startAmbientMusic(mood: MusicMood) {
        stopAmbientMusic()
        activeMood = mood
        musicJob = scope.launch {
            val sampleRate = 22050
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            var track: AudioTrack? = null
            try {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val format = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                track.play()

                val chords = when (mood) {
                    MusicMood.CASINO_AMBIENCE -> arrayOf(
                        doubleArrayOf(130.81, 164.81, 196.00, 246.94), // Cmaj7
                        doubleArrayOf(110.00, 130.81, 164.81, 196.00), // Am7
                        doubleArrayOf(146.83, 174.61, 220.00, 261.63), // Dm7
                        doubleArrayOf(123.47, 146.83, 196.00, 246.94)  // G6
                    )
                    MusicMood.CLASSIC_CARD_ROOM -> arrayOf(
                        doubleArrayOf(146.83, 185.00, 220.00, 277.18), // Dmaj7
                        doubleArrayOf(123.47, 146.83, 185.00, 220.00), // Bm7
                        doubleArrayOf(164.81, 196.00, 246.94, 293.66), // Em7
                        doubleArrayOf(110.00, 164.81, 220.00, 261.63)  // Asus
                    )
                    MusicMood.TRADITIONAL_SITAR -> arrayOf(
                        // Warm Tanpura-inspired Sa-Pa-Sa pentatonic drone
                        doubleArrayOf(130.81, 196.00, 261.63, 329.63),
                        doubleArrayOf(130.81, 196.00, 293.66, 392.00),
                        doubleArrayOf(130.81, 196.00, 261.63, 349.23),
                        doubleArrayOf(130.81, 196.00, 261.63, 329.63)
                    )
                    MusicMood.MINIMAL_AMBIENT -> arrayOf(
                        doubleArrayOf(174.61, 220.00, 261.63),
                        doubleArrayOf(146.83, 196.00, 261.63)
                    )
                }

                val chunkSamples = 2048
                val buf = ShortArray(chunkSamples)
                var sampleIndex = 0L
                val chordLenSamples = sampleRate * 4 // 4 seconds per warm chord swell

                while (isActive) {
                    val s = currentSettings
                    if (s.muteAll || !s.musicEnabled) break
                    val gain = (s.masterVolume * s.musicVolume * 0.16f).coerceIn(0f, 0.28f)
                    for (i in 0 until chunkSamples) {
                        val t = sampleIndex.toDouble() / sampleRate
                        val chordIdx = ((sampleIndex / chordLenSamples) % chords.size).toInt()
                        val posInChord = (sampleIndex % chordLenSamples).toDouble() / chordLenSamples
                        val envelope = sin(PI * posInChord).coerceIn(0.18, 1.0)
                        val notes = chords[chordIdx]
                        var v = 0.0
                        for (freq in notes) {
                            v += sin(2.0 * PI * freq * t) * 0.7 + sin(4.0 * PI * freq * t) * 0.15
                        }
                        v = (v / notes.size) * envelope * gain
                        buf[i] = (v * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        sampleIndex++
                    }
                    track.write(buf, 0, chunkSamples)
                }
            } catch (_: Throwable) {
                // Safe exit on unsupported audio environments
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {}
            }
        }
    }

    fun stopAmbientMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    fun release() {
        stopAmbientMusic()
        try {
            soundPool?.release()
        } catch (_: Throwable) {}
        soundPool = null
        scope.cancel()
    }

    private fun loadSynthSound(pool: SoundPool, dir: File, sfx: CardSfx, generator: () -> ShortArray) {
        val file = File(dir, "${sfx.name.lowercase()}.wav")
        if (!file.exists()) {
            writeWav(file, generator(), 22050)
        }
        val id = pool.load(file.absolutePath, 1)
        soundIds[sfx] = id
    }

    private fun synthCardSnap(durationMs: Int, crispness: Float): ShortArray {
        val sr = 22050
        val count = (sr * durationMs) / 1000
        val out = ShortArray(count)
        val rng = Random(durationMs)
        var prev = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val env = exp(-t * 9.0) * sin(PI * t.coerceAtMost(0.15) / 0.15)
            val noise = rng.nextDouble(-1.0, 1.0)
            // High-pass blend for realistic linen cardstock flick
            val filtered = noise - prev * (1.0 - crispness)
            prev = noise
            val body = sin(2.0 * PI * (380.0 - 140.0 * t) * (i.toDouble() / sr)) * 0.25
            val sample = (filtered * 0.65 + body) * env * 0.72
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthCardPlace(): ShortArray {
        val sr = 22050
        val count = (sr * 90) / 1000
        val out = ShortArray(count)
        val rng = Random(42)
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val feltThud = sin(2.0 * PI * (150.0 - 70.0 * t) * (i.toDouble() / sr)) * exp(-t * 10.0) * 0.65
            val paperBrush = rng.nextDouble(-1.0, 1.0) * exp(-t * 14.0) * 0.45
            val sample = (feltThud + paperBrush) * 0.85
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthShuffleRiffle(): ShortArray {
        val sr = 22050
        val count = (sr * 420) / 1000
        val out = ShortArray(count)
        val rng = Random(108)
        val taps = 12
        val spacing = count / (taps + 2)
        for (tap in 0 until taps) {
            val start = tap * spacing + rng.nextInt(0, (spacing / 3).coerceAtLeast(1))
            val len = (sr * 24) / 1000
            for (j in 0 until len) {
                val idx = start + j
                if (idx >= count) break
                val t = j.toDouble() / len
                val env = exp(-t * 8.5)
                val v = rng.nextDouble(-1.0, 1.0) * env * 0.52
                val sum = out[idx] + (v * Short.MAX_VALUE).toInt()
                out[idx] = sum.coerceIn(-32767, 32767).toShort()
            }
        }
        return out
    }

    private fun synthCardCollect(): ShortArray {
        val sr = 22050
        val count = (sr * 170) / 1000
        val out = ShortArray(count)
        val rng = Random(77)
        var lp = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val env = sin(PI * t) * exp(-t * 2.2)
            val noise = rng.nextDouble(-1.0, 1.0)
            lp = lp * 0.7 + noise * 0.3
            out[i] = (lp * env * 0.65 * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthWoodClick(): ShortArray {
        val sr = 22050
        val count = (sr * 45) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val env = exp(-t * 16.0)
            val v = (sin(2.0 * PI * 760.0 * i / sr) * 0.6 + sin(2.0 * PI * 1420.0 * i / sr) * 0.3) * env * 0.55
            out[i] = (v * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthChord(freqs: DoubleArray, durationMs: Int): ShortArray {
        val sr = 22050
        val count = (sr * durationMs) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val env = exp(-t * 4.5) * (1.0 - exp(-t * 60.0))
            var v = 0.0
            for (f in freqs) {
                v += sin(2.0 * PI * f * i / sr)
            }
            v = (v / freqs.size) * env * 0.55
            out[i] = (v * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthArpeggio(freqs: DoubleArray, durationMs: Int): ShortArray {
        val sr = 22050
        val count = (sr * durationMs) / 1000
        val out = ShortArray(count)
        val step = count / freqs.size.coerceAtLeast(1)
        for (n in freqs.indices) {
            val start = n * (step / 2)
            val f = freqs[n]
            for (i in start until count) {
                val localT = (i - start).toDouble() / (count - start).coerceAtLeast(1)
                val env = exp(-localT * 5.2) * (1.0 - exp(-localT * 75.0))
                val v = (sin(2.0 * PI * f * i / sr) * 0.75 + sin(4.0 * PI * f * i / sr) * 0.2) * env * 0.38
                val sum = out[i] + (v * Short.MAX_VALUE).toInt()
                out[i] = sum.coerceIn(-32767, 32767).toShort()
            }
        }
        return out
    }

    private fun writeWav(file: File, pcm: ShortArray, sampleRate: Int) {
        val byteLen = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + byteLen)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // PCM chunk size
            putShort(1) // PCM format
            putShort(1) // Mono
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(byteLen)
        }
        val data = ByteBuffer.allocate(byteLen).order(ByteOrder.LITTLE_ENDIAN)
        for (s in pcm) data.putShort(s)
        FileOutputStream(file).use { fos ->
            fos.write(header.array())
            fos.write(data.array())
        }
    }
}
