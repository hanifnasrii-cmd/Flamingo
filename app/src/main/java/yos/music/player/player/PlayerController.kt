package yos.music.player.player

interface PlayerController {
    fun togglePlayPause()
    fun isPlaying(): Boolean
    fun nowPlayingTitle(): String?
    fun nowPlayingArtUri(): String?
}
