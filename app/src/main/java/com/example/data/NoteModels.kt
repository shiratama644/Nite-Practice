package com.example.data

enum class Clef {
    TREBLE, BASS
}

enum class Letter {
    C, D, E, F, G, A, B;

    val japanese: String
        get() = when (this) {
            C -> "ド"
            D -> "レ"
            E -> "ミ"
            F -> "ファ"
            G -> "ソ"
            A -> "ラ"
            B -> "シ"
        }
}

data class Note(
    val id: String,              // Unique identifier e.g. "TREBLE_C4"
    val clef: Clef,
    val letter: Letter,
    val octave: Int,             // e.g. 4 for Middle C (C4)
    val staffStep: Int           // Offset step from middle line of the staff (B4=0 for Treble, D3=0 for Bass)
) {
    val displayPitch: String
        get() = "${letter.name}$octave"

    val japaneseName: String
        get() = letter.japanese

    val englishName: String
        get() = letter.name
}

object NotePool {
    // Treble Clef Notes (from C4 up to B5)
    val trebleNotes = listOf(
        Note("TREBLE_C4", Clef.TREBLE, Letter.C, 4, -6),
        Note("TREBLE_D4", Clef.TREBLE, Letter.D, 4, -5),
        Note("TREBLE_E4", Clef.TREBLE, Letter.E, 4, -4),
        Note("TREBLE_F4", Clef.TREBLE, Letter.F, 4, -3),
        Note("TREBLE_G4", Clef.TREBLE, Letter.G, 4, -2),
        Note("TREBLE_A4", Clef.TREBLE, Letter.A, 4, -1),
        Note("TREBLE_B4", Clef.TREBLE, Letter.B, 4, 0),
        Note("TREBLE_C5", Clef.TREBLE, Letter.C, 5, 1),
        Note("TREBLE_D5", Clef.TREBLE, Letter.D, 5, 2),
        Note("TREBLE_E5", Clef.TREBLE, Letter.E, 5, 3),
        Note("TREBLE_F5", Clef.TREBLE, Letter.F, 5, 4),
        Note("TREBLE_G5", Clef.TREBLE, Letter.G, 5, 5),
        Note("TREBLE_A5", Clef.TREBLE, Letter.A, 5, 6),
        Note("TREBLE_B5", Clef.TREBLE, Letter.B, 5, 7)
    )

    // Bass Clef Notes (from E2 up to C4)
    val bassNotes = listOf(
        Note("BASS_E2", Clef.BASS, Letter.E, 2, -6),
        Note("BASS_F2", Clef.BASS, Letter.F, 2, -5),
        Note("BASS_G2", Clef.BASS, Letter.G, 2, -4),
        Note("BASS_A2", Clef.BASS, Letter.A, 2, -3),
        Note("BASS_B2", Clef.BASS, Letter.B, 2, -2),
        Note("BASS_C3", Clef.BASS, Letter.C, 3, -1),
        Note("BASS_D3", Clef.BASS, Letter.D, 3, 0),
        Note("BASS_E3", Clef.BASS, Letter.E, 3, 1),
        Note("BASS_F3", Clef.BASS, Letter.F, 3, 2),
        Note("BASS_G3", Clef.BASS, Letter.G, 3, 3),
        Note("BASS_A3", Clef.BASS, Letter.A, 3, 4),
        Note("BASS_B3", Clef.BASS, Letter.B, 3, 5),
        Note("BASS_C4", Clef.BASS, Letter.C, 4, 6)
    )

    val allNotes = trebleNotes + bassNotes

    fun getNoteById(id: String): Note? {
        return allNotes.find { it.id == id }
    }
}
