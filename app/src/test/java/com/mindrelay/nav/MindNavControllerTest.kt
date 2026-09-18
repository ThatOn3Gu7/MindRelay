package com.mindrelay.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MindNavControllerTest {

    @Test
    fun startsOnHome() {
        val nav = MindNavController()
        assertEquals(MindScreen.HOME, nav.current.screen)
        assertFalse(nav.canPop)
        assertEquals(1, nav.stack.size)
    }

    @Test
    fun pushAddsDestination() {
        val nav = MindNavController()
        nav.navigate(MindScreen.INBOX, MindTransition.SLIDE_RIGHT)
        assertEquals(MindScreen.INBOX, nav.current.screen)
        assertEquals(2, nav.stack.size)
        assertTrue(nav.canPop)
    }

    @Test
    fun popReturnsToPreviousDestinationWithoutDuplicates() {
        val nav = MindNavController()
        nav.navigate(MindScreen.INBOX, MindTransition.SLIDE_RIGHT)
        nav.navigate(MindScreen.CAPTURE_DETAIL, MindTransition.SLIDE_RIGHT, "42")
        assertEquals(3, nav.stack.size)

        nav.pop()
        assertEquals(MindScreen.INBOX, nav.current.screen)
        assertEquals(2, nav.stack.size)

        nav.pop()
        assertEquals(MindScreen.HOME, nav.current.screen)
        assertEquals(1, nav.stack.size)

        // Popping at the root is a no-op and never underflows.
        nav.pop()
        assertEquals(MindScreen.HOME, nav.current.screen)
        assertEquals(1, nav.stack.size)
    }

    @Test
    fun popToRootCollapsesToBottomTab() {
        val nav = MindNavController()
        // Tabs are entered via resetTo (the single-tab route is the new root).
        nav.resetTo(listOf(MindRoute(MindScreen.PROJECTS)), MindTransition.FADE)
        nav.navigate(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, "7")
        nav.navigate(MindScreen.SESSION, MindTransition.SLIDE_RIGHT, "9")
        assertEquals(3, nav.stack.size)

        nav.popToRoot()
        assertEquals(1, nav.stack.size)
        assertEquals(MindScreen.PROJECTS, nav.current.screen)
    }

    @Test
    fun replaceTopSwapsCurrentDestinationWithoutGrowingStack() {
        val nav = MindNavController()
        nav.navigate(MindScreen.NEW_PROJECT, MindTransition.SLIDE_UP)
        assertEquals(2, nav.stack.size)
        nav.replaceTop(MindScreen.PROJECT_DETAIL, MindTransition.SLIDE_RIGHT, "5")
        assertEquals(2, nav.stack.size)
        assertEquals(MindScreen.PROJECT_DETAIL, nav.current.screen)
    }

    @Test
    fun resetToReplacesWholeStack() {
        val nav = MindNavController()
        nav.navigate(MindScreen.INBOX, MindTransition.SLIDE_RIGHT)
        nav.navigate(MindScreen.CAPTURE_DETAIL, MindTransition.SLIDE_RIGHT, "1")
        nav.resetTo(listOf(MindRoute(MindScreen.MEMORIES)), MindTransition.FADE)
        assertEquals(1, nav.stack.size)
        assertEquals(MindScreen.MEMORIES, nav.current.screen)
    }

    @Test
    fun tabSwitchingUsesResetToSoBackDoesNotReplayTabs() {
        val nav = MindNavController()
        // Simulate the nav bar: switching tabs resets to the single tab route.
        nav.resetTo(listOf(MindRoute(MindScreen.MEMORIES)), MindTransition.FADE)
        nav.navigate(MindScreen.MEMORY_DETAIL, MindTransition.SLIDE_RIGHT, "3")
        nav.pop()
        assertEquals(MindScreen.MEMORIES, nav.current.screen)
        assertEquals(1, nav.stack.size)
    }
}
