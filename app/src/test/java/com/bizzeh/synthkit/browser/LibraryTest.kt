package com.bizzeh.synthkit.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryTest {
    @Test
    fun toggleAddsThenRemovesAFavourite() {
        val added = Library().toggleFavourite("0:24")

        assertEquals(setOf("0:24"), added.favourites)
        assertEquals(emptySet<String>(), added.toggleFavourite("0:24").favourites)
    }

    @Test
    fun openedInstrumentMovesToTheFrontWithoutDuplicates() {
        val library = Library(recents = listOf("0:1", "0:2", "0:3")).opened("0:2")

        assertEquals(listOf("0:2", "0:1", "0:3"), library.recents)
    }

    @Test
    fun recentsKeepOnlyTheLatestEight() {
        val library = (0 until 10).fold(Library()) { lib, program -> lib.opened("0:$program") }

        assertEquals((9 downTo 2).map { "0:$it" }, library.recents)
    }
}
