package com.bizzeh.synthkit.project

/** What the project list can ask for. [create] returns the new project so it can be opened. */
class ProjectActions(
    val create: () -> Project,
    val rename: (String, String) -> Unit,
    val duplicate: (String) -> Unit,
    val delete: (String) -> Unit,
)
