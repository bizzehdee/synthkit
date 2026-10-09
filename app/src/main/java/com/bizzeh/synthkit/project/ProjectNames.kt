package com.bizzeh.synthkit.project

/** Naming rules for new, renamed and duplicated projects. */
object ProjectNames {
    /** The first "Project N" not already used, given a [pattern] like "Project %1$d". */
    fun next(existing: Collection<String>, pattern: String): String =
        generateSequence(1) { it + 1 }.map { pattern.format(it) }.first { it !in existing }

    /** A trimmed name, or null when it is blank. Long names are cut to the maximum length. */
    fun clean(name: String): String? = name.trim().take(Project.MAX_NAME_LENGTH).ifBlank { null }
}
