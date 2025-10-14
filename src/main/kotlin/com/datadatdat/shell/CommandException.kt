/*
 * Copyright Datadatdat.
 */

package com.datadatdat.shell

import java.io.IOException

class CommandException(
    message: String,
    val exitCode: Int,
    val output: String,
) : IOException(message)
