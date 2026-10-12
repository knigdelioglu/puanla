package com.knigdelioglu.puanla.ui.navigation

import com.knigdelioglu.puanla.ui.viewmodel.AppDestination

/** Only primary destinations belong in the tablet's main navigation. */
val primaryNavigationDestinations = listOf(
    AppDestination.WORKSPACE,
    AppDestination.CLASSROOMS,
    AppDestination.REPORTS
)

/** Settings is a utility screen; group work and OCR belong to their parent tabs. */
fun AppDestination.primaryTab(previousPrimaryTab: AppDestination = AppDestination.WORKSPACE): AppDestination =
    when (this) {
        AppDestination.WORKSPACE, AppDestination.GROUP_WORK -> AppDestination.WORKSPACE
        AppDestination.CLASSROOMS, AppDestination.OCR_IMPORT -> AppDestination.CLASSROOMS
        AppDestination.REPORTS -> AppDestination.REPORTS
        AppDestination.SETTINGS -> previousPrimaryTab
    }
