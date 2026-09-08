package today.vanta.util.client.music;
// from yuri client all credit to kawase for making the core thing or sum and unleg1t for making it work
public class MediaTrack {
    private final String title;
    private final String artist;
    private final String album;
    private final long lengthMillis;
    private final long capturedPositionMillis;
    private final long capturedAt;
    private final boolean playing;
    private final String source;

    public MediaTrack(String title, String artist, String album, long lengthMillis, long capturedPositionMillis, boolean playing, String source) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.lengthMillis = lengthMillis;
        this.capturedPositionMillis = capturedPositionMillis;
        this.capturedAt = System.currentTimeMillis();
        this.playing = playing;
        this.source = source;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public long getLengthMillis() { return lengthMillis; }
    public long getCapturedPositionMillis() { return capturedPositionMillis; }
    public long getCapturedAt() { return capturedAt; }
    public boolean isPlaying() { return playing; }
    public String getSource() { return source; }
}
