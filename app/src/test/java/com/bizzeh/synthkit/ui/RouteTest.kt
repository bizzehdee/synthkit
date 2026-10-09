package com.bizzeh.synthkit.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteTest {
    private val routes = listOf(
        Route.Projects,
        Route.Project("p-1"),
        Route.AddTrack("p-1"),
        Route.Browser("p-1"),
        Route.Browser("p-1", "t-2"),
        Route.Track("p-1", "t-2"),
        Route.Editor("p-1", "t-2"),
        Route.Export("p-1"),
    )

    @Test
    fun everyRouteSurvivesEncoding() {
        routes.forEach { assertEquals(it, Route.decode(it.encode())) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownRouteIsRejected() {
        Route.decode("settings")
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedRouteIsRejected() {
        Route.decode("track/only-one-id")
    }

    @Test
    fun everyScreenButTheListBelongsToAProject() {
        assertNull(Route.Projects.projectId)
        routes.drop(1).forEach { assertEquals("p-1", it.projectId) }
    }
}
