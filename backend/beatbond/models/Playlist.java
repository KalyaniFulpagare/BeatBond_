package backend.beatbond.models;

import java.util.ArrayList;

public class Playlist {
    private int id;
    private String name;
    private String owner;
    public ArrayList<Integer> songIds = new ArrayList<>();

    public Playlist(int id, String name, String owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getOwner() { return owner; }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\":").append(id).append(",\"name\":\"").append(escape(name)).append("\",\"owner\":\"").append(escape(owner)).append("\",\"songs\":[");
        for (int i = 0; i < songIds.size(); i++) {
            if (i>0) sb.append(',');
            sb.append(songIds.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }
}
