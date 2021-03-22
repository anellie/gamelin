/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/22/21, 6:05 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system.io

import xyz.angm.gamelin.int
import xyz.angm.gamelin.interfaces.FileSystem
import xyz.angm.gamelin.isBit
import kotlin.math.max

/** A game that the system is playing.
 * TODO: Implement more MBCs.
 *
 * @property rom The raw ROM file of this game. */
abstract class Cartridge(private val rom: ByteArray) : IODevice() {

    internal val romBankCount = (2 shl rom[ROM_BANKS].int())
    internal val ramBankCount = when (rom[RAM_BANKS].int()) {
        0 -> 0
        2 -> 1
        3 -> 4
        4 -> 16
        5 -> 8
        else -> throw UnsupportedOperationException("Unknown cartridge controller")
    }
    internal val supportsCGB = read(CGB_FLAG).isBit(7)
    internal val requiresCGB = read(CGB_FLAG) == CGB_ONLY

    private val ram = loadRAM()
    protected var ramEnable = false
    private val ramAvailable get() = ram.isNotEmpty() && ramEnable

    protected var romBank = 1
    protected var ramBank = 0

    override fun read(addr: Int): Int {
        return when (addr) {
            in 0x0000..0x3FFF -> rom[addr]
            in 0x4000..0x7FFF -> rom[(addr and 0x3FFF) + (0x4000 * romBank)]
            in 0xA000..0xBFFF -> if (ramAvailable) ram[(addr and 0x1FFF) + (0x2000 * ramBank)] else MMU.INVALID_READ.toByte()
            else -> throw UnsupportedOperationException("Not cartridge")
        }.int()
    }

    override fun write(addr: Int, value: Int) {
        when (addr) {
            in 0xA000..0xBFFF -> if (ramAvailable) ram[(addr and 0x1FFF) + (0x2000 * ramBank)] = value
                .toByte()
        }
    }

    fun getTitle(extended: Boolean = false): String {
        val str = StringBuilder()
        for (byte in 0x134..if (extended) 0x0142 else 0x013E) { // Title in Game ROM
            val value = read(byte)
            if (value == 0x00) break
            str.append(value.toChar())
        }
        return str.toString()
    }

    /** Save the game RAM to disk, if applicable. */
    fun save() {
        if (ramBankCount > 0) FileSystem.saveRAM("${getTitle(extended = true)}${rom[DESTINATION]}", ram)
    }

    /** Tries to load the RAM from disk. */
    private fun loadRAM(): ByteArray {
        var ram: ByteArray? = null
        if (ramBankCount > 0) ram = FileSystem.loadRAM("${getTitle(extended = true)}${rom[DESTINATION]}")
        return ram ?: ByteArray(ramBankCount * 0x2000)
    }

    companion object {

        const val CGB_FLAG = 0x0143
        const val CGB_ONLY = 0xC0
        const val DMG_AND_CGB = 0x80

        const val KIND = 0x0147
        const val ROM_BANKS = 0x0148
        const val RAM_BANKS = 0x0149
        const val DESTINATION = 0x014A
        const val BANK_COUNT_1MB = 64

        fun ofRom(rom: ByteArray): Cartridge {
            return when (rom[KIND].int()) {
                in 0x01..0x03 -> MBC1(rom)
                in 0x0F..0x13 -> MBC3(rom)
                else -> NoMBC(rom)
            }
        }
    }
}

/** Cartridges with no MBC: 1 ROM bank, no RAM */
class NoMBC(rom: ByteArray) : Cartridge(rom)

/** Cartridges with the MBC1 controller. */
class MBC1(rom: ByteArray) : Cartridge(rom) {

    private var ramMode = false
    private var upperReg = 0

    override fun write(addr: Int, value: Int) {
        when (addr) {
            in 0x0000..0x1FFF -> ramEnable = (value and 0x0F) == 0x0A
            in 0x2000..0x3FFF -> romBank = max((value % romBankCount), 1) and 0x1F
            in 0x4000..0x5FFF -> {
                upperReg = value and 0x03
                updateReg()
            }
            in 0x6000..0x7FFF -> {
                ramMode = value.isBit(0)
                updateReg()
            }
            else -> super.write(addr, value)
        }
    }

    // TODO: Mode 1 (RAM mode) 0000-3FFF mapping
    private fun updateReg() {
        ramBank = if (ramBankCount == 4 && ramMode) upperReg else 0
        romBank = (romBank and 0x1F)
        if (romBankCount >= BANK_COUNT_1MB && !ramMode) romBank += upperReg shl 5
    }
}

/** Cartridges with the MBC3 controller. */
class MBC3(rom: ByteArray) : Cartridge(rom) {

    override fun write(addr: Int, value: Int) {
        when (addr) {
            in 0x0000..0x1FFF -> ramEnable = (value and 0x0F) == 0x0A
            in 0x2000..0x3FFF -> romBank = max((value % romBankCount), 1)
            in 0x4000..0x5FFF -> ramBank = value and 0x03
            else -> super.write(addr, value)
        }
    }
}