package backend.beatbond.models;

public class Song {
    private int id;
    private String title;
    private String artist;
    private int playCount;
    private String filename;
    private String genre;

    public Song(int id, String title, String artist, String filename, String genre) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.filename = filename;
        this.genre = genre;
        this.playCount = 0;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public int getPlayCount() { return playCount; }
    public void incrementPlay() { playCount++; }
    public String getFilename() { return filename; }
    public String getGenre() { return genre; }

    public String toJson() {
        return "{\"id\":"+id+",\"title\":\""+escape(title)+"\",\"artist\":\""+escape(artist)+"\",\"playCount\":"+playCount+",\"filename\":\""+escape(filename)+"\",\"genre\":\""+escape(genre)+"\"}";
    }

    private String escape(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }
}
