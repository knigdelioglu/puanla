package com.knigdelioglu.arc.compose.display

import org.junit.Assert.*
import org.junit.Test

class ArcImageDropPolicyTest {
    @Test fun imageAndContentUriTransfersAreOffered() {
        assertTrue(ArcImageDropPolicy.accepts(listOf("image/jpeg")))
        assertTrue(ArcImageDropPolicy.accepts(listOf("image/png", "text/plain")))
        assertTrue(ArcImageDropPolicy.accepts(listOf("text/uri-list")))
        assertTrue(ArcImageDropPolicy.accepts(listOf("application/octet-stream")))
    }

    @Test fun textualContentCannotEnterImageDropPipeline() {
        assertFalse(ArcImageDropPolicy.accepts(emptyList()))
        assertFalse(ArcImageDropPolicy.accepts(listOf("text/plain")))
        assertFalse(ArcImageDropPolicy.accepts(listOf("application/pdf")))
    }
}
