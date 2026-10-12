package com.knigdelioglu.puanla.ui.navigation

import com.knigdelioglu.puanla.ui.viewmodel.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationDestinationsTest {
    @Test
    fun onlyThreePrimaryDestinationsAreShown() {
        assertEquals(
            listOf(AppDestination.WORKSPACE, AppDestination.CLASSROOMS, AppDestination.REPORTS),
            primaryNavigationDestinations
        )
        assertEquals(listOf("Puanlama", "Sınıflar", "Raporlar"), primaryNavigationDestinations.map { it.label })
    }

    @Test
    fun groupAndOcrKeepTheirParentTabsSelected() {
        assertEquals(AppDestination.WORKSPACE, AppDestination.GROUP_WORK.primaryTab())
        assertEquals(AppDestination.CLASSROOMS, AppDestination.OCR_IMPORT.primaryTab())
    }

    @Test
    fun settingsPreservesThePreviouslySelectedPrimaryTab() {
        assertEquals(AppDestination.REPORTS, AppDestination.SETTINGS.primaryTab(AppDestination.REPORTS))
        assertEquals(AppDestination.CLASSROOMS, AppDestination.SETTINGS.primaryTab(AppDestination.CLASSROOMS))
    }
}
