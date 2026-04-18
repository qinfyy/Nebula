package emu.nebula.game.formation;

import emu.nebula.game.player.PlayerManager;

import java.util.HashMap;
import java.util.Map;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.Player;
import emu.nebula.proto.Public.FormationInfo;
import lombok.Getter;

@Getter
@Entity(value = "formations", useDiscriminator = false)
public class FormationManager extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    private Map<Integer, Formation> formations;

    @Deprecated // Morphia only
    public FormationManager() {
        
    }
    
    public FormationManager(Player player) {
        super(player);
        this.uid = player.getUid();
        this.formations = new HashMap<>();
        
        this.save();
    }
    
    public Formation getFormationById(int num) {
        return this.formations.get(num);
    }
    
    public boolean updateFormation(FormationInfo info) {
        // Sanity check
        if (info.getNumber() < 1 || info.getNumber() > GameConstants.MAX_FORMATIONS) {
            return false;
        }
        
        // More sanity
        if (info.getCharIds().length() < 1 || info.getCharIds().length() > 3) {
            return false;
        }
        
        if (info.getDiscIds().length() < 3 || info.getDiscIds().length() > 6) {
            return false;
        }
        
        // Validate formation to make sure we have all the chars and discs
        // TODO
        
        // Create formation
        var formation = new Formation(this.getPlayer(), info);
        
        // Add to formations map
        this.formations.put(formation.getNum(), formation);
        
        // Save to db
        Nebula.getGameDatabase().update(this, this.getPlayerUid(), "formations." + formation.getNum(), formation, true);
        
        // Success
        return true;
    }

}
