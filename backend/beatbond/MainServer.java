package backend.beatbond;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import backend.beatbond.models.Song;
import backend.beatbond.models.Playlist;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

public class MainServer {
    private static MusicService service = new MusicService();
    private static DataStore store = DataStore.getInstance();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);

        server.createContext("/", new StaticHandler());
        server.createContext("/assets/", new StaticHandler());

        server.createContext("/api/register", (exchange)->{
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,200,"{\"ok\":false}"); return; }
            String body = readBody(exchange);
            Map<String,String> map = parseJson(body);
            boolean ok = service.register(map.getOrDefault("username",""), map.getOrDefault("password",""));
            sendJson(exchange,200,"{\"ok\":"+ok+"}");
        });

        server.createContext("/api/login", (exchange)->{
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,200,"{\"ok\":false}"); return; }
            String body = readBody(exchange);
            Map<String,String> map = parseJson(body);
            boolean ok = service.login(map.getOrDefault("username",""), map.getOrDefault("password",""));
            sendJson(exchange,200,"{\"ok\":"+ok+"}");
        });

        server.createContext("/api/songs", (exchange)->{
            List<String> arr = store.songs.stream().map(Song::toJson).collect(Collectors.toList());
            sendJson(exchange,200,"["+String.join(",",arr)+"]");
        });

        server.createContext("/api/play", (exchange)->{
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,200,"{\"ok\":false}"); return; }
            String body = readBody(exchange);
            Map<String,String> map = parseJson(body);
            String user = map.getOrDefault("username", null);
            int id = Integer.parseInt(map.getOrDefault("id","0"));
            Song s = service.playSong(user, id);
            if (s==null) sendJson(exchange,404,"{\"ok\":false}"); else sendJson(exchange,200,"{\"ok\":true,\"playCount\":"+s.getPlayCount()+"}");
        });

        server.createContext("/api/like", (exchange)->{
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,200,"{\"ok\":false}"); return; }
            String body = readBody(exchange);
            Map<String,String> map = parseJson(body);
            boolean ok = service.likeSong(map.getOrDefault("username",""), Integer.parseInt(map.getOrDefault("id","0")));
            sendJson(exchange,200,"{\"ok\":"+ok+"}");
        });

        server.createContext("/api/unlike", (exchange)->{
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,200,"{\"ok\":false}"); return; }
            String body = readBody(exchange);
            Map<String,String> map = parseJson(body);
            boolean ok = service.unlikeSong(map.getOrDefault("username",""), Integer.parseInt(map.getOrDefault("id","0")));
            sendJson(exchange,200,"{\"ok\":"+ok+"}");
        });

        server.createContext("/api/createPlaylist", (exchange)->{
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,200,"{\"ok\":false}"); return; }
            String body = readBody(exchange);
            Map<String,String> map = parseJson(body);
            Playlist p = service.createPlaylist(map.getOrDefault("username",""), map.getOrDefault("name",""));
            sendJson(exchange,200,p.toJson());
        });

        server.createContext("/api/playlists", (exchange)->{
            String list = "[" + service.allPlaylists().stream().map(Playlist::toJson).collect(Collectors.joining(",")) + "]";
            sendJson(exchange,200,list);
        });

        server.createContext("/api/playlist", (exchange)->{
            String query = exchange.getRequestURI().getQuery();
            int id = Integer.parseInt(parseQuery(query).getOrDefault("id","0"));
            Playlist p = service.getPlaylist(id);
            if (p==null) sendJson(exchange,404,"{\"ok\":false}"); else sendJson(exchange,200,p.toJson());
        });

        server.createContext("/api/removeFromPlaylist", (exchange)->{
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,200,"{\"ok\":false}"); return; }
            String body = readBody(exchange);
            Map<String,String> map = parseJson(body);
            boolean ok = service.removeFromPlaylist(Integer.parseInt(map.getOrDefault("playlistId","0")), Integer.parseInt(map.getOrDefault("songId","0")));
            sendJson(exchange,200,"{\"ok\":"+ok+"}");
        });

        server.createContext("/api/deletePlaylist", (exchange)->{
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,200,"{\"ok\":false}"); return; }
            String body = readBody(exchange);
            Map<String,String> map = parseJson(body);
            boolean ok = service.deletePlaylist(Integer.parseInt(map.getOrDefault("playlistId","0")), map.getOrDefault("username",""));
            sendJson(exchange,200,"{\"ok\":"+ok+"}");
        });

        server.createContext("/api/userdata", (exchange)->{
            String query = exchange.getRequestURI().getQuery();
            String user = parseQuery(query).getOrDefault("username","");
            String resp = service.getUserData(user);
            sendJson(exchange,200,resp);
        });

        server.createContext("/api/addToPlaylist", (exchange)->{
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,200,"{\"ok\":false}"); return; }
            String body = readBody(exchange);
            Map<String,String> map = parseJson(body);
            boolean ok = service.addToPlaylist(Integer.parseInt(map.getOrDefault("playlistId","0")), Integer.parseInt(map.getOrDefault("songId","0")));
            sendJson(exchange,200,"{\"ok\":"+ok+"}");
        });

        server.createContext("/api/trending", (exchange)->{
            List<String> arr = service.trending().stream().map(Song::toJson).collect(Collectors.toList());
            sendJson(exchange,200,"["+String.join(",",arr)+"]");
        });

        server.createContext("/api/friendRecommendations", (exchange)->{
            String query = exchange.getRequestURI().getQuery();
            String user = parseQuery(query).getOrDefault("username","");
            List<String> arr = service.friendRecommendations(user).stream().map(Song::toJson).collect(Collectors.toList());
            sendJson(exchange,200,"["+String.join(",",arr)+"]");
        });

        server.createContext("/api/musictwin", (exchange)->{
            String query = exchange.getRequestURI().getQuery();
            String user = parseQuery(query).getOrDefault("username","");
            String result = service.musicTwinWithReason(user);
            sendJson(exchange,200,result);
        });

        server.createContext("/api/recentlyPlayed", (exchange)->{
            String query = exchange.getRequestURI().getQuery();
            String user = parseQuery(query).getOrDefault("username","");
            List<String> arr = service.recentlyPlayed(user).stream().map(Song::toJson).collect(Collectors.toList());
            sendJson(exchange,200,"["+String.join(",",arr)+"]");
        });

        server.createContext("/api/stats", (exchange)->{
            String query = exchange.getRequestURI().getQuery();
            String user = parseQuery(query).getOrDefault("username","");
            String result = service.getUserStats(user);
            sendJson(exchange,200,result);
        });

        server.createContext("/api/popularArtists", (exchange)->{
            List<String> arr = service.popularArtists().stream().map(Song::toJson).collect(Collectors.toList());
            sendJson(exchange,200,"["+String.join(",",arr)+"]");
        });

        server.setExecutor(null);
        System.out.println("Server running at http://localhost:3000");
        server.start();
    }

    static class StaticHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                sendFile(exchange, "frontend/index.html", "text/html");
                return;
            }
            if (path.startsWith("/assets/songs/") && path.endsWith(".mp3")) {
                String name = path.substring(path.lastIndexOf('/')+1);
                File base64file = new File("frontend/assets/songs/"+name+".base64");
                if (base64file.exists()) {
                    byte[] b64 = Files.readAllBytes(base64file.toPath());
                    byte[] audio = Base64.getDecoder().decode(new String(b64).trim());
                    Headers h = exchange.getResponseHeaders();
                    h.add("Content-Type","audio/mpeg");
                    exchange.sendResponseHeaders(200, audio.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(audio);
                    os.close();
                    return;
                }
            }
            // serve other static files
            String local = "frontend" + path;
            File f = new File(local);
            if (f.exists() && f.isFile()) {
                String type = Files.probeContentType(f.toPath());
                if (type==null) type = "application/octet-stream";
                sendFile(exchange, local, type);
                return;
            }
            exchange.sendResponseHeaders(404, -1);
        }
    }

    static void sendFile(HttpExchange exchange, String localPath, String contentType) throws IOException {
        File f = new File(localPath);
        byte[] bytes = Files.readAllBytes(f.toPath());
        Headers h = exchange.getResponseHeaders();
        h.add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    static void sendJson(HttpExchange exchange, int code, String body) throws IOException {
        Headers h = exchange.getResponseHeaders();
        h.add("Content-Type","application/json");
        byte[] b = body.getBytes("UTF-8");
        exchange.sendResponseHeaders(code, b.length);
        OutputStream os = exchange.getResponseBody();
        os.write(b);
        os.close();
    }

    static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    static Map<String,String> parseJson(String s) {
        Map<String,String> map = new HashMap<>();
        s = s.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0,s.length()-1);
        String[] parts = s.split(",");
        for (String p: parts) {
            String[] kv = p.split(":",2);
            if (kv.length<2) continue;
            String k = kv[0].trim().replace("\"", "");
            String v = kv[1].trim();
            if (v.startsWith("\"")) v = v.substring(1);
            if (v.endsWith("\"")) v = v.substring(0,v.length()-1);
            map.put(k,v);
        }
        return map;
    }

    static Map<String,String> parseQuery(String q) {
        Map<String,String> map = new HashMap<>();
        if (q==null) return map;
        for (String part: q.split("&")) {
            String[] kv = part.split("=",2);
            if (kv.length==2) map.put(kv[0], kv[1]);
        }
        return map;
    }
}
