package emu.nebula.game.formation;

import dev.morphia.annotations.Entity;
import emu.nebula.game.player.Player;
import emu.nebula.game.tower.StarTowerPotentialPreset;
import emu.nebula.proto.Public.FormationInfo;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class Formation {
    private int num;
    private int[] charIds;
    private int[] discIds;
    private long presetId;
    
    @Deprecated
    public Formation() {
        
    }
    
    public Formation(int num) {
        this.num = num;
        this.charIds = new int[3];
        this.discIds = new int[6];
    }
    
    public Formation(Player player, FormationInfo formation) {
        this.num = formation.getNumber();
        this.charIds = formation.getCharIds().toArray();
        this.discIds = formation.getDiscIds().toArray();
        
        // Validate presetId
        var preset = player.getStarTowerManager().getPresetById(formation.getPreselectionId());
        if (preset != null && isValidPreset(preset)) {
            this.presetId = preset.getUid();
        }
    }

    public int getCharIdAt(int i) {
        if (i < 0 || i >= this.charIds.length) {
            return -1;
        }
        
        return this.charIds[i];
    }

    public int getDiscIdAt(int i) {
        if (i < 0 || i >= this.discIds.length) {
            return -1;
        }
        
        return this.discIds[i];
    }
    
    public int getCharCount() {
        int count = 0;
        
        for (int id : this.getCharIds()) {
            if (id > 0) {
                count++;
            }
        }
        
        return count;
    }

    public int getDiscCount() {
        int count = 0;
        
        for (int id : this.getDiscIds()) {
            if (id > 0) {
                count++;
            }
        }
        
        return count;
    }

    private boolean isValidPreset(StarTowerPotentialPreset preset) {
        // Returns true if all the character ids in this preset match our charIds
        for (int charId : this.getCharIds()) {
            if (!preset.getCharPotentials().containsKey(charId)) {
                // Character id not found
                return false;
            }
        }
        
        return true;
    }
    
    // Proto
    
    public FormationInfo toProto() {
        var proto = FormationInfo.newInstance()
                .setNumber(this.getNum())
                .addAllCharIds(this.getCharIds())
                .addAllDiscIds(this.getDiscIds())
                .setPreselectionId(this.getPresetId());
        
        return proto;
    }
}
