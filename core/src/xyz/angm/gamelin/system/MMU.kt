/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/13/21, 9:12 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import xyz.angm.gamelin.hex16
import xyz.angm.gamelin.hex8
import xyz.angm.gamelin.int

private const val INVALID_READ = 0xFF.toByte()
private val bios = arrayOf(
    0x31, 0xFE, 0xFF, 0xAF, 0x21, 0xFF, 0x9F, 0x32, 0xCB, 0x7C, 0x20, 0xFB, 0x21, 0x26, 0xFF, 0x0E,
    0x11, 0x3E, 0x80, 0x32, 0xE2, 0x0C, 0x3E, 0xF3, 0xE2, 0x32, 0x3E, 0x77, 0x77, 0x3E, 0xFC, 0xE0,
    0x47, 0x11, 0x04, 0x01, 0x21, 0x10, 0x80, 0x1A, 0xCD, 0x95, 0x00, 0xCD, 0x96, 0x00, 0x13, 0x7B,
    0xFE, 0x34, 0x20, 0xF3, 0x11, 0xD8, 0x00, 0x06, 0x08, 0x1A, 0x13, 0x22, 0x23, 0x05, 0x20, 0xF9,
    0x3E, 0x19, 0xEA, 0x10, 0x99, 0x21, 0x2F, 0x99, 0x0E, 0x0C, 0x3D, 0x28, 0x08, 0x32, 0x0D, 0x20,
    0xF9, 0x2E, 0x0F, 0x18, 0xF3, 0x67, 0x3E, 0x64, 0x57, 0xE0, 0x42, 0x3E, 0x91, 0xE0, 0x40, 0x04,
    0x1E, 0x02, 0x0E, 0x0C, 0xF0, 0x44, 0xFE, 0x90, 0x20, 0xFA, 0x0D, 0x20, 0xF7, 0x1D, 0x20, 0xF2,
    0x0E, 0x13, 0x24, 0x7C, 0x1E, 0x83, 0xFE, 0x62, 0x28, 0x06, 0x1E, 0xC1, 0xFE, 0x64, 0x20, 0x06,
    0x7B, 0xE2, 0x0C, 0x3E, 0x87, 0xF2, 0xF0, 0x42, 0x90, 0xE0, 0x42, 0x15, 0x20, 0xD2, 0x05, 0x20,
    0x4F, 0x16, 0x20, 0x18, 0xCB, 0x4F, 0x06, 0x04, 0xC5, 0xCB, 0x11, 0x17, 0xC1, 0xCB, 0x11, 0x17,
    0x05, 0x20, 0xF5, 0x22, 0x23, 0x22, 0x23, 0xC9, 0xCE, 0xED, 0x66, 0x66, 0xCC, 0x0D, 0x00, 0x0B,
    0x03, 0x73, 0x00, 0x83, 0x00, 0x0C, 0x00, 0x0D, 0x00, 0x08, 0x11, 0x1F, 0x88, 0x89, 0x00, 0x0E,
    0xDC, 0xCC, 0x6E, 0xE6, 0xDD, 0xDD, 0xD9, 0x99, 0xBB, 0xBB, 0x67, 0x63, 0x6E, 0x0E, 0xEC, 0xCC,
    0xDD, 0xDC, 0x99, 0x9F, 0xBB, 0xB9, 0x33, 0x3E, 0x3c, 0x42, 0xB9, 0xA5, 0xB9, 0xA5, 0x42, 0x4C,
    0x21, 0x04, 0x01, 0x11, 0xA8, 0x00, 0x1A, 0x13, 0xBE, 0x20, 0xFE, 0x23, 0x7D, 0xFE, 0x34, 0x20,
    0xF5, 0x06, 0x19, 0x78, 0x86, 0x23, 0x05, 0x20, 0xFB, 0x86, 0x20, 0xFE, 0x3E, 0x01, 0xE0, 0x50
).map { it.toByte() }

class MMU(private val gb: GameBoy, private val rom: ByteArray) {

    // ROM: 0000-7FFF TODO bank switching
    private val vram = ByteArray(8_192) // 8000-9FFF
    private val extRam = ByteArray(8_192) // A000-BFFF
    private val ram = ByteArray(8_192) // C000-DFFF
    private val spriteRam = ByteArray(160) // FE00-FE9F
    private val mmIO = ByteArray(128) // FF00-FF7F
    private val zram = ByteArray(128) // FF80-FFFF
    private var inBios = false

    internal fun read(addr: Short): Byte {
        return when (val a = addr.int()) {
            // Cannot read from:
            // FF47: PPU Background Palette
            0xFF47 -> {
                GameBoy.log.info { "Attempted to read write-only memory at ${a.hex16()}, giving ${INVALID_READ.hex8()}. (PC: ${gb.cpu.pc.hex16()})" }
                INVALID_READ
            }

            else -> readAny(addr)
        }
    }

    internal fun write(addr: Short, value: Byte) {
        // 0x01FE is a special register written to by the BIOS to remove
        // the BIOS from memory
        if (addr.int() == 0x01FE && inBios) {
            inBios = false
            return
        }

        when (val a = addr.int()) {
            // Cannot write to:
            // 0000-7FFF: *RO*M
            // FF44: Current PPU scan line
            in 0x0000..0x7FFF, 0xFF44 -> GameBoy.log.info { "Attempted to write ${value.hex8()} to read-only memory location ${a.hex16()}, ignored. (PC: ${gb.cpu.pc.hex16()})" }
            else -> writeAny(addr, value)
        }
    }

    internal fun readAny(addr: Short): Byte {
        return when (val a = addr.int()) {
            in 0x0000..0x7FFF -> {
                if (inBios && addr < 0x0100) bios[a]
                else rom[a]
            }
            in 0x8000..0x9FFF -> vram[a and 0x1FFF]
            in 0xA000..0xBFFF -> extRam[a and 0x1FFF]
            in 0xC000..0xDFFF -> ram[a and 0x1FFF]
            in 0xE000..0xFDFF -> ram[a and 0x1FFF]
            in 0xFE00..0xFE9F -> spriteRam[a and 0xFF]
            in 0xFEA0..0xFEFF -> 0
            in 0xFF00..0xFF7F -> mmIO[a and 0x7F]
            in 0xFF80..0xFFFF -> zram[a and 0x7F]
            else -> throw IndexOutOfBoundsException("what ${a.toString(16)}")
        }
    }

    internal fun writeAny(addr: Short, value: Byte) {
        when (val a = addr.int()) {
            in 0x0000..0x7FFF -> rom[a] = value
            in 0x8000..0x9FFF -> vram[a and 0x1FFF] = value
            in 0xA000..0xBFFF -> extRam[a and 0x1FFF] = value
            in 0xC000..0xDFFF -> ram[a and 0x1FFF] = value
            in 0xE000..0xFDFF -> ram[a and 0x1FFF] = value
            in 0xFE00..0xFE9F -> spriteRam[a and 0xFF] = value
            in 0xFEA0..0xFEFF -> Unit
            in 0xFF00..0xFF7F -> mmIO[a and 0x7F] = value
            in 0xFF80..0xFFFF -> zram[a and 0x7F] = value
            else -> throw IndexOutOfBoundsException("what ${a.toString(16)}")
        }
    }
}