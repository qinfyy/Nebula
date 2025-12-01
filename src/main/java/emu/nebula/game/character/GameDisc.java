package emu.nebula.game.character;

import org.bson.types.ObjectId;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.DiscDef;
import emu.nebula.data.resources.SubNoteSkillPromoteGroupDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.quest.QuestCondition;
import emu.nebula.proto.Public.Disc;
import emu.nebula.proto.PublicStarTower.StarTowerDisc;

import lombok.Getter;

@Getter
@Entity(value = "discs", useDiscriminator = false)
public class GameDisc implements GameDatabaseObject {
    @Id 
    private ObjectId uid;
    @Indexed
    private int playerUid;
    
    private transient DiscDef data;
    private transient Player player;
    
    private int discId;
    private int level;
    private int exp;
    private int phase;
    private int star;
    
    private boolean read;
    private boolean avg;
    
    private long createTime;
    
    @Deprecated // Morphia only!
    public GameDisc() {
        
    }
    
    public GameDisc(Player player, int discId) {
        this(player, GameData.getDiscDataTable().get(discId));
    }
    
    public GameDisc(Player player, DiscDef data) {
        this.player = player;
        this.playerUid = player.getUid();
        this.data = data;
        this.discId = data.getId();
        this.level = 1;
        this.createTime = Nebula.getCurrentTime();
    }
    
    public void setPlayer(Player player) {
        this.player = player;
    }
    
    public void setData(DiscDef data) {
        if (this.data == null && data.getId() == this.getDiscId()) {
            this.data = data;
        }
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public void setPhase(int phase) {
        this.phase = phase;
    }
    
    public void setStar(int star) {
        this.star = star;
    }
    
    public SubNoteSkillPromoteGroupDef getSubNoteSkillDef() {
        int id = (this.getData().getSubNoteSkillGroupId() * 100) + this.getPhase();
        return GameData.getSubNoteSkillPromoteGroupDataTable().get(id);
    }
    
    public int getMaxGainableExp() {
        if (this.getLevel() >= this.getMaxLevel()) {
            return 0;
        }
        
        int maxLevel = this.getMaxLevel();
        int max = 0;
        
        for (int i = this.getLevel() + 1; i <= maxLevel; i++) {
            int dataId = (this.getData().getStrengthenGroupId() * 1000) + i;
            var data = GameData.getDiscStrengthenDataTable().get(dataId);
            
            if (data != null) {
                max += data.getExp();
            }
        }
        
        return Math.max(max - this.getExp(), 0);
    }
    
    public int getMaxExp() {
        if (this.getLevel() >= this.getMaxLevel()) {
            return 0;
        }
        
        int dataId = (this.getData().getStrengthenGroupId() * 1000) + (this.level + 1);
        var data = GameData.getDiscStrengthenDataTable().get(dataId);
        return data != null ? data.getExp() : 0;
    }

    public int getMaxLevel() {
        return 10 + (this.getPhase() * 10);
    }
    
    public void addExp(int amount) {
        // Setup
        int expRequired = this.getMaxExp();
        int oldLevel = this.getLevel();

        // Add exp
        this.exp += amount;

        // Check for level ups
        while (this.exp >= expRequired && expRequired > 0) {
            this.level += 1;
            this.exp -= expRequired;
            
            expRequired = this.getMaxExp();
        }
        
        // Clamp exp
        if (this.getLevel() >= this.getMaxLevel()) {
            this.exp = 0;
        }
        
        // Check if we leveled up
        if (this.level > oldLevel) {
            // Trigger quest
            this.getPlayer().trigger(QuestCondition.DiscStrengthenTotal, this.level - oldLevel);
        }
        
        // Save to database
        this.save();
    }
    
    // Handlers
    
    public PlayerChangeInfo upgrade(ItemParamMap params) {
        // Calculate exp gained
        int exp = 0;
        
        // Check if item is an exp item
        for (var entry : params.entries()) {
            var data = GameData.getDiscItemExpDataTable().get(entry.getIntKey());
            if (data == null) return null;
            
            exp += data.getExp() * entry.getIntValue();
        }
        
        // Clamp exp gain
        exp = Math.min(this.getMaxGainableExp(), exp);
        
        // Calculate gold required
        params.add(GameConstants.GOLD_ITEM_ID, (int) Math.ceil(exp * 0.25D));
        
        // Verify that the player has the items
        if (!this.getPlayer().getInventory().hasItems(params)) {
            return null;
        }
        
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Remove items
        this.getPlayer().getInventory().removeItems(params, change);
        
        // Add exp
        this.addExp(exp);
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo promote() {
        // TODO check player level to make sure they can advance this disc
        
        // Get promote data
        int phaseId = (this.getData().getPromoteGroupId() * 1000) + (this.phase + 1);
        var data = GameData.getDiscPromoteDataTable().get(phaseId);
        
        if (data == null) {
            return null;
        }
        
        // Verify that the player has the items
        if (!this.getPlayer().getInventory().hasItems(data.getMaterials())) {
            return null;
        }
        
        // Remove items
        var change = this.getPlayer().getInventory().removeItems(data.getMaterials(), null);
        
        // Add phase level
        this.phase++;
        
        // Save to database
        this.save();
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo limitBreak(int count) {
        // Sanity check
        if (count <= 0) {
            return null;
        }
        
        // Create params with limit break items
        var materials = new ItemParamMap();
        materials.add(this.getData().getTransformItemId(), count);
        
        // Verify that the player has the items
        if (!this.getPlayer().getInventory().hasItems(materials)) {
            return null;
        }
        
        // Remove items
        var change = this.getPlayer().getInventory().removeItems(materials, null);
        
        // Add star
        int old = this.star;
        this.star = Math.max(this.star + count, 5);
        
        // Save to database if star count changed
        if (this.star != old) {
            this.save();
        }
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo receiveReadReward(int readType) {
        // Sanity check
        if (readType == 1) {
            if (this.isRead()) {
                return null;
            }
        } else if (readType == 2) {
            if (this.isAvg()) {
                return null;
            }
        } else {
            return null;
        }
        
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Add reward
        if (this.getData().getReadReward() != null) {
            int id = getData().getReadReward()[0];
            int count = getData().getReadReward()[1];
            
            this.getPlayer().getInventory().addItem(id, count, change);
        }
        
        // Set flag
        if (readType == 1) {
            this.read = true;
        } else if (readType == 2) {
            this.avg = true;
        }
        
        // Save to database
        this.save();
        
        // Success
        return change;
    }
    
    // Proto
    
    public Disc toProto() {
        var proto = Disc.newInstance()
                .setId(this.getDiscId())
                .setLevel(this.getLevel())
                .setExp(this.getExp())
                .setPhase(this.getPhase())
                .setStar(this.getStar())
                .setRead(this.isRead())
                .setAvg(this.isAvg())
                .setCreateTime(this.getCreateTime());
        
        return proto;
    }
    
    public StarTowerDisc toStarTowerProto() {
        var proto = StarTowerDisc.newInstance()
                .setId(this.getDiscId())
                .setLevel(this.getLevel())
                .setPhase(this.getPhase())
                .setStar(this.getStar());
        
        return proto;
    }
}
