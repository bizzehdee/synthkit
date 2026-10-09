package com.bizzeh.synthkit.export

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportFileNameTest {
    @Test
    fun keepsLettersDigitsSpacesDashesAndUnderscores() {
        assertEquals("My Song-2_b", ExportViewModel.fileName("My Song-2_b"))
    }

    @Test
    fun dropsPathAndShellCharacters() {
        assertEquals("etcpasswd", ExportViewModel.fileName("../etc/passwd"))
        assertEquals("ab", ExportViewModel.fileName("a;*?\\\"<>|b"))
    }

    @Test
    fun emptyResultFallsBackAndLongNamesAreCut() {
        assertEquals("export", ExportViewModel.fileName("///"))
        assertEquals(60, ExportViewModel.fileName("x".repeat(200)).length)
    }
}
