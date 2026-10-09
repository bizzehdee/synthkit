package com.bizzeh.synthkit.project

/**
 * Checks a project read from storage before it is used. Project files are
 * untrusted input: a corrupt or hand-edited file must not crash the app,
 * reach outside the projects folder, or use unbounded memory.
 */
object ProjectValidation {
    const val MAX_TRACKS = 8
    const val MAX_NOTES_PER_TRACK = 10_000
    private val ID = Regex("^[A-Za-z0-9-]{1,64}$")
    private val BANKS = setOf(0, 128)

    fun isSafeId(id: String): Boolean = ID.matches(id)

    /** Every problem found; an empty list means the project is valid. */
    fun problems(project: Project): List<String> = buildList {
        if (project.formatVersion != Project.FORMAT_VERSION) add("unsupported format ${project.formatVersion}")
        if (!isSafeId(project.id)) add("bad project id")
        if (project.name.isBlank() || project.name.length > Project.MAX_NAME_LENGTH) add("bad name")
        if (project.tempoBpm !in Project.MIN_TEMPO..Project.MAX_TEMPO) add("tempo ${project.tempoBpm} out of range")
        if (project.loopBars !in 0..Project.MAX_LOOP_BARS) add("loop bars ${project.loopBars} out of range")
        if (project.tracks.size > MAX_TRACKS) add("too many tracks")
        if (project.tracks.map { it.id }.toSet().size != project.tracks.size) add("duplicate track ids")
        project.tracks.forEach { addAll(trackProblems(it, project.loopTicks)) }
    }

    private fun trackProblems(track: Track, loopTicks: Int): List<String> = buildList {
        val label = "track ${track.id.take(16)}"
        if (!isSafeId(track.id)) add("$label: bad id")
        if (track.bank !in BANKS || track.program !in 0..127) add("$label: bad instrument")
        if (!(track.volume in 0f..1f)) add("$label: bad volume")
        val notes = track.notes
        if (notes.size > MAX_NOTES_PER_TRACK) add("$label: too many notes")
        if (notes.isNotEmpty() && loopTicks == 0) add("$label: notes without a loop")
        if (notes.any { it.key !in 0..127 || it.velocity !in 1..127 || it.lengthTicks < 1 || it.tick !in 0 until maxOf(loopTicks, 1) }) {
            add("$label: note out of range")
        }
    }
}
