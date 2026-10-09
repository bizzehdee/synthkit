package com.bizzeh.synthkit.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteTest {
    @Test
    fun everyRouteSurvivesEncoding() {
        listOf(Route.Home, Route.Browser, Route.Play("128:25")).forEach { route ->
            assertEquals(route, Route.decode(route.encode()))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownRouteIsRejected() {
        Route.decode("settings")
    }

    @Test
    fun openingAnInstrumentLeavesHomeUnderneath() {
        assertEquals(listOf(Route.Home, Route.Play("0:24")), Route.openInstrument("0:24"))
    }
}
