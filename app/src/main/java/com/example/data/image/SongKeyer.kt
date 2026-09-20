package com.example.data.image

import coil.key.Keyer
import coil.request.Options
import com.example.data.model.Song

/**
 * Ensures Coil generates stable, unique memory and disk cache keys for [Song] instances.
 */
class SongKeyer : Keyer<Song> {
    override fun key(data: Song, options: Options): String {
        return "album_art_song_${data.id}"
    }
}
