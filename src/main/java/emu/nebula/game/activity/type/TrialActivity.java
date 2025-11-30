package emu.nebula.game.activity.type;

import dev.morphia.annotations.Entity;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.ActivityDef;
import emu.nebula.game.activity.ActivityManager;
import emu.nebula.game.activity.GameActivity;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.proto.ActivityDetail.ActivityMsg;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
@Entity
public class TrialActivity extends GameActivity {
    private IntList completed;
    
    @Deprecated // Morphia only
    public TrialActivity() {
        
    }
    
    public TrialActivity(ActivityManager manager, ActivityDef data) {
        super(manager, data);
        this.completed = new IntArrayList();
    }
    
    public PlayerChangeInfo claimReward(int groupId) {
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Make sure we haven't completed this group yet
        if (this.getCompleted().contains(groupId)) {
            return change;
        }
        
        // Get trial control
        var control = GameData.getTrialControlDataTable().get(this.getId());
        if (control == null) return change;
        
        // Get group
        var group = GameData.getTrialGroupDataTable().get(groupId);
        if (group == null) return change;
        
        // Set as completed
        this.getCompleted().add(groupId);
        
        // Save to database
        this.save();
        
        // Add rewards
        return getPlayer().getInventory().addItems(group.getRewards(), change);
    }
    
    // Proto

    @Override
    public void encodeActivityMsg(ActivityMsg msg) {
        var proto = msg.getMutableTrial();
        
        for (int id : this.getCompleted()) {
            proto.addCompletedGroupIds(id);
        }
    }

}
