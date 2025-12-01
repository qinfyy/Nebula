package emu.nebula.game.character;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;
import emu.nebula.Nebula;
import emu.nebula.data.resources.ChatDef;
import emu.nebula.game.achievement.AchievementCondition;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.Contacts;
import emu.nebula.proto.Public.UI32;
import lombok.Getter;
import us.hebi.quickbuf.RepeatedInt;

@Getter
@Entity(useDiscriminator = false)
public class CharacterContact {
    private transient GameCharacter character;
    
    private boolean top;
    private long triggerTime;
    private Map<Integer, CharacterChat> chats;

    @Deprecated // Morphia only
    public CharacterContact() {
        
    }
    
    public CharacterContact(GameCharacter character) {
        this.character = character;
        this.chats = new HashMap<>();
        this.triggerTime = character.getCreateTime();
        
        // Get starter chat
        for (var chatData : character.getData().getChats()) {
            // Skip chats that have a requirement
            if (chatData.getPreChatId() != 0) {
                continue;
            }
            
            // Add chat for character
            var chat = new CharacterChat(chatData);
            this.getChats().put(chat.getId(), chat);
        }
    }
    
    public void setCharacter(GameCharacter character) {
        this.character = character;
    }
    
    public void addNextChat() {
        // Find next chat data
        ChatDef nextChatData = null;
        
        for (var chatData : getCharacter().getData().getChats()) {
            // Skip chats that we have added
            if (this.getChats().containsKey(chatData.getId())) {
                continue;
            }
            
            nextChatData = chatData;
            break;
        }
        
        // Skip if we cant find any new chat data
        if (nextChatData == null) {
            return;
        }
        
        // Add chat for character
        var chat = new CharacterChat(nextChatData);
        this.getChats().put(chat.getId(), chat);
        
        // Send packet
        this.getCharacter().getPlayer().addNextPackage(
            NetMsgId.phone_chat_change_notify, 
            UI32.newInstance().setValue(chat.getId())
        );
    }
    
    public CharacterChat getChatById(int chatId) {
        return this.getChats().get(chatId);
    }
    
    public boolean hasNew() {
        return this.getChats().values()
                .stream()
                .anyMatch(c -> !c.isEnd());
    }
    
    public PlayerChangeInfo report(ChatDef chatData, int process, RepeatedInt options, boolean end) {
        // Get chat
        var chat = this.getChatById(chatData.getId());
        if (chat == null) {
            return null;
        }
        
        // Player change info
        var change = new PlayerChangeInfo();
        
        // Sanity check
        if (chat.isEnd()) {
            return change;
        }
        
        // Report
        chat.report(process, options, end);
        
        // Set trigger time
        this.triggerTime = Nebula.getCurrentTime();
        
        // Add next chat
        this.addNextChat();
        
        // TODO save more efficiently
        this.getCharacter().save();
        
        // Add rewards
        if (chat.isEnd()) {
            getCharacter().getPlayer().getInventory().addItem(
                    chatData.getReward1(), 
                    chatData.getRewardQty1(),
                    change
            );
        }
        
        // Trigger quest/achievement
        this.getCharacter().getPlayer().trigger(AchievementCondition.ChatTotal, 1);
        
        // Success
        return change.setSuccess(true);
    }
    
    public void toggleTop() {
        // Toggle
        this.top = !this.top;
        
        // TODO save more efficiently
        this.getCharacter().save();
    }
    
    // Proto
    
    public Contacts toProto() {
        var proto = Contacts.newInstance()
                .setCharId(this.getCharacter().getCharId())
                .setTop(this.isTop())
                .setTriggerTime(this.getTriggerTime());
        
        for (var chat : this.getChats().values()) {
            proto.addChats(chat.toProto());
        }
        
        return proto;
    }
}
