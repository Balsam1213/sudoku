package com.balsam.sudoku.ui.common

import android.media.AudioManager
import android.media.ToneGenerator

/** 基于 ToneGenerator 的轻量提示音，无需音频资源文件。 */
class SoundPlayer {

    private var tone: ToneGenerator? = create()

    private fun create(): ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
    } catch (_: RuntimeException) {
        null
    }

    /** 选择方格 / 切换数字的轻提示音。 */
    fun select() = play(ToneGenerator.TONE_PROP_BEEP, 40)

    /** 填入正确数字的确认音。 */
    fun correct() = play(ToneGenerator.TONE_PROP_ACK, 90)

    /** 成就解锁的提示音。 */
    fun achievement() = play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)

    fun error() = play(ToneGenerator.TONE_PROP_NACK, 120)

    fun success() = play(ToneGenerator.TONE_CDMA_CONFIRM, 300)

    private fun play(toneType: Int, durationMs: Int) {
        val current = tone ?: create()?.also { tone = it } ?: return
        try {
            current.startTone(toneType, durationMs)
        } catch (_: RuntimeException) {
        }
    }

    fun release() {
        try {
            tone?.release()
        } catch (_: RuntimeException) {
        }
        tone = null
    }

    private companion object {
        const val VOLUME = 55
    }
}
