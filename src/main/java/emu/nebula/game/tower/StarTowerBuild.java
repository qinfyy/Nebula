package emu.nebula.game.tower;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.SecondarySkillDef;
import emu.nebula.data.resources.StarTowerBuildRankDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.game.character.GameDisc;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.game.player.Player;
import emu.nebula.proto.Public.ItemTpl;
import emu.nebula.proto.PublicStarTower.BuildPotential;
import emu.nebula.proto.PublicStarTower.StarTowerBuildBrief;
import emu.nebula.proto.PublicStarTower.StarTowerBuildDetail;
import emu.nebula.proto.PublicStarTower.StarTowerBuildInfo;
import emu.nebula.proto.PublicStarTower.TowerBuildChar;
import emu.nebula.util.Snowflake;

import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;

@Getter
@Entity(value = "builds", useDiscriminator = false)
public class StarTowerBuild implements GameDatabaseObject {
    @Id
    private int uid;
    @Indexed
    private int playerUid;
    
    private String name;
    private boolean lock;
    private boolean preference;
    private int score;
    
    private int[] charIds;
    private int[] discIds;
    
    private ItemParamMap charPots;
    private ItemParamMap potentials;
    private ItemParamMap subNoteSkills;
    
    private IntSet secondarySkills;
    
    @Deprecated
    public StarTowerBuild() {
        // Morphia only
    }
    
    public StarTowerBuild(Player player) {
        this.uid = Snowflake.newUid();
        this.playerUid = player.getUid();
        this.name = "";
        this.charPots = new ItemParamMap();
        this.potentials = new ItemParamMap();
        this.subNoteSkills = new ItemParamMap();
    }
    
    public StarTowerBuild(StarTowerGame game) {
        // Initialize basic variables
        this(game.getPlayer());
        
        // Set char/disc ids
        this.charIds = game.getCharIds();
        this.discIds = game.getDiscIds();
        
        // Add potentials
        for (var entry : game.getPotentials()) {
            // Get potential data
            int id = entry.getIntKey();
            int level = entry.getIntValue();
            
            // Add to potential map
            this.getPotentials().put(id, level);
        }
        
        // Add sub note skills
        for (var entry : game.getItems()) {
            this.getSubNoteSkills().add(entry.getIntKey(), entry.getIntValue());
        }
        
        // Set secondary skills
        this.secondarySkills = game.getSecondarySkills();
        
        // Caclulate record score and cache it
        this.calculateScore();
    }
    
    public void setChars(List<GameCharacter> characters) {
        this.charIds = characters.stream()
                .mapToInt(c -> c.getCharId())
                .toArray();
    }
    
    public void setDiscs(List<GameDisc> discs) {
        this.discIds = discs.stream()
                .mapToInt(d -> d.getDiscId())
                .toArray();
    }

    public void setName(String newName) {
        // Clamp name length to prevent long names
        if (newName.length() > 32) {
            newName = newName.substring(0, 31);
        }
        
        this.name = newName;
        Nebula.getGameDatabase().update(this, this.getUid(), "name", this.getName());
    }
    
    public void setLock(boolean state) {
        // Skip if no change detected
        if (this.lock == state) {
            return;
        }
        
        // Set locked state
        this.lock = state;
        Nebula.getGameDatabase().update(this, this.getUid(), "lock", this.isLock());
    }
    
    public void setPreference(boolean state) {
        // Skip if no change detected
        if (this.preference == state) {
            return;
        }
        
        // Set preference (favorite toggle)
        this.preference = state;
        Nebula.getGameDatabase().update(this, this.getUid(), "preference", this.isPreference());
    }
    
    // Score
    
    public int calculateScore() {
        // Clear
        this.score = 0;
        this.getCharPots().clear();
        
        // Add score from potentials
        for (var potential : this.getPotentials()) {
            var data = GameData.getPotentialDataTable().get(potential.getIntKey());
            if (data == null) continue;
            
            // Add score
            this.score += data.getBuildScore(potential.getIntValue());
            
            // Add for character potential count
            this.getCharPots().add(data.getCharId(), potential.getIntValue());
        }
        
        // Add score from sub note skills
        for (var item : this.getSubNoteSkills()) {
            this.score += item.getIntValue() * 15;
        }
        
        // Check secondary skills
        if (this.getSecondarySkills() == null) {
            this.secondarySkills = SecondarySkillDef.calculateSecondarySkills(this.getDiscIds(), this.getSubNoteSkills());
        }
        
        // Add score from secondary skills
        for (int id : this.getSecondarySkills()) {
            var data = GameData.getSecondarySkillDataTable().get(id);
            if (data == null) continue;
            
            this.score += data.getScore();
        }
        
        // Complete
        return this.score;
    }
    
    public StarTowerBuildRankDef getRank() {
        StarTowerBuildRankDef rank = null;
        
        // TODO optimize
        for (var data : GameData.getStarTowerBuildRankDataTable()) {
            if (this.getScore() >= data.getMinGrade()) {
                rank = data;
            }
        }
        
        return rank;
    }
    
    // Proto
    
    public StarTowerBuildInfo toProto() {
        var proto = StarTowerBuildInfo.newInstance()
                .setBrief(this.toBriefProto())
                .setDetail(this.toDetailProto());

        return proto;
    }

    public StarTowerBuildBrief toBriefProto() {
        var proto = StarTowerBuildBrief.newInstance()
                .setId(this.getUid())
                .setName(this.getName())
                .setLock(this.isLock())
                .setPreference(this.isPreference())
                .setScore(this.getScore())
                .addAllDiscIds(this.getDiscIds());
        
        // Add characters
        for (int charId : this.getCharIds()) {
            var charProto = TowerBuildChar.newInstance()
                    .setCharId(charId)
                    .setPotentialCnt(this.getCharPots().get(charId));
            
            proto.addChars(charProto);
        }
        
        return proto;
    }
    
    public StarTowerBuildDetail toDetailProto() {
        var proto = StarTowerBuildDetail.newInstance();
        
        // Potentials
        for (var entry : this.getPotentials().int2IntEntrySet()) {
            var potential = BuildPotential.newInstance()
                    .setPotentialId(entry.getIntKey())
                    .setLevel(entry.getIntValue());
            
            proto.getMutablePotentials().add(potential);
        }
        
        // Sub note skills
        for (var entry : this.getSubNoteSkills().int2IntEntrySet()) {
            var skill = ItemTpl.newInstance()
                    .setTid(entry.getIntKey())
                    .setQty(entry.getIntValue());
            
            proto.addSubNoteSkills(skill);
        }
        
        // Secondary skills
        for (int id : this.getSecondarySkills()) {
            proto.addActiveSecondaryIds(id);
        }
        
        return proto;
    }
    
    // Database

    public void delete() {
        Nebula.getGameDatabase().delete(this);
    }
}
