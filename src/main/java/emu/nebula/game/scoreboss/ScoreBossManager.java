package emu.nebula.game.scoreboss;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.ScoreBossControlDef;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerManager;
import lombok.Getter;

@Getter
public class ScoreBossManager extends PlayerManager  {
    private int levelId;
    private long buildId;
    
    private boolean checkedDatabase;
    private ScoreBossRankEntry ranking;
    
    public ScoreBossManager(Player player) {
        super(player);
    }
    
    public int getControlId() {
        return Nebula.getGameContext().getScoreBossModule().getControlId();
    }
    
    public ScoreBossControlDef getControlData() {
        return GameData.getScoreBossControlDataTable().get(this.getControlId());
    }
    
    public ScoreBossRankEntry getRanking() {
        if (this.ranking == null && !this.checkedDatabase) {
            this.ranking = Nebula.getGameDatabase().getObjectByUid(ScoreBossRankEntry.class, this.getPlayerUid());
            this.checkedDatabase = true;
        }
        
        return this.ranking;
    }
    
    public boolean apply(int levelId, long buildId) {
        // Get level
        var control = getControlData();
        if (control == null || !control.getLevelGroup().contains(levelId)) {
            return false;
        }
        
        // Get build
        var build = this.getPlayer().getStarTowerManager().getBuildById(buildId);
        if (build == null) {
            return false;
        }
        
        // Set
        this.levelId = levelId;
        this.buildId = buildId;
        
        // Success
        return true;
    }

    public boolean settle(int stars, int score) {
        // Get level
        var control = getControlData();
        if (control == null || !control.getLevelGroup().contains(this.getLevelId())) {
            return false;
        }
        
        // Get build
        var build = getPlayer().getStarTowerManager().getBuildById(this.getBuildId());
        if (build == null) {
            return false;
        }
        
        // Get ranking from database
        this.getRanking();
        
        // Create ranking if its not in the database
        if (this.ranking == null) {
            this.ranking = new ScoreBossRankEntry(this.getPlayer(), this.getControlId());
        }
        
        // Settle
        this.ranking.settle(this.getPlayer(), build, getControlId(), getLevelId(), stars, score);
        
        // Save ranking
        this.ranking.save();
        
        // Clear
        this.levelId = 0;
        this.buildId = 0;
        
        // Success
        return true;
    }
}
