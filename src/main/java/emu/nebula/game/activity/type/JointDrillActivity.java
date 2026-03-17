package emu.nebula.game.activity.type;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dev.morphia.annotations.Entity;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.ActivityDef;
import emu.nebula.data.resources.JointDrill2LevelDef;
import emu.nebula.game.activity.ActivityManager;
import emu.nebula.game.activity.GameActivity;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.jointdrill.JointDrillBuild;
import emu.nebula.game.jointdrill.JointDrillRankEntry;
import emu.nebula.game.jointdrill.JointDrillScore;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.tower.StarTowerBuild;
import emu.nebula.proto.ActivityDetail.ActivityMsg;
import emu.nebula.proto.PublicJointDrill.JointDrillLevel;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.Getter;

@Getter
@Entity
public class JointDrillActivity extends GameActivity {
    private Int2IntMap passedLevels;
    
    // Current level
    private transient JointDrill2LevelDef level;
    private transient StarTowerBuild build;
    private transient boolean simulate;
    
    private transient List<JointDrillBuild> teams;
    
    // Ranking
    private transient boolean checkedDatabase;
    private transient JointDrillRankEntry ranking;
    
    @Deprecated // Morphia only
    public JointDrillActivity() {
        this.teams = new ArrayList<>();
    }
    
    public JointDrillActivity(ActivityManager manager, ActivityDef data) {
        super(manager, data);
        
        // Give the player starting amount of tickets
        int tickets = this.getPlayer().getInventory().getResourceCount(GameConstants.JOINT_DRILL_TICKET_ID);
        if (tickets < 3) {
            this.getPlayer().getInventory().addItem(GameConstants.JOINT_DRILL_TICKET_ID, 3 - tickets);
        }
    }
    
    public Int2IntMap getPassedLevels() {
        if (this.passedLevels == null) {
            this.passedLevels = new Int2IntOpenHashMap();
        }
        
        return this.passedLevels;
    }
    
    public long getStartTime() {
        // Force activity to be always open for 6+ days
        return Nebula.getCurrentServerTime() - TimeUnit.DAYS.toSeconds(1);
    }
    
    public void reset() {
        this.level = null;
        this.build = null;
        this.teams.clear();
    }
    
    // Joint drill
    
    public JointDrillRankEntry getRankEntry() {
        if (this.ranking == null && !this.checkedDatabase) {
            this.ranking = Nebula.getGameDatabase().getObjectByUid(JointDrillRankEntry.class, this.getPlayer().getUid());
            this.checkedDatabase = true;
        }
        
        return this.ranking;
    }

    public synchronized boolean inProgress() {
        return this.level != null;
    }
    
    public synchronized PlayerChangeInfo apply(int levelId, long buildId, boolean simulate) {
        // Get level and record used
        var level = GameData.getJointDrill2LevelDataTable().get(levelId);
        var build = this.getPlayer().getStarTowerManager().getBuildById(buildId);
        
        // Verify that level and record exists
        if (level == null || build == null) {
            return null;
        }
        
        // Create result
        PlayerChangeInfo change = null;
        
        // Consume ticket IF not simulated
        if (!simulate) {
            // Make sure player has a ticket
            if (!getPlayer().getInventory().hasItem(GameConstants.JOINT_DRILL_TICKET_ID, 1)) {
                return null;
            }
            
            change = getPlayer().getInventory().removeItem(GameConstants.JOINT_DRILL_TICKET_ID, 1);
        } else {
            change = new PlayerChangeInfo();
        }
        
        // Set level and build
        this.level = level;
        this.build = build;
        this.teams.clear();
        this.simulate = simulate;
        
        // Failure - No level or record found
        return change;
    }
    
    public synchronized JointDrillScore settle(int time, int damage, boolean win) {
        // Make sure we are currently in progress
        if (!this.inProgress()) {
            return null;
        }
        
        // Create score data
        var score = new JointDrillScore();
        
        // Calculate score
        if (win) {
            score.calculateScore(this.getLevel(), time);
        } else {
            // TODO calculate partial score
        }
        
        // Add joint drill team
        if (this.getBuild() != null) {
            var team = new JointDrillBuild(this.getBuild(), time, damage);
            this.getTeams().add(team);
        }
        
        // Only update if we are not simulating a challenge
        if (!this.simulate && win) {
            // Check if this is our first clear
            boolean isFirstClear = this.getPassedLevels().containsKey(this.getLevel().getId());
            
            // Get current score
            int currentScore = this.getPassedLevels().get(this.getLevel().getId());
            
            // Update score
            if (score.getTotal() >= currentScore) {
                // Update passed levels
                this.getPassedLevels().put(this.getLevel().getId(), score.getTotal());
                
                // Save activity to database
                this.save();
            }
            
            // Handle ranking
            if (this.getTeams().size() > 0) {
                // Get ranking from database
                this.getRankEntry();
                
                // Create ranking if its not in the database
                if (this.ranking == null) {
                    this.ranking = new JointDrillRankEntry(this.getPlayer(), this.getId());
                }
                
                // Settle ranking and save if we have a higher score
                if (score.getTotal() > this.ranking.getScore()) {
                    this.ranking.settle(this.getPlayer(), this.getTeams(), this.getId(), score.getTotal());
                    this.ranking.save();
                }
            }
            
            // Handle rewards
            if (isFirstClear) {
                score.getRewards().add(this.getLevel().getFirstRewards().generate());
            }
            
            score.getRewards().add(this.getLevel().getRewards().generate());
            
            // Add rewards
            this.getPlayer().getInventory().addItems(score.getRewards(), score.getChange());
        }
        
        // Reset level
        this.reset();
        
        // Finished
        return score;
    }
    
    public synchronized boolean continueDrill(long buildId) {
        // Make sure we are currently in progress
        if (!this.inProgress()) {
            return false;
        }
        
        if (this.getTeams().size() >= 3) {
            return false;
        }
        
        // Get build
        var build = this.getPlayer().getStarTowerManager().getBuildById(buildId);
        
        if (build == null) {
            return false;
        }
        
        // TODO validate build as unique
        
        // Set build
        this.build = build;
        
        // Complete
        return true;
    }
    
    public synchronized JointDrillScore giveup(int time, int damage) {
        // Make sure we are currently in progress
        if (!this.inProgress() || this.getBuild() == null) {
            return null;
        }
        
        // Create and calculate score
        var score = new JointDrillScore();
        
        // TODO calculate partial score
        
        // Log joint drill team
        var team = new JointDrillBuild(this.getBuild(), time, damage);
        this.getTeams().add(team);
        
        // Clear build
        this.build = null;
        
        // Complete
        return score;
    }
    
    public synchronized PlayerChangeInfo sweep(int levelId, int count) {
        // Sanity check
        if (count <= 0) {
            return null;
        }
        
        // Check if the player has completed this level
        if (!this.getPassedLevels().containsKey(levelId)) {
            return null;
        }
        
        // Verify that we have enough tickets
        if (!getPlayer().getInventory().hasItem(GameConstants.JOINT_DRILL_TICKET_ID, count)) {
            return null;
        }
        
        // Get level data
        var level = GameData.getJointDrill2LevelDataTable().get(levelId);
        
        if (level == null) {
            return null;
        }
        
        // Setup variables
        var change = new PlayerChangeInfo();
        var totalRewards = new ItemParamMap();
        var list = new ArrayList<>();
        
        // Consume tickets
        getPlayer().getInventory().removeItem(GameConstants.JOINT_DRILL_TICKET_ID, count, change);
        
        // Generate rewards from sweep
        for (int i = 0; i < count; i++) {
            var rewards = level.getRewards().generate();
            
            totalRewards.add(rewards);
            list.add(rewards);
        }
        
        // Add rewards to player
        getPlayer().getInventory().addItems(totalRewards, change);
        
        // Set reward list as extra data for change info
        change.setExtraData(list);
        
        // Complete
        return change;
    }
    
    // Proto

    @Override
    public void encodeActivityMsg(ActivityMsg msg) {
        var proto = msg.getMutableJointDrill();
        
        // Mark
        proto.getMutableMeta();
        proto.getMutableMode2();
        
        // Add passed levels
        if (this.passedLevels != null) {
            for (var entry : this.passedLevels.int2IntEntrySet()) {
                int levelId = entry.getIntKey();
                int score = entry.getIntValue();
                
                var info = JointDrillLevel.newInstance()
                        .setLevelId(levelId)
                        .setScore(score);
                
                proto.addPassedLevels(info);
            }
        }
    }

}
