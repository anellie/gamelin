/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/21/21, 3:18 AM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

import com.soywiz.klock.milliseconds
import com.soywiz.korge.Korge
import com.soywiz.korge.time.delay
import com.soywiz.korge.view.Views
import com.soywiz.korim.color.Colors
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.ImageData
import org.w3c.files.FileReader
import xyz.angm.gamelin.saveConfiguration
import xyz.angm.gamelin.stringToBytes
import xyz.angm.gamelin.system.GameBoy

lateinit var view: Views
lateinit var screen: HTMLCanvasElement
lateinit var screenCtx: CanvasRenderingContext2D
lateinit var screenData: ImageData

suspend fun main() = Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"]) {
    view = views
    screen = document.getElementById("screen") as HTMLCanvasElement
    screenCtx = screen.getContext("2d") as CanvasRenderingContext2D
    screenData = screenCtx.createImageData(160.0, 144.0)

    val gb = GameBoy()

    val fileElem = document.getElementById("game") as HTMLInputElement
    fileElem.addEventListener("change", {
        val file = fileElem.files!!.item(0)!!
        document.getElementById("filetext")!!.innerHTML = file.name
        val reader = FileReader()
        reader.addEventListener("loadend", {
            val res = reader.result as String
            gb.loadGame(stringToBytes(res))
        })
        reader.readAsBinaryString(file)
    })
    window.onunload = {
        gb.mmu.cart.save()
        saveConfiguration()
    }

    while (!gb.gameLoaded) delay(100.milliseconds)
    window.setInterval({
        gb.advanceDelta(1 / 30f)
    }, 33)
}
