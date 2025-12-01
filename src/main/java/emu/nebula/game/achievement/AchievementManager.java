package emu.nebula.game.achievement;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.AchievementDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.Achievements;
import emu.nebula.proto.Public.Events;
import lombok.Getter;
import lombok.Setter;
import us.hebi.quickbuf.RepeatedInt;

@Getter
@Entity(value = "achievements", useDiscriminator = false)
public class AchievementManager extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    // Achievement data
    private Map<Integer, GameAchievement> achievements;
    
    @Setter
    private transient boolean queueSave;
    
    @Deprecated // Morphia only
    public AchievementManager() {
        
    }
    
    public AchievementManager(Player player) {
        super(player);
        this.uid = player.getUid();
        this.achievements = new HashMap<>();
        
        this.save();
    }
    
    public synchronized int getCompletedAchievementsCount() {
        return (int) this.getAchievements().values().stream()
                .filter(GameAchievement::isComplete)
                .count();
    }
    
    /**
     * Returns true if there are any unclaimed achievements
     */
    public synchronized boolean hasNewAchievements() {
        for (var achievement : this.getAchievements().values()) {
            if (achievement.isClaimable()) {
                return true;
            }
        }
        
        return false;
    }
    
    public synchronized GameAchievement getAchievement(AchievementDef data) {
        // Try and get achievement normally
        var achievement = this.getAchievements().get(data.getId());
        
        // Create achievement if it doesnt exist
        if (achievement == null) {
            achievement = new GameAchievement(data);
            
            this.getAchievements().put(achievement.getId(), achievement);
        }
        
        return achievement;
    }
    
    public synchronized void handleClientEvents(Events events) {
        //
        boolean hasCompleted = false;
        
        // Parse events
        for (var event : events.getList()) {
            // Check id
            if (event.getId() != 200) {
                continue;
            }
            
            // Sanity check
            if (event.getData().length() < 2) {
                continue;
            }
            
            // Get achievement id and progress
            int id = event.getData().get(1);
            int progress = event.getData().get(0);
            
            if (progress <= 0) {
                continue;
            }
            
            // Get achievement data
            var data = GameData.getAchievementDataTable().get(id);
            if (data == null) continue;
            
            // Make sure achivement can be completed by the client
            if (data.getCompleteCond() != AchievementCondition.ClientReport.getValue()) {
                continue;
            }
            
            // Get achievement
            var achievement = this.getAchievement(data);
            
            // Update achievement
            boolean changed = achievement.trigger(true, progress, 0, 0);
            
            // Only save/update on client if achievement was changed
            if (changed) {
                // Sync
                this.syncAchievement(achievement);
                
                // Set save flag
                this.queueSave = true;
                
                // Check if achievement was completed
                if (achievement.isComplete()) {
                    hasCompleted = true;
                }
            }
        }
        
        // Trigger update
        if (hasCompleted) {
            this.getPlayer().trigger(AchievementCondition.AchievementTotal, this.getCompletedAchievementsCount());
        }
    }
    
    public synchronized void trigger(int condition, int progress, int param1, int param2) {
        // Sanity check
        if (progress <= 0) {
            return;
        }
        
        // Blacklist
        if (condition == AchievementCondition.ClientReport.getValue()) {
            return;
        }
        
        // Get achievements to trigger
        var triggerList = AchievementHelper.getAchievementsByCondition(condition);
        
        if (triggerList == null) {
            return;
        }
        
        // Check what type of achievement condition this is
        boolean isTotal = AchievementHelper.isTotalAchievement(condition);
        boolean hasCompleted = false;
        
        // Parse achievements
        for (var data : triggerList) {
            // Get achievement
            var achievement = this.getAchievement(data);
            
            // Update achievement
            boolean changed = achievement.trigger(isTotal, progress, param1, param2);

            // Only save/update on client if achievement was changed
            if (changed) {
                // Sync
                this.syncAchievement(achievement);
                
                // Set save flag
                this.queueSave = true;
                
                // Check if achievement was completed
                if (achievement.isComplete()) {
                    hasCompleted = true;
                }
            }
        }
        
        // Trigger update
        if (hasCompleted) {
            this.getPlayer().trigger(AchievementCondition.AchievementTotal, this.getCompletedAchievementsCount());
        }
    }
    
    /**
     * Update this achievement on the player client
     */
    private void syncAchievement(GameAchievement achievement) {
        if (!getPlayer().hasSession()) {
            return;
        }
        
        getPlayer().addNextPackage(
            NetMsgId.achievement_change_notify, 
            achievement.toProto()
        );
    }
   
    public synchronized PlayerChangeInfo recvRewards(RepeatedInt ids) {
        // Sanity check
        if (ids.length() <= 0) {
            return null;
        }
        
        // Init variables
        var rewards = new ItemParamMap();
        
        // Claim achievements
        for (int id : ids) {
            // Get achievement
            var achievement = this.getAchievements().get(id);
            if (achievement == null) continue;
            
            // Check if we can claim this achievement
            if (achievement.isClaimable()) {
                // Claim
                achievement.setClaimed(true);
                
                // Add rewards
                rewards.add(achievement.getData().getTid1(), achievement.getData().getQty1());
                
                // Save
                this.queueSave = true;
            }
        }
        
        // Success
        return this.getPlayer().getInventory().addItems(rewards);
    }
    
    // Proto
    
    public synchronized Achievements toProto() {
        var proto = Achievements.newInstance();
        
        for (var achievement : this.getAchievements().values()) {
            proto.addList(achievement.toProto());
        }
        
        return proto;
    }
    
    // Database
    
    @Override
    public void save() {
        Nebula.getGameDatabase().save(this);
        this.queueSave = false;
    }
}
