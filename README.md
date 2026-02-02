🎵 BeatBond — Social Music Player

BeatBond is a full-stack Java-based social music web application that connects people through shared music taste. It combines playlist management, friend-based recommendations, trending analysis, and a unique Music Twin matching feature using efficient data structures and modular OOP design.

🚀 Features
🔐 User System

Secure user registration & login

Session-based authentication

Personal music profile

🎶 Music Player

Play songs directly in the browser

Real audio playback using HTML5 <audio>

Track play counts for analytics

📂 Playlist Management

Create custom playlists

Add / remove songs

View saved playlists anytime

❤️ Favorites

Mark songs as favorites

Quick access to liked music

👯 Music Twin (Unique Feature)

Finds users with similar music taste

Uses playlist & favorites comparison

Demonstrates use of sets, maps, and similarity logic

📈 Trending Songs

Songs ranked by play count

Dynamic trending list

🤝 Friend-Based Recommendations

Discover music your friends listen to

Suggests songs outside your library

🛠 Tech Stack
Layer	Technology
Frontend	HTML, CSS, JavaScript
Backend	Java (HTTP Server)
Architecture	REST-style APIs
Audio	HTML5 Audio Player
Data Structures	HashMap, ArrayList, HashSet
Design	Modular OOP
📁 Project Structure
BeatBond/
│
├── backend/
│   └── beatbond/
│       ├── MainServer.java
│       ├── MusicService.java
│       ├── DataStore.java
│       ├── RecommendationEngine.java
│       └── models/
│           ├── User.java
│           ├── Song.java
│           └── Playlist.java
│
├── frontend/
│   ├── index.html
│   ├── assets/
│   │   ├── styles.css
│   │   ├── app.js
│   │   └── songs/
│
└── .gitignore

How to Run the Project
1️⃣ Compile Backend
Open terminal inside project root:
javac -d out backend/beatbond/*.java backend/beatbond/models/*.java

2️⃣ Start Server
java -cp out backend.beatbond.MainServer
Server runs at:
http://localhost:3000

3️⃣ Open Frontend
Open:
frontend/index.html in your browser.

Why BeatBond Stands Out:
Unlike a basic music player, BeatBond introduces a social layer and a Music Twin algorithm that matches users based on listening behavior — making music discovery interactive and personal.
