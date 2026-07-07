// Copyright Dit 2026
// SPDX-License-Identifier: BUSL-1.1

package dev.dit.shell

import java.io.IOException

class CommandException(
    message: String,
    val exitCode: Int,
    val output: String,
) : IOException(message)
