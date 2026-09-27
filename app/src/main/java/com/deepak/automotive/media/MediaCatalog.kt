package com.deepak.automotive.media

/** Tiny in-memory catalog so the media tree works without network or audio files. */
data class Track(val id: String, val title: String, val artist: String, val durationMs: Long)

data class Category(val id: String, val title: String, val tracks: List<Track>)

object MediaCatalog {
    const val ROOT_ID = "root"

    val categories = listOf(
        Category(
            "drive", "Drive Mix", listOf(
                Track("t1", "Highway Lights", "The ECUs", 212_000),
                Track("t2", "CAN You Hear Me", "Bus Arbitration", 187_000),
                Track("t3", "Torque Vectoring", "Drivetrain", 240_000),
            )
        ),
        Category(
            "podcasts", "Automotive Podcasts", listOf(
                Track("p1", "Inside the VHAL", "AAOS Weekly", 1_800_000),
                Track("p2", "Secure Boot Explained", "Car Security Talk", 1_500_000),
            )
        ),
    )

    val allTracks: List<Track> get() = categories.flatMap { it.tracks }
    fun track(id: String) = allTracks.firstOrNull { it.id == id }
}
