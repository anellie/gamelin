/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:56 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import com.badlogic.gdx.Gdx
import xyz.angm.gamelin.system.CLOCK_SPEED_HZ

actual class AudioOutput actual constructor() {

    private var device = Gdx.audio.newAudioDevice(SAMPLE_RATE, false)
    private var counter = 0
    private var bufferIndex = 0
    private var buffer = ShortArray(BUFFER_SIZE)

    @Volatile
    private var play = false
        @Synchronized set

    init {
        Thread {
            while (true) {
                if (play) {
                    device.writeSamples(buffer, 0, BUFFER_SIZE)
                    play = false
                }
            }
        }.start()
    }

    actual fun reset() {
        counter = 0
        bufferIndex = 0
        buffer.fill(0)
    }

    actual fun play(left: Byte, right: Byte) {
        buffer[bufferIndex++] = (left * 32).toShort()
        buffer[bufferIndex++] = (right * 32).toShort()
        if (bufferIndex >= BUFFER_SIZE) {
            play = true
            bufferIndex = 0
        }
    }

    actual fun needsSamples() = !play

    actual fun dispose() = device.dispose()

    actual companion object {
        actual val SAMPLE_RATE get() = 22050
        actual val BUFFER_SIZE get() = 1024
        actual val DIVIDER get() = CLOCK_SPEED_HZ / SAMPLE_RATE
    }
}