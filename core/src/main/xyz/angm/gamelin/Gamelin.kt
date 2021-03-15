/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/15/21, 4:25 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin

import com.badlogic.gdx.*
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.IntMap
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.Menu
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.file.FileChooser
import com.kotcrab.vis.ui.widget.file.FileTypeFilter
import com.kotcrab.vis.ui.widget.file.StreamingFileChooserListener
import ktx.assets.file
import ktx.collections.*
import xyz.angm.gamelin.system.GameBoy
import xyz.angm.gamelin.system.InstSet
import xyz.angm.gamelin.windows.*
import kotlin.system.exitProcess

/** The emulator. */
class Gamelin : ApplicationAdapter() {

    private lateinit var stage: Stage
    private val windows = HashMap<String, Window>()
    private val hotkeyHandler = HotkeyHandler()
    private lateinit var root: VisTable
    private lateinit var menuBar: MenuBar
    private lateinit var gb: GameBoy
    private lateinit var gbWindow: GameBoyWindow

    override fun create() {
        VisUI.load()
        gb = GameBoy()
        stage = Stage(com.badlogic.gdx.utils.viewport.ScreenViewport())
        root = VisTable()

        val multi = InputMultiplexer()
        multi.addProcessor(gb.keyboard)
        multi.addProcessor(stage)
        multi.addProcessor(hotkeyHandler)
        Gdx.input.inputProcessor = multi

        gbWindow = GameBoyWindow(gb)
        stage.addActor(gbWindow)
        stage.addActor(root)

        root.setFillParent(true)
        createMenus()
        root.add().expand().fill()
    }

    private fun createMenus() {
        menuBar = MenuBar()
        val file = Menu("File")
        val view = Menu("View")

        file.item("Load ROM", 1) {
            val chooser = FileChooser("Choose ROM", FileChooser.Mode.OPEN)
            chooser.selectionMode = FileChooser.SelectionMode.FILES
            chooser.setDirectory(file("roms"))
            val filter = FileTypeFilter(true)
            filter.addRule("GameBoy ROMs (.gb)", "gb")
            chooser.setFileTypeFilter(filter)
            chooser.setListener(object : StreamingFileChooserListener() {
                override fun selected(file: FileHandle) = gb.loadGame(file.readBytes())
            })
            stage.addActor(chooser)
            chooser.fadeIn()
        }
        file.item("Reset", 0) { gb.reset() }
        file.item("Exit", Input.Keys.F4 - Input.Keys.NUM_0) { Gdx.app.exit() }

        fun windowItem(name: String, shortcut: Int, create: () -> Window) {
            view.item(name, shortcut) { toggleWindow(name, create) }
        }

        windowItem("Debugger", 2) { DebuggerWindow(gb) }
        windowItem("BG Map Viewer", 3) { BGMapViewer(gb) }
        windowItem("VRAM Viewer", 4) { VRAMViewer(gb) }
        windowItem("Instruction Set", 5) { InstructionSetWindow("Instruction Set", InstSet.op) }
        windowItem("Extended InstSet", 6) { InstructionSetWindow("Extended InstSet", InstSet.ep) }

        menuBar.addMenu(file)
        menuBar.addMenu(view)
        root.add(menuBar.table).expandX().fillX().row()
    }

    private fun Menu.item(name: String, shortcut: Int, click: () -> Unit) {
        addItem(MenuItem(name, object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) = click()
        }).setShortcut(Input.Keys.NUM_0 + shortcut))
        hotkeyHandler.register(Input.Keys.NUM_0 + shortcut, click)
    }

    private fun toggleWindow(name: String, create: () -> Window) {
        val prev = windows[name]
        when {
            prev?.stage == stage -> prev.fadeOut()
            prev != null -> {
                stage.addActor(prev)
                prev.fadeIn()
            }
            else -> {
                val window = create()
                windows[window.name] = window
                toggleWindow(name, create)
            }
        }
    }

    override fun render() {
        val delta = Gdx.graphics.deltaTime
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        gb.advanceDelta(delta)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        gbWindow.centerWindow()
    }

    override fun dispose() {
        gb.dispose()
        stage.dispose()
        exitProcess(0)
    }

    class HotkeyHandler : InputAdapter() {

        private val actions = IntMap<() -> Unit>(10)

        fun register(key: Int, click: () -> Unit) {
            actions[key] = click
        }

        override fun keyDown(keycode: Int): Boolean {
            actions[keycode]?.invoke()
            return true
        }
    }
}
