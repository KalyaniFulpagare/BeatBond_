package backend.beatbond;

import backend.beatbond.models.User;
import backend.beatbond.models.Song;

import java.util.*;

public class RecommendationEngine {
    private DataStore store = DataStore.getInstance();

    public List<Song> friendRecommendations(String username) {
        User u = store.users.get(username);
        if (u==null) return new ArrayList<>();
        HashSet<Integer> result = new HashSet<>();
        for (String friend : u.friends) {
            User f = store.users.get(friend);
            if (f==null) continue;
            for (Integer id : f.listeningHistory) {
                if (!u.listeningHistory.contains(id)) result.add(id);
            }
        }
        List<Song> out = new ArrayList<>();
        for (Integer id: result) {
            Song s = store.findSongById(id);
            if (s!=null) out.add(s);
        }
        return out;
    }

    public String musicTwinWithReason(String username) {
        User u = store.users.get(username);
        if (u==null) return "{\"twin\":\"\",\"reason\":\"\",\"common\":0,\"score\":0.0,\"commonSongs\":[]}";
        
        // Use liked songs as primary (more intentional), fall back to listening history
        Set<Integer> userPrefs = u.likedSongs.isEmpty() ? new HashSet<>(u.listeningHistory) : u.likedSongs;
        
        // If user has no prefs, cannot find a twin
        if (userPrefs.isEmpty()) return "{\"twin\":\"\",\"reason\":\"\",\"common\":0,\"score\":0.0,\"commonSongs\":[]}";
        
        double bestScore = -1.0;
        String bestUser = "";
        Set<Integer> bestCommonSet = new HashSet<>();
        
        for (Map.Entry<String,User> e: store.users.entrySet()) {
            if (e.getKey().equals(username)) continue;
            User other = e.getValue();
            
            // Use other's liked songs as primary
            Set<Integer> otherPrefs = other.likedSongs.isEmpty() ? new HashSet<>(other.listeningHistory) : other.likedSongs;
            if (otherPrefs.isEmpty()) continue;
            
            Set<Integer> inter = new HashSet<>(userPrefs);
            inter.retainAll(otherPrefs);
            
            // Only consider if there's actual common ground
            if (inter.isEmpty()) continue;
            
            double score = jaccard(userPrefs, otherPrefs);
            if (score > bestScore) {
                bestScore = score;
                bestUser = other.getUsername();
                bestCommonSet = inter;
            }
        }
        
        // No meaningful match found
        if (bestScore <= 0 || bestCommonSet.isEmpty()) return "{\"twin\":\"\",\"reason\":\"\",\"common\":0,\"score\":0.0,\"commonSongs\":[]}";
        
        int commonCount = bestCommonSet.size();
        String reason = "";
        if (commonCount==1) reason = "You both like the same song";
        else if (commonCount==2) reason = "You share "+commonCount+" favorite songs";
        else if (commonCount>=3) reason = "Strong match! "+commonCount+" songs in common";
        
        // build common song titles array
        StringBuilder commonArr = new StringBuilder();
        commonArr.append('[');
        int idx=0;
        for (Integer id: bestCommonSet) {
            Song s = store.findSongById(id);
            if (s==null) continue;
            if (idx>0) commonArr.append(',');
            commonArr.append('"').append(escape(s.getTitle())).append('"');
            idx++;
        }
        commonArr.append(']');

        String scoreStr = String.format("%.0f", bestScore*100);
        return "{\"twin\":\""+escape(bestUser)+"\",\"reason\":\""+escape(reason)+"\",\"common\":"+commonCount+",\"score\":"+scoreStr+",\"commonSongs\":"+commonArr.toString()+"}";
    }

    private String escape(String s) {
        if (s==null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }

    private double jaccard(Set<Integer> a, Set<Integer> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<Integer> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<Integer> uni = new HashSet<>(a);
        uni.addAll(b);
        return (double)inter.size()/ (double)uni.size();
    }
}
