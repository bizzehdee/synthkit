package com.bizzeh.synthkit.audio

/** A playable preset in the loaded SoundFont. */
data class Preset(val bank: Int, val program: Int, val name: String) {
    companion object {
        private const val FIELDS_PER_PRESET = 3

        /** Parses the flat bank, program, name triples from the native engine. */
        fun fromNative(fields: Array<String>): List<Preset> {
            require(fields.size % FIELDS_PER_PRESET == 0) { "Preset fields are not in triples: ${fields.size}" }
            return fields.toList().chunked(FIELDS_PER_PRESET).map { (bank, program, name) ->
                Preset(bank.toInt(), program.toInt(), name)
            }
        }
    }
}
