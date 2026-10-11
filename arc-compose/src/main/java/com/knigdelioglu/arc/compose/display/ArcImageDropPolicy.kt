package com.knigdelioglu.arc.compose.display

/** A drop is only offered when Android advertises an image or URI transfer. */
object ArcImageDropPolicy {
    fun accepts(mimeTypes: Collection<String>): Boolean = mimeTypes.any { type ->
        type.startsWith("image/", ignoreCase = true) ||
            type.equals("text/uri-list", ignoreCase = true) ||
            type.equals("application/octet-stream", ignoreCase = true)
    }
}
