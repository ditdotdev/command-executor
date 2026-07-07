// Copyright Dit 2026
// SPDX-License-Identifier: BUSL-1.1

package dev.dit.shell

import io.kotlintest.TestCaseOrder
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.io.IOException

class CommandExceptionTest : StringSpec() {
    override fun testCaseOrder() = TestCaseOrder.Random

    init {
        "exitCode is set correctly" {
            val e = CommandException("failed", exitCode = 1, output = "error")
            e.exitCode shouldBe 1
        }

        "output is set correctly" {
            val e = CommandException("failed", exitCode = 1, output = "error output")
            e.output shouldBe "error output"
        }

        "message is set correctly" {
            val e = CommandException("command failed", exitCode = 1, output = "")
            e.message shouldBe "command failed"
        }

        "is an IOException" {
            val e: IOException = CommandException("failed", exitCode = 1, output = "")
            e.message shouldBe "failed"
        }

        "non-zero exit codes are preserved" {
            val e = CommandException("failed", exitCode = 127, output = "command not found")
            e.exitCode shouldBe 127
            e.output shouldBe "command not found"
        }
    }
}
