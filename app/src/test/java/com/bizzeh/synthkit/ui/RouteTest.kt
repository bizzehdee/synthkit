package com.bizzeh.synthkit.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteTest {
    @Test
    fun everyRouteSurvivesEncoding() {
        listOf(Route.Projects, Route.Browser, Route.Project("p-1"), Route.Play("128:25")).forEach { route ->
            assertEquals(route, Route.decode(route.encode()))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownRouteIsRejected() {
        Route.decode("settings")
    }

    @Test
    fun openingAnInstrumentKeepsTheOpenProjectUnderneath() {
        val stack = listOf(Route.Projects, Route.Project("p"), Route.Browser)

        assertEquals(
            listOf(Route.Projects, Route.Project("p"), Route.Play("0:24")),
            Route.openInstrument(stack, "0:24"),
        )
    }

    @Test
    fun openingAnInstrumentReplacesAnEarlierInstrument() {
        val stack = listOf(Route.Projects, Route.Project("p"), Route.Play("0:0"), Route.Browser)

        assertEquals(Route.Play("0:24"), Route.openInstrument(stack, "0:24").last())
        assertEquals(3, Route.openInstrument(stack, "0:24").size)
    }

    @Test
    fun openingAnInstrumentWithoutAProjectStartsFromTheList() {
        assertEquals(listOf(Route.Projects, Route.Play("0:0")), Route.openInstrument(listOf(Route.Browser), "0:0"))
    }
}
