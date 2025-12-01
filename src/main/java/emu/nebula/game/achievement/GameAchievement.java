package emu.nebula.game.achievement;

import dev.morphia.annotations.Entity;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.AchievementDef;
import emu.nebula.proto.Public.Achievement;
import emu.nebula.proto.Public.QuestProgress;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class GameAchievement {
    private int id;
    private int curProgress;
    private long completed;
    private boolean claimed;
    
    private transient AchievementDef data;
    
    @Deprecated // Morphia only
    public GameAchievement() {
        
    }
    
    public GameAchievement(AchievementDef data) {
        this.id = data.getId();
        this.data = data;
    }
    
    public AchievementDef getData() {
        if (this.data == null) {
            this.data = GameData.getAchievementDataTable().get(this.getId());
        }
        
        return this.data;
    }
    
    public int getMaxProgress() {
        return this.getData().getAimNumShow();
    }

    public boolean isComplete() {
        return this.completed > 0;
    }
    
    public boolean isClaimable() {
        return !this.isClaimed() && this.isComplete();
    }
    
    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }
    
    public int getStatus() {
        if (this.isClaimed()) {
            return 2;
        } else if (this.isComplete()) {
            return 1;
        }
        
        return 0;
    }
    
    /**
     * Returns true if achievement was updated
     */
    public boolean trigger(boolean isTotal, int progress, int param1, int param2) {
        // Sanity check
        if (this.isComplete()) {
            return false;
        }
        
        // Check param conditions
        var data = this.getData();
        if (data == null) return false;
        
        if ((data.hasParam1() || param1 != 0) && data.getParam1() != param1) {
            return false;
        }
        
        if ((data.hasParam2() || param2 != 0) && data.getParam2() != param2) {
            return false;
        }
        
        // Set previous progress
        int prevProgress = this.curProgress;
        
        // Update progress
        if (isTotal) {
            this.curProgress += progress;
        } else {
            this.curProgress = progress;
        }
        
        // Check if completed
        if (this.getCurProgress() >= this.getMaxProgress()) {
            this.curProgress = this.getMaxProgress();
            this.completed = Nebula.getCurrentTime();
            return true;
        }
        
        // Check if progress was changed
        return prevProgress != this.curProgress;
    }
    
    // Proto
    
    public Achievement toProto() {
        var progress = QuestProgress.newInstance()
                .setCur(this.getCurProgress())
                .setMax(this.getMaxProgress());
        
        var proto = Achievement.newInstance()
                .setId(this.getId())
                .setCompleted(this.getCompleted())
                .setStatus(this.getStatus())
                .addProgress(progress);
        
        return proto;
    }
}
