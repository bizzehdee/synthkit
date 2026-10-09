package com.bizzeh.synthkit.browser

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryViewModelTest {
    private val application =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application

    private fun clear() = application.getSharedPreferences("library", Context.MODE_PRIVATE).edit().clear().commit()

    @Before
    fun clearBefore() {
        clear()
    }

    @After
    fun clearAfter() {
        clear()
    }

    @Test
    fun favouritesAndRecentsSurviveANewViewModel() {
        val first = LibraryViewModel(application)
        first.toggleFavourite("0:24")
        first.opened("0:40")
        first.opened("128:0")

        val second = LibraryViewModel(application)

        assertEquals(Library(favourites = setOf("0:24"), recents = listOf("128:0", "0:40")), second.library.value)
    }
}
