/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/18/21, 9:47 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.interfaces

import org.khronos.webgl.Uint16Array
import org.khronos.webgl.set
import screenCtx
import screenData
import xyz.angm.gamelin.system.io.MMU

internal actual class TileRenderer actual constructor(mmu: MMU, width: Int, height: Int, scale: Float) {

    actual fun drawPixel(x: Int, y: Int, colorIdx: Int) {
        var idx = (x + (y * 160)) * 4
        val color = colors[colorIdx]
        val dat = screenData.data.unsafeCast<Uint16Array>()
        dat[idx++] = color
        dat[idx++] = color
        dat[idx++] = color
        dat[idx] = 255
    }

    actual fun finishFrame() {
        screenCtx.putImageData(screenData, 0.0, 0.0)
    }

    actual fun dispose() {
    }

    companion object {
        private val colors = shortArrayOf(255, 178, 90, 0)
    }
}