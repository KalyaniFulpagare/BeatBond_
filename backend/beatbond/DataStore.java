package backend.beatbond;

import backend.beatbond.models.*;
import java.util.ArrayList;
import java.util.HashMap;

public class DataStore {
    public HashMap<String, User> users = new HashMap<>();
    public ArrayList<Song> songs = new ArrayList<>();
    public ArrayList<Playlist> playlists = new ArrayList<>();

    private static DataStore instance = null;

    private DataStore() {
        // sample songs
        songs.add(new Song(1, "Sunny Beat", "DJ A", "song1.mp3", "Pop"));
        songs.add(new Song(2, "Midnight Loop", "DJ B", "song2.mp3", "Electronic"));
        songs.add(new Song(3, "Electric Flow", "Artist C", "song3.mp3", "Electronic"));
        songs.add(new Song(4, "Deep Vibes", "Artist D", "song4.mp3", "Ambient"));
        songs.add(new Song(5, "Night Drive", "Artist E", "song5.mp3", "Synthwave"));

        // sample users with listening history
        User u1 = new User("alice","pass");
        User u2 = new User("bob","pass");
        
        // alice listened to songs 1, 2, 3
        u1.listeningHistory.add(1);
        u1.listeningHistory.add(2);
        u1.listeningHistory.add(3);
        u1.likedSongs.add(1);
        
        // bob listened to songs 2, 3, 4 (similar to alice)
        u2.listeningHistory.add(2);
        u2.listeningHistory.add(3);
        u2.listeningHistory.add(4);
        u2.likedSongs.add(2);
        
        u1.friends.add("bob");
        u2.friends.add("alice");
        
        users.put(u1.getUsername(), u1);
        users.put(u2.getUsername(), u2);
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) instance = new DataStore();
        return instance;
    }

    public Song findSongById(int id) {
        for (Song s: songs) if (s.getId()==id) return s;
        return null;
    }
}
