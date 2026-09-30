package pt.ipc_app.service.models.plans

import pt.ipc_app.domain.DailyList

/** Plan template has numbered days; calendar dates belong to patient assignments. */
data class LibraryPlan(val id: Int, val title: String, val dailyLists: List<DailyList?>)
