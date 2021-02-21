package org.monora.uprotocol.client.android.io

import org.monora.uprotocol.core.io.StreamDescriptor
import java.io.File

class FileStreamDescriptor(val file: File) : StreamDescriptor {
    override fun length(): Long = file.length()
}