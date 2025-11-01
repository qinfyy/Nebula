package emu.nebula.game;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import emu.nebula.game.gacha.GachaModule;
import emu.nebula.game.player.PlayerModule;
import emu.nebula.net.GameSession;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;

@Getter
public class GameContext {
    private final Object2ObjectMap<String, GameSession> sessions;
    
    // Modules
    private final PlayerModule playerModule;
    private final GachaModule gachaModule;
    
    // Cleanup thread
    private final Timer cleanupTimer;
    
    public GameContext() {
        this.sessions = new Object2ObjectOpenHashMap<>();
        
        this.playerModule = new PlayerModule(this);
        this.gachaModule = new GachaModule(this);
        
        this.cleanupTimer = new Timer();
        this.cleanupTimer.scheduleAtFixedRate(new CleanupTask(this), 0, TimeUnit.SECONDS.toMillis(60));
    }
    
    public synchronized GameSession getSessionByToken(String token) {
        return sessions.get(token);
    }
    
    public synchronized void addSession(GameSession session) {
        this.sessions.put(session.getToken(), session);
    }

    public synchronized void generateSessionToken(GameSession session) {
        // Remove token
        if (session.getToken() != null) {
            this.sessions.remove(session.getToken());
        }
        
        // Generate token
        String token = null;
        
        do {
            token = session.generateToken();
        } while (this.getSessions().containsKey(token));
        
        // Register session
        this.sessions.put(session.getToken(), session);
    }
    
    // TODO add timeout to config
    public synchronized void cleanupInactiveSessions() {
        var it = this.getSessions().entrySet().iterator();
        long timeout = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(600); // 10 minutes
        
        while (it.hasNext()) {
            var session = it.next().getValue();
            
            if (timeout > session.getLastActiveTime()) {
                // Remove from session map
                it.remove();
                
                // Clear player
                session.clearPlayer(this);
            }
        }
    }
    
    @Getter
    public static class CleanupTask extends TimerTask {
        private GameContext gameContext;
        
        public CleanupTask(GameContext gameContext) {
            this.gameContext = gameContext;
        }

        @Override
        public void run() {
            this.getGameContext().cleanupInactiveSessions();
        }
    }
}
