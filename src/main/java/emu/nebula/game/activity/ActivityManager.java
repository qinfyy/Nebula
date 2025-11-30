package emu.nebula.game.activity;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.activity.type.*;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Entity(value = "activity", useDiscriminator = false)
public class ActivityManager extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    // Achievement data
    private Map<Integer, GameActivity> activities;
    
    @Setter
    private transient boolean queueSave;
    
    @Deprecated // Morphia only
    public ActivityManager() {
        
    }
    
    public ActivityManager(Player player) {
        super(player);
        this.uid = player.getUid();
        this.activities = new HashMap<>();
    }
    
    public <T extends GameActivity> T getActivity(Class<T> activityClass, int id) {
        // Get activity first
        var activity = this.getActivities().get(id);
        if (activity == null) return null;
        
        // Check activity type
        if (activityClass.isInstance(activity)) {
            return activityClass.cast(activity);
        }
        
        // Failure
        return null;
    }
    
    /**
     * This needs to be called after being loaded from the database
     */
    public synchronized void init() {
        // Check if any activities
        var it = this.getActivities().entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            var activity = entry.getValue();
            
            // Validate the activity to make sure it exists
            var data = GameData.getActivityDataTable().get(activity.getId());
            if (data == null) {
                it.remove();
                this.queueSave = true;
            }
            
            // Set data
            activity.setData(data);
            activity.setManager(this);
        }
        
        // Load activities
        for (var id : Nebula.getGameContext().getActivityModule().getActivities()) {
            // Check if we already have this activity
            if (this.getActivities().containsKey(id)) {
                continue;
            }
            
            // Create activity
            var activity = this.createActivity(id);
            
            if (activity == null) {
                continue;
            }
            
            // Add activity
            this.getActivities().put(id, activity);
            
            // Set save flag
            this.queueSave = true;
        }
        
        // Save if any activities were changed
        if (this.queueSave) {
            this.save();
        }
    }
    
    private GameActivity createActivity(int id) {
        // Get activity data first
        var data = GameData.getActivityDataTable().get(id);
        if (data == null) {
            return null;
        }
        
        GameActivity activity = switch (data.getType()) {
            case Trial -> new TrialActivity(this, data);
            default -> null;
        };
        
        return activity;
    }

    // Database
    
    @Override
    public void save() {
        Nebula.getGameDatabase().save(this);
        this.queueSave = false;
    }
}
