const api = (path, opts)=>fetch(path, opts).then(r=>r.json()).catch(e=>{ console.error('API error:', e); return {ok:false}; });
let currentUser = null;
let pendingSongId = null;

function initButtons() {
  const btnReg = document.getElementById('btnReg');
  const btnLogin = document.getElementById('btnLogin');
  if (btnReg) {
    btnReg.onclick = ()=>{
      const u=document.getElementById('regUser').value, p=document.getElementById('regPass').value;
      if (!u || !p) { alert('enter username and password'); return; }
      api('/api/register',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:u,password:p})}).then(r=>{ alert(r.ok ? 'registered!' : 'register failed'); });
    };
  }
  if (btnLogin) {
    btnLogin.onclick = ()=>{
      const u=document.getElementById('loginUser').value, p=document.getElementById('loginPass').value;
      if (!u || !p) { alert('enter username and password'); return; }
      api('/api/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:u,password:p})}).then(r=>{ 
        if (r.ok) { currentUser=u; showDashboard(); setUserDisplay(); loadAll(); loadTrending(); loadFavorites(); loadFriendRecs(); loadMusicTwin(); } 
        else alert('login failed - try alice/pass or bob/pass'); 
      });
    };
  }
}

document.addEventListener('DOMContentLoaded', initButtons);

function showDashboard(){ document.getElementById('auth').style.display='none'; document.getElementById('dashboard').style.display='block'; }

function setUserDisplay(){ const d=document.getElementById('userDisplay'); if (d) d.textContent = currentUser || 'Guest'; }

function loadAll(searchTerm){ api('/api/songs').then(list=>{
  const ul=document.getElementById('allSongs'); ul.innerHTML='';
  const q = (searchTerm||'').toString().toLowerCase();
  const selectedGenre = window.selectedGenre || '';
  list.forEach(s=>{
    if (q) {
      const inTitle = s.title.toLowerCase().includes(q);
      const inArtist = s.artist.toLowerCase().includes(q);
      if (!inTitle && !inArtist) return;
    }
    if (selectedGenre && selectedGenre !== 'All' && s.genre !== selectedGenre) return;
    const li=document.createElement('li');
    li.dataset.songId = String(s.id);
    li.innerHTML = `<div style="display:flex;align-items:center;gap:12px"><div><strong>${s.title}</strong><div class=\"meta\">${s.artist} • <span class=\"muted\">${s.genre}</span></div></div><div style=\"margin-left:auto\"><button onclick=play(${s.id},'${s.filename}') class=\"btn small\">Play</button><button data-id="${s.id}" class=\"btn small like-btn\">Like</button><button onclick=addToPlaylistPrompt(${s.id}) class=\"btn small\">Add</button></div></div>`;
    ul.appendChild(li);
  });
  // Ensure like/unlike text and handlers are correct after rendering
  updateSongLikeButtons();
}); }

// Search handling
function debounce(fn, wait){ let t; return (...a)=>{ clearTimeout(t); t=setTimeout(()=>fn(...a), wait); }; }
const searchEl = document.getElementById('q');
if (searchEl) {
  const doSearch = debounce((e)=>{ loadAll(e.target.value); }, 250);
  searchEl.addEventListener('input', doSearch);
}

function loadGenres(){
  api('/api/songs').then(list=>{
    const set = new Set();
    list.forEach(s=> set.add(s.genre));
    const arr = ['All', ...Array.from(set)];
    const node = document.getElementById('genreList');
    node.innerHTML='';
    arr.forEach(g=>{
      const b = document.createElement('button');
      b.className='btn small';
      b.textContent = g;
      b.style.margin = '6px 6px 6px 0';
      b.onclick = ()=>{ window.selectedGenre = g; loadAll(document.getElementById('q').value || '');
        Array.from(node.children).forEach(c=>c.style.opacity=1); b.style.opacity=0.7; };
      node.appendChild(b);
    });
  });
}

// initialize genres after DOM
document.addEventListener('DOMContentLoaded', ()=>{ loadGenres(); });

function play(id, filename){ api('/api/play',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:currentUser,id:id})}).then(()=>{
  const player=document.getElementById('player');
  player.src = '/assets/songs/'+filename;
  player.play();
  loadTrending(); loadFavorites(); loadFriendRecs();
}); }

function like(id){ 
  console.log('like request', id, 'user', currentUser);
  api('/api/like',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:currentUser,id:id})}).then(r=>{
    console.log('like response', r);
    // update local state on success
    if (r && r.ok) {
      let likedSet = JSON.parse(localStorage.getItem('liked.'+currentUser) || '[]').map(Number);
      if (!likedSet.includes(Number(id))) likedSet.push(Number(id));
      likedSet = Array.from(new Set(likedSet));
      localStorage.setItem('liked.'+currentUser, JSON.stringify(likedSet));
    }
    loadFavorites();
    updateSongLikeButtons();
  }).catch(e=>{ console.error('like error', e); });
}

function unlike(id){
  console.log('unlike request', id, 'user', currentUser);
  api('/api/unlike',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:currentUser,id:id})}).then(r=>{
    console.log('unlike response', r);
    // Always remove locally (optimistic) so UI updates immediately
    let likedSet = JSON.parse(localStorage.getItem('liked.'+currentUser) || '[]').map(Number);
    likedSet = likedSet.filter(x=>x!==Number(id));
    localStorage.setItem('liked.'+currentUser, JSON.stringify(likedSet));
    loadFavorites();
    updateSongLikeButtons();
  }).catch(e=>{ console.error('unlike error', e);
    // on error still remove locally
    let likedSet = JSON.parse(localStorage.getItem('liked.'+currentUser) || '[]').map(Number);
    likedSet = likedSet.filter(x=>x!==Number(id));
    localStorage.setItem('liked.'+currentUser, JSON.stringify(likedSet));
    loadFavorites();
    updateSongLikeButtons();
  });
}

function updateSongLikeButtons(){
  const liked = (JSON.parse(localStorage.getItem('liked.'+currentUser) || '[]') || []).map(Number);
  document.querySelectorAll('#allSongs li').forEach(li=>{
    const likeBtn = li.querySelector('.like-btn');
    if (!likeBtn) return;
    const id = parseInt(likeBtn.dataset.id || li.dataset.songId || '0');
    if (!id) return;
    if (liked.includes(id)) { likeBtn.textContent='Unlike'; likeBtn.onclick = ()=>unlike(id); }
    else { likeBtn.textContent='Like'; likeBtn.onclick = ()=>like(id); }
  });
}

function loadTrending(){ api('/api/trending').then(list=>{
  const ul=document.getElementById('trending'); ul.innerHTML='';
  // store ONLY top 3 trending ids globally for badges in All Songs
  window.trendingTop = list.slice(0, 3).map(s=>s.id);
  list.forEach((s,idx)=>{
    const li=document.createElement('li');
    let reason = '';
    if (idx===0) reason = `Most played — ${s.playCount} plays`;
    else if (idx<3) reason = `Top trending — ${s.playCount} plays`;
    ul.appendChild(li);
  });
  // refresh All Songs badges
  updateAllTrendingBadges();
}); }

function updateAllTrendingBadges(){
  const tops = window.trendingTop || [];
  document.querySelectorAll('#allSongs li').forEach(li=>{
    const sid = parseInt(li.dataset.songId || '0');
    let existing = li.querySelector('.trend-badge');
    if (tops.includes(sid)) {
      if (!existing) {
        const node = document.createElement('div'); node.className='trend-badge'; node.textContent = 'Top Trending'; node.style.marginLeft='12px'; node.style.fontSize='12px'; node.style.color='var(--accent)'; li.querySelector('div').appendChild(node);
      }
    } else {
      if (existing) existing.remove();
    }
  });
}

function loadFavorites(){ 
  const ul=document.getElementById('favorites'); ul.innerHTML='';
  let liked = (JSON.parse(localStorage.getItem('liked.'+currentUser) || '[]') || []).map(Number);
  if (liked.length===0) { ul.textContent='No favorites yet'; return; }
  api('/api/songs').then(list=>{
    ul.innerHTML='';
    list.forEach(s=>{ if (liked.includes(s.id)) { const li=document.createElement('li'); li.textContent = `${s.title} - ${s.artist}`; ul.appendChild(li); } });
    if (ul.children.length===0) ul.textContent='No favorites yet';
  });
}

// show playlist details and allow remove
function showPlaylistDetails(playlistId){
  api('/api/playlist?id='+playlistId).then(p=>{
    const ul = document.getElementById('playlists'); ul.innerHTML='';
    const header = document.createElement('li'); header.innerHTML = `<strong>${p.name}</strong> <button class="btn small" onclick="deletePlaylist(${p.id})">Delete</button>`;
    ul.appendChild(header);
    if (!p.songs || p.songs.length===0) { const li=document.createElement('li'); li.textContent='No songs'; ul.appendChild(li); return; }
    p.songs.forEach(id=>{
      api('/api/songs').then(list=>{ const s = list.find(x=>x.id===id); if (s){ const li=document.createElement('li'); li.innerHTML = `${s.title} - ${s.artist} <button class=\"btn small\" onclick=removeFromPlaylist(${p.id},${s.id})>Remove</button>`; ul.appendChild(li);} });
    });
  });
}

function removeFromPlaylist(pid, sid){ api('/api/removeFromPlaylist',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({playlistId:pid,songId:sid})}).then(r=>{ if (r.ok) showPlaylistDetails(pid); }); }

function deletePlaylist(pid){ if (!confirm('Delete playlist?')) return; api('/api/deletePlaylist',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({playlistId:pid,username:currentUser})}).then(r=>{ if (r.ok) loadPlaylists(); }); }

function addToPlaylistPrompt(songId){ 
  pendingSongId = songId;
  const modal = document.getElementById('modalOverlay');
  const opts = document.getElementById('playlistOptions');
  opts.innerHTML = '';
  
  const pls = JSON.parse(localStorage.getItem('playlists.'+currentUser) || '[]');
  if (pls.length > 0) {
    pls.forEach(pl=>{
      const div = document.createElement('div');
      div.className = 'playlist-option';
      div.textContent = pl.name;
      div.onclick = ()=>{ addSongToExistingPlaylist(pl.id, songId); };
      opts.appendChild(div);
    });
  } else {
    opts.textContent = 'No playlists yet. Create one below.';
  }
  
  document.getElementById('newPlName').value = '';
  modal.style.display = 'flex';
}

function closeAddModal() {
  document.getElementById('modalOverlay').style.display = 'none';
  pendingSongId = null;
}

function addSongToExistingPlaylist(playlistId, songId) {
  api('/api/addToPlaylist',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({playlistId:playlistId,songId:songId})}).then(r=>{
    if (r.ok) {
      const pls = JSON.parse(localStorage.getItem('playlists.'+currentUser) || '[]');
      const pl = pls.find(p=>p.id===playlistId);
      if (pl && !pl.songs.includes(songId)) pl.songs.push(songId);
      localStorage.setItem('playlists.'+currentUser, JSON.stringify(pls));
      alert('Song added!');
      closeAddModal();
      loadPlaylists();
    }
  });
}

function createNewPlaylist() {
  const name = document.getElementById('newPlName').value;
  if (!name) { alert('Enter playlist name'); return; }
  api('/api/createPlaylist',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:currentUser,name:name})}).then(p=>{
    const pls = JSON.parse(localStorage.getItem('playlists.'+currentUser) || '[]');
    pls.push({id:p.id, name:p.name, songs:[pendingSongId]});
    localStorage.setItem('playlists.'+currentUser, JSON.stringify(pls));
    alert('Playlist created & song added!');
    closeAddModal();
    loadPlaylists();
  });
}

function loadPlaylists(){ 
  const ul=document.getElementById('playlists'); ul.innerHTML='';
  const pls = JSON.parse(localStorage.getItem('playlists.'+currentUser) || '[]');
  if (pls.length===0) { ul.textContent='No playlists yet'; return; }
  pls.forEach(pl=>{ const li=document.createElement('li'); li.innerHTML = `<div style="display:flex;align-items:center;justify-content:space-between"><span>${pl.name} (${pl.songs.length})</span><div><button class=\"btn small\" onclick=showPlaylistDetails(${pl.id})>Open</button></div></div>`; ul.appendChild(li); });
}

function loadFriendRecs(){ api('/api/friendRecommendations?username='+encodeURIComponent(currentUser)).then(list=>{
  const ul=document.getElementById('friendRecs'); ul.innerHTML='';
  list.forEach(s=>{ const li=document.createElement('li'); li.textContent = s.title; ul.appendChild(li); });
}); }

function loadMusicTwin(){ 
  const container = document.getElementById('musicTwin');
  
  api('/api/musictwin?username='+encodeURIComponent(currentUser)).then(r=>{ 
    if (r.twin && r.twin.length > 0) {
      const score = r.score || 0;
      const reason = r.reason || 'Your music match!';
      
      // Create interactive card
      const html = `
        <div class="twin-discover" onclick="revealTwin(this, '${r.twin}', '${reason}', ${score})">
          <div class="twin-discover-text">🔍 Find Your Music Twin</div>
          <div class="twin-discover-hint">Click or hover to reveal</div>
        </div>
      `;
      container.innerHTML = html;
      
      // Store data for reveal
      container.dataset.twin = r.twin;
      container.dataset.reason = reason;
      container.dataset.score = score;
      container.dataset.common = JSON.stringify(r.commonSongs || []);
      
    } else {
      container.innerHTML = '<span class="muted" style="font-size:13px">Play more songs to find your twin</span>';
    }
  }); 
}

function revealTwin(element, twin, reason, score) {
  const container = element.parentElement;
  const common = JSON.parse(container.dataset.common || '[]');
  let commonHtml = '';
  if (common && common.length>0) {
    commonHtml = '<div style="margin-top:8px;text-align:left"><strong>Common songs:</strong><ul style="margin:6px 0 0 16px">';
    common.forEach(s=>{ commonHtml += `<li style="font-size:13px;color:var(--muted)">${s}</li>`; });
    commonHtml += '</ul></div>';
  }
  const html = `
    <div class="twin-revealed">
      <div class="twin-emoji">🎵</div>
      <div class="twin-name">${twin}</div>
      <div class="twin-reason">${reason}</div>
      <div class="twin-score">Match: <strong>${score}%</strong></div>
      ${commonHtml}
    </div>
  `;
  container.innerHTML = html;
}

const createPlEl = document.getElementById('createPl');
if (createPlEl) {
  createPlEl.onclick = ()=>{ 
    const name=document.getElementById('plName').value; 
    if(!name) return; 
    api('/api/createPlaylist',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:currentUser,name:name})}).then(p=>{
      const pls = JSON.parse(localStorage.getItem('playlists.'+currentUser) || '[]');
      pls.push({id:p.id, name:p.name, songs:[]});
      localStorage.setItem('playlists.'+currentUser, JSON.stringify(pls));
      document.getElementById('plName').value='';
      loadPlaylists();
      alert('Playlist created!');
    });
  };
}

// Init player bar dynamically
function initPlayerBar() {
  if (!document.getElementById('playerBar')) {
    const playerBar = document.createElement('div'); playerBar.id='playerBar'; const audio = document.createElement('audio'); audio.id='player'; audio.controls=true; playerBar.appendChild(audio); document.body.appendChild(playerBar);
  }
}

initPlayerBar();
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initButtons);
} else {
  initButtons();
}
