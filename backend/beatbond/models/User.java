package backend.beatbond.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class User {
    private String username;
    private String password;
    public HashSet<Integer> likedSongs = new HashSet<>();
    public HashSet<Integer> listeningHistory = new HashSet<>();
    public ArrayList<Integer> playlists = new ArrayList<>();
    public ArrayList<String> friends = new ArrayList<>();
    public LinkedList<Integer> recentlyPlayed = new LinkedList<>(); // track order, max 20

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    
    public void addToRecentlyPlayed(int songId) {
        recentlyPlayed.remove((Integer)songId); // remove if already there
        recentlyPlayed.addFirst(songId);
        if (recentlyPlayed.size() > 20) recentlyPlayed.removeLast(); // keep only last 20
    }
}
