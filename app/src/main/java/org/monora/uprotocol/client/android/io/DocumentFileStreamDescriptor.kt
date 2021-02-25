package org.monora.uprotocol.client.android.io

import com.genonbeta.android.framework.io.DocumentFile
import org.monora.uprotocol.core.io.StreamDescriptor

class DocumentFileStreamDescriptor(val documentFile: DocumentFile) : StreamDescriptor {
    override fun length(): Long = documentFile.getLength()
}