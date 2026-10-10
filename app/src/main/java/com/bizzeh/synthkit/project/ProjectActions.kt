package com.bizzeh.synthkit.project

/** What the project list can ask for. [create] returns the new project so it can be opened. */
class ProjectActions(
    /** Creates a project; the argument turns the playback click on. */
    val create: (Boolean) -> Project,
    val rename: (String, String) -> Unit,
    val duplicate: (String) -> Unit,
    val delete: (String) -> Unit,
    /** Saves a changed project. */
    val save: (Project) -> Unit,
)
