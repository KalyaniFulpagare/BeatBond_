package backend.beatbond;

import backend.beatbond.models.*;
import java.util.*;

public class MusicService {
    private DataStore store = DataStore.getInstance();
    private RecommendationEngine rec = new RecommendationEngine();

    public boolean register(String username, String password) {
        if (store.users.containsKey(username)) return false;
        store.users.put(username, new User(username,password));
        return true;
    }

    public boolean login(String username, String password) {
        User u = store.users.get(username);
        if (u==null) return false;
        return u.getPassword().equals(password);
    }

    public List<Song> allSongs() { return store.songs; }

    public Song playSong(String username, int songId) {
        Song s = store.findSongById(songId);
        if (s==null) return null;
        s.incrementPlay();
        if (username!=null) {
            User u = store.users.get(username);
            if (u!=null) {
                u.listeningHistory.add(songId);
                u.addToRecentlyPlayed(songId);
            }
        }
        return s;
    }

    public boolean likeSong(String username, int songId) {
        User u = store.users.get(username);
        if (u==null) return false;
        u.likedSongs.add(songId);
        return true;
    }

    public boolean unlikeSong(String username, int songId) {
        User u = store.users.get(username);
        if (u==null) return false;
        u.likedSongs.remove(songId);
        return true;
    }

    public String getUserData(String username) {
        User u = store.users.get(username);
        if (u==null) return "{}";
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"username\":\"").append(u.getUsername()).append("\"");
        sb.append(",\"liked\":[");
        int i=0; for (Integer id: u.likedSongs) { if (i>0) sb.append(','); sb.append(id); i++; }
        sb.append(']');
        sb.append(",\"playlists\":[");
        i=0; for (Integer pid: u.playlists) { if (i>0) sb.append(','); Playlist p = store.playlists.get(pid-1); sb.append(p.toJson()); i++; }
        sb.append(']');
        sb.append('}');
        return sb.toString();
    }

    public List<Playlist> allPlaylists() { return store.playlists; }

    public Playlist getPlaylist(int id) {
        if (id<=0 || id>store.playlists.size()) return null;
        return store.playlists.get(id-1);
    }

    public boolean removeFromPlaylist(int playlistId, int songId) {
        Playlist p = getPlaylist(playlistId);
        if (p==null) return false;
        p.songIds.removeIf(sid->sid==songId);
        return true;
    }

    public boolean deletePlaylist(int playlistId, String username) {
        if (playlistId<=0 || playlistId>store.playlists.size()) return false;
        Playlist p = store.playlists.get(playlistId-1);
        if (!p.getOwner().equals(username)) return false;
        store.playlists.remove(playlistId-1);
        // update user playlist ids
        for (User u: store.users.values()) {
            u.playlists.removeIf(id->id==playlistId);
        }
        return true;
    }

    public Playlist createPlaylist(String username, String name) {
        int id = store.playlists.size()+1;
        Playlist p = new Playlist(id,name,username);
        store.playlists.add(p);
        User u = store.users.get(username);
        if (u!=null) u.playlists.add(p.getId());
        return p;
    }

    public boolean addToPlaylist(int playlistId, int songId) {
        if (playlistId<=0 || playlistId>store.playlists.size()) return false;
        Playlist p = store.playlists.get(playlistId-1);
        p.songIds.add(songId);
        return true;
    }

    public List<Song> trending() {
        List<Song> copy = new ArrayList<>(store.songs);
        copy.sort((a,b)->Integer.compare(b.getPlayCount(), a.getPlayCount()));
        return copy;
    }

    public List<Song> friendRecommendations(String username) {
        return rec.friendRecommendations(username);
    }

    public String musicTwinWithReason(String username) { return rec.musicTwinWithReason(username); }

    public List<Song> recentlyPlayed(String username) {
        User u = store.users.get(username);
        if (u==null) return new ArrayList<>();
        List<Song> result = new ArrayList<>();
        for (Integer id: u.recentlyPlayed) {
            Song s = store.findSongById(id);
            if (s!=null) result.add(s);
        }
        return result;
    }

    public String getUserStats(String username) {
        User u = store.users.get(username);
        if (u==null) return "{}";
        int totalPlays = 0;
        for (Song s: store.songs) totalPlays += s.getPlayCount();
        int userLiked = u.likedSongs.size();
        int userPlayed = u.listeningHistory.size();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"username\":\"").append(u.getUsername()).append("\"");
        sb.append(",\"totalLiked\":").append(userLiked);
        sb.append(",\"totalPlayed\":").append(userPlayed);
        sb.append(",\"totalPlaysGlobal\":").append(totalPlays);
        sb.append("}");
        return sb.toString();
    }

    public List<Song> popularArtists() {
        java.util.Map<String, Integer> artistPlays = new java.util.HashMap<>();
        for (Song s: store.songs) {
            artistPlays.put(s.getArtist(), artistPlays.getOrDefault(s.getArtist(), 0) + s.getPlayCount());
        }
        List<Song> topByArtist = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        List<Song> sorted = new ArrayList<>(store.songs);
        sorted.sort((a,b)->Integer.compare(b.getPlayCount(), a.getPlayCount()));
        for (Song s: sorted) {
            if (!seen.contains(s.getArtist())) {
                topByArtist.add(s);
                seen.add(s.getArtist());
            }
            if (topByArtist.size()>=5) break;
        }
        return topByArtist;
    }}