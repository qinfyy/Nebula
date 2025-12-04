package emu.nebula.game.tower;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
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
    
    @Deprecated
    public StarTowerBuild() {
        
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
        
        // Characters
        this.charIds = game.getChars().stream()
                .filter(c -> c.getId() > 0)
                .mapToInt(c -> c.getId())
                .toArray();
        
        // Discs
        this.discIds = game.getDiscs().stream()
                .filter(d -> d.getId() > 0)
                .mapToInt(d -> d.getId())
                .toArray();
        
        // Add potentials
        for (var entry : game.getPotentials()) {
            //
            int id = entry.getIntKey();
            int level = entry.getIntValue();
            
            // Add to potential map
            this.getPotentials().put(id, level);
            
            // Add to character
            var potentialData = GameData.getPotentialDataTable().get(id);
            if (potentialData != null) {
                int charId = potentialData.getCharId();
                this.getCharPots().put(charId, this.getCharPots().get(charId) + 1);
            }
        }
        
        // Add sub note skills
        for (var entry : game.getItems()) {
            this.getSubNoteSkills().put(entry.getIntKey(), entry.getIntValue());
        }
        
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
        if (newName.length() > 32) {
            newName = newName.substring(0, 31);
        }
        
        this.name = newName;
        Nebula.getGameDatabase().update(this, this.getUid(), "name", this.getName());
    }
    
    public void setLock(boolean state) {
        this.lock = state;
        Nebula.getGameDatabase().update(this, this.getUid(), "lock", this.isLock());
    }
    
    public void setPreference(boolean state) {
        this.preference = state;
        Nebula.getGameDatabase().update(this, this.getUid(), "preference", this.isPreference());
    }
    
    // Score
    
    public int calculateScore() {
        // Clear score
        this.score = 0;
        
        // Potentials
        for (var potential : this.getPotentials().int2IntEntrySet()) {
            var data = GameData.getPotentialDataTable().get(potential.getIntKey());
            if (data == null) continue;
            
            this.score += data.getBuildScore(potential.getIntValue());
        }
        
        // Sub note skills
        for (var item : this.getSubNoteSkills()) {
            this.score += item.getIntValue() * 15;
        }
        
        // Complete
        return this.score;
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
        
        return proto;
    }
    
    // Database

    public void delete() {
        Nebula.getGameDatabase().delete(this);
    }
}
