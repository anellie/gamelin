/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/12/21, 2:03 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.gamelin.system

import kotlin.experimental.and

internal class Cpu(private val gb: GameBoy) {

    var pc: Short = 0
    var sp: Short = 0
    val regs = Array<Byte>(Reg.values().size) { 0 }

    fun nextInstruction(): Inst {
        val inst = InstSet.instOf(gb.read(pc), gb.read(pc + 1))
        inst.execute(gb)
        if (inst.incPC) pc = (pc + inst.size).toShort()
        return inst
    }

    fun jmpRelative(by: Int) {
        pc = (pc + by).toShort()
    }

    fun jmpAbsolute(to: Int) {
        pc = to.toShort()
    }

    fun brRelative(cond: Boolean, by: Int): Boolean {
        if (cond) jmpRelative(by)
        return cond
    }

    fun brAbsolute(cond: Boolean, to: Int): Boolean {
        if (cond) jmpAbsolute(to)
        return cond
    }

    fun flag(flag: Flag) = flagVal(flag) == 1
    fun flagVal(flag: Flag) = ((regs[Reg.F.idx].toInt() ushr flag.position) and 1)

    fun flag(flag: Flag, value: Int) {
        regs[Reg.F.idx] = ((regs[Reg.F.idx] and flag.invMask.toByte()) + flag.from(value)).toByte()
    }
}

internal enum class Reg(val idx: Int) {
    A(0),
    B(1),
    C(2),
    D(3),
    E(4),
    F(5),
    H(6),
    L(7),
}

internal enum class DReg(val low: Reg, val high: Reg) {
    BC(Reg.B, Reg.C),
    DE(Reg.D, Reg.E),
    HL(Reg.H, Reg.L),
}

internal enum class Flag(val position: Int) {
    Zero(7),
    Negative(6),
    HalfCarry(5),
    Carry(4);

    val mask get() = 1 shl position
    val invMask get() = (1 shl position) xor 0xFF

    fun get(reg: Int) = (reg and mask) shr position
    fun isSet(reg: Int) = ((reg and mask) shr position) != 0
    fun from(value: Int) = (if (value != 0) 1 else 0) shl position
}