/*
 * Developed as part of the Gamelin project.
 * This file was last modified at 3/25/21, 6:43 PM.
 * Copyright 2021, see git repository at git.angm.xyz for authors and other info.
 * This file is under the GPL3 license. See LICENSE in the root directory of this repository for details.
 */

package xyz.angm.xyz.gamelin.tests

import io.kotest.core.config.AbstractProjectConfig

object ProjectConfig : AbstractProjectConfig() {
    override val parallelism = 10
    const val TEST_ALL = false
}