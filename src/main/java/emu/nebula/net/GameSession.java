package emu.nebula.net;

import java.security.MessageDigest;
import java.util.Base64;

import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;

import emu.nebula.Nebula;
import emu.nebula.game.GameContext;
import emu.nebula.game.account.Account;
import emu.nebula.game.account.AccountHelper;
import emu.nebula.game.player.Player;
import emu.nebula.proto.Public.MailState;
import emu.nebula.util.AeadHelper;
import emu.nebula.util.Utils;
import lombok.Getter;
import lombok.SneakyThrows;
import us.hebi.quickbuf.ProtoMessage;
import us.hebi.quickbuf.RepeatedByte;

@Getter
public class GameSession {
    private String token;
    private Account account;
    private Player player;
    
    // Crypto
    private int encryptMethod; // 0 = gcm, 1 = chacha20
    private byte[] clientPublicKey;
    private byte[] serverPublicKey;
    private byte[] serverPrivateKey;
    private byte[] key;
    
    //
    private long lastActiveTime;
    
    public GameSession() {
        this.updateLastActiveTime();
    }
    
    public void setPlayer(Player player) {
        this.player = player;
        this.player.addSession(this);
    }
    
    public void clearPlayer(GameContext context) {
        // Sanity check
        if (this.player == null) {
            return;
        }
        
        // Clear player
        var player = this.player;
        this.player = null;
        
        // Remove session from player
        player.removeSession(this);
        
        // Clean up from player module
        if (!player.hasSessions()) {
            context.getPlayerModule().removeFromCache(player);
        }
    }

    public boolean hasPlayer() {
        return this.player != null;
    }
    
    // Encryption

    public void setClientKey(RepeatedByte key) {
        this.clientPublicKey = key.toArray();
    }
    
    public void generateServerKey() {
        var pair = AeadHelper.generateECDHKEyPair();
        
        this.serverPrivateKey = ((ECPrivateKeyParameters) pair.getPrivate()).getD().toByteArray();
        this.serverPublicKey = ((ECPublicKeyParameters) pair.getPublic()).getQ().getEncoded(false);
    }
    
    public void calculateKey() {
        this.key = AeadHelper.generateKey(clientPublicKey, serverPublicKey, serverPrivateKey);
        this.encryptMethod = Utils.randomRange(0, 1);
    }
    
    public String generateToken() {
        String temp = System.currentTimeMillis() + ":" +  AeadHelper.generateBytes(64).toString();
        
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(temp.getBytes());
            
            this.token = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            this.token = Base64.getEncoder().encodeToString(temp.getBytes());
        }
        
        return this.token;
    }
    
    // Login
    
    public boolean login(String loginToken) {
        // Sanity check
        if (this.account != null) {
            return false;
        }
        
        // Get account
        this.account = AccountHelper.getAccountByLoginToken(loginToken);
        
        if (account == null) {
            return false;
        }
        
        // Note: We should cache players in case multiple sessions try to login to the same player at the time
        // Get player by account
        var player = Nebula.getGameContext().getPlayerModule().getPlayerByAccount(account);
        
        // Skip intro
        if (player == null && Nebula.getConfig().getServerOptions().skipIntro) {
            player = Nebula.getGameContext().getPlayerModule().createPlayer(this, "Test", false);
        }
        
        // Set player
        if (player != null) {
            this.setPlayer(player);
        }
        
        return true;
    }
    
    public void updateLastActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }
    
    // Packet encoding helper functions
    
    public byte[] encodeMsg(int msgId, byte[] packet) {
        return PacketHelper.encodeMsg(msgId, packet);
    }

    @SneakyThrows
    public byte[] encodeMsg(int msgId, ProtoMessage<?> proto) {
        // Add any extra data
        this.addNextPackage(proto);
        
        // Encode to message like normal
        return PacketHelper.encodeMsg(msgId, proto);
    }
    
    public byte[] encodeMsg(int msgId) {
        return PacketHelper.encodeMsg(msgId);
    }
    
    private void addNextPackage(ProtoMessage<?> proto) {
        // Sanity check and make sure proto has a "nextPackage" field
        if (this.getPlayer() == null || !PacketHelper.hasNextPackageMethod(proto)) {
            return;
        }
        
        // Update mail state flag
        if (this.getPlayer().getMailbox().isNewState()) {
            // Clear
            this.getPlayer().getMailbox().clearNewState();
            
            // Send mail state notify
            this.getPlayer().addNextPackage(
                NetMsgId.mail_state_notify, 
                MailState.newInstance().setNew(true)
            );
        }
        
        // Set next package
        if (this.getPlayer().getNextPackages().size() > 0) {
            // Set current package
            NetMsgPacket curPacket = null;
            
            // Chain link next packages
            while (getPlayer().getNextPackages().size() > 0) {
                // Make sure the current packet has a nextPackage field
                if (curPacket != null && !PacketHelper.hasNextPackageMethod(curPacket.getProto())) {
                    break;
                }
                
                // Get current package
                var nextPacket = getPlayer().getNextPackages().pop();
                
                // Set cur packet if its null
                if (curPacket == null) {
                    curPacket = nextPacket;
                    continue;
                }
                
                // Set next package
                PacketHelper.setNextPackage(nextPacket.getProto(), curPacket.toByteArray());
                
                // Update next packet
                curPacket = nextPacket;
            }
            
            // Set next package of current proto via reflection
            if (curPacket != null) {
                PacketHelper.setNextPackage(proto, curPacket.toByteArray());
            }
        }
    }
}
