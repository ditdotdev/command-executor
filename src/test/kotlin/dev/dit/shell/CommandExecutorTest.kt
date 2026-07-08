// Copyright Dit 2026
// SPDX-License-Identifier: BUSL-1.1

package dev.dit.shell

import io.kotlintest.TestCase
import io.kotlintest.TestCaseOrder
import io.kotlintest.TestResult
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.SpyK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class CommandExecutorTest : StringSpec() {
    @SpyK
    var executor: CommandExecutor = CommandExecutor()

    override fun beforeTest(testCase: TestCase) {
        MockKAnnotations.init(this)
    }

    override fun afterTest(
        testCase: TestCase,
        result: TestResult,
    ) {
        clearAllMocks()
    }

    override fun testCaseOrder() = TestCaseOrder.Random

    private fun mockProcess(
        stdout: String = "",
        stderr: String = "",
        exitCode: Int = 0,
        alive: Boolean = false,
    ): Process {
        val process = mockk<Process>()
        every { process.inputStream } returns ByteArrayInputStream(stdout.toByteArray())
        every { process.errorStream } returns ByteArrayInputStream(stderr.toByteArray())
        every { process.waitFor(any(), any()) } returns !alive
        every { process.isAlive } returns alive
        every { process.exitValue() } returns exitCode
        every { process.destroy() } just Runs
        return process
    }

    init {
        "default timeout is 60 seconds" {
            executor.timeout shouldBe 60
        }

        "custom timeout is set correctly" {
            val customExecutor = CommandExecutor(timeout = 120)
            customExecutor.timeout shouldBe 120
        }

        "exec returns stdout on success" {
            val process = mockProcess(stdout = "hello world")
            val result = executor.exec(process, "echo hello world")
            result shouldBe "hello world"
        }

        "exec destroys process on success" {
            val process = mockProcess(stdout = "output")
            executor.exec(process, "some command")
            verify { process.destroy() }
        }

        "exec throws on non-zero exit code" {
            val process = mockProcess(stderr = "something failed", exitCode = 1)
            val e =
                shouldThrow<CommandException> {
                    executor.exec(process, "bad command")
                }
            e.exitCode shouldBe 1
            e.output shouldBe "something failed"
        }

        "exec destroys process on failure" {
            val process = mockProcess(stderr = "error", exitCode = 1)
            shouldThrow<CommandException> {
                executor.exec(process, "bad command")
            }
            verify { process.destroy() }
        }

        "exec throws IOException on timeout" {
            val process = mockProcess(alive = true)
            shouldThrow<java.io.IOException> {
                executor.exec(process, "slow command")
            }
        }

        "exec with args delegates to start and exec" {
            val process = mockProcess(stdout = "result")
            every { executor.start(*anyVararg()) } returns process
            val result = executor.exec("echo", "result")
            result shouldBe "result"
        }

        "checkResult does not throw on zero exit code" {
            val process = mockProcess(exitCode = 0)
            executor.checkResult(process)
        }

        "checkResult throws CommandException on non-zero exit code" {
            val process = mockProcess(stderr = "error output", exitCode = 2)
            val e =
                shouldThrow<CommandException> {
                    executor.checkResult(process)
                }
            e.exitCode shouldBe 2
            e.output shouldBe "error output"
        }

        "checkResult exception message contains error output" {
            val process = mockProcess(stderr = "permission denied", exitCode = 1)
            val e =
                shouldThrow<CommandException> {
                    executor.checkResult(process)
                }
            e.message shouldNotBe null
            e.message!!.contains("permission denied") shouldBe true
        }

        "getOutput returns process stdout" {
            val process = mockProcess(stdout = "command output")
            val result = executor.getOutput(process)
            result shouldBe "command output"
        }

        "getOutput returns empty string for no output" {
            val process = mockProcess(stdout = "")
            val result = executor.getOutput(process)
            result shouldBe ""
        }

        "getOutput returns multiline output" {
            val process = mockProcess(stdout = "line1\nline2\nline3")
            val result = executor.getOutput(process)
            result shouldBe "line1\nline2\nline3"
        }

        "start returns a process" {
            every { executor.start(*anyVararg()) } returns mockProcess()
            val process = executor.start("echo", "test")
            process shouldNotBe null
        }

        "log companion object is accessible" {
            CommandExecutor.log shouldNotBe null
        }

        "start creates a real process" {
            val realExecutor = CommandExecutor()
            val process = realExecutor.start("java", "-version")
            process shouldNotBe null
            process.waitFor()
            process.destroy()
        }

        "exec with real process succeeds" {
            val realExecutor = CommandExecutor()
            val process = realExecutor.start("java", "-version")
            val result = realExecutor.exec(process, "java -version")
            result shouldNotBe null
        }

        "getOutput closes the inputStream even when read throws IOException" {
            val throwingStream =
                object : InputStream() {
                    var closed: Boolean = false

                    override fun read(): Int = throw IOException("boom")

                    override fun close() {
                        closed = true
                        super.close()
                    }
                }
            val process = mockk<Process>()
            every { process.inputStream } returns throwingStream
            shouldThrow<IOException> {
                executor.getOutput(process)
            }
            throwingStream.closed shouldBe true
        }

        "checkResult closes the errorStream even when read throws IOException" {
            val throwingStream =
                object : InputStream() {
                    var closed: Boolean = false

                    override fun read(): Int = throw IOException("boom")

                    override fun close() {
                        closed = true
                        super.close()
                    }
                }
            val process = mockk<Process>()
            every { process.exitValue() } returns 1
            every { process.errorStream } returns throwingStream
            shouldThrow<IOException> {
                executor.checkResult(process)
            }
            throwingStream.closed shouldBe true
        }

        "getOutput propagates IOException when both read and close throw" {
            val throwingStream =
                object : InputStream() {
                    override fun read(): Int = throw IOException("read boom")

                    override fun close(): Unit = throw IOException("close boom")
                }
            val process = mockk<Process>()
            every { process.inputStream } returns throwingStream
            shouldThrow<IOException> {
                executor.getOutput(process)
            }
        }

        "checkResult propagates IOException when both read and close throw" {
            val throwingStream =
                object : InputStream() {
                    override fun read(): Int = throw IOException("read boom")

                    override fun close(): Unit = throw IOException("close boom")
                }
            val process = mockk<Process>()
            every { process.exitValue() } returns 1
            every { process.errorStream } returns throwingStream
            shouldThrow<IOException> {
                executor.checkResult(process)
            }
        }

        "exec logs and rethrows IOException from getOutput" {
            val throwingStream =
                object : InputStream() {
                    override fun read(): Int = throw IOException("read failed")
                }
            val process = mockk<Process>()
            every { process.inputStream } returns throwingStream
            every { process.exitValue() } returns 1
            every { process.destroy() } just Runs
            shouldThrow<IOException> {
                executor.exec(process, "broken command")
            }
            verify { process.destroy() }
        }
    }
}
