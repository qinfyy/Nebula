package emu.nebula.game.gacha;

import org.bson.types.ObjectId;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.data.resources.GachaDef;
import emu.nebula.data.resources.GachaDef.GachaPackage;
import emu.nebula.data.resources.GachaPkgDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.Player;
import emu.nebula.util.Utils;
import lombok.Getter;

@Getter
@Entity(value = "banner_info", useDiscriminator = false)
public class GachaBannerInfo implements GameDatabaseObject {
    @Id
    private ObjectId id;
    
    private int bannerId;
    private int playerUid;
    
    private int total;
    private int missTimesA;
    private int missTimesUpA;
    private int missTimesB;
    private boolean usedGuarantee;
    
    @Deprecated //Morphia only
    public GachaBannerInfo() {
        
    }
    
    public GachaBannerInfo(Player player, GachaDef data) {
        this.playerUid = player.getUid();
        this.bannerId = data.getId();
    }
    
    public int doPull(GachaDef data) {
        // Pull chances
        int chanceA = 20;   // 2%
        int chanceB = 100;  // 8%
        
        // 4 star pity
        if (this.missTimesB >= 9) {
            chanceB = 1000;
        }
        
        // 5 star pity
        if (this.missTimesA >= 159) {
            chanceA = 1000;
            chanceB = 0;
        }
        
        // Add miss times
        this.missTimesB++;
        this.missTimesA++;
        //this.missTimesUpA++;
        
        // Get random
        int random = Utils.randomRange(1, 1000);
        GachaPackage gp = null;
        
        if (random <= chanceA) {
            // Reset pity
            this.missTimesA = 0;
            
            // Get A package
            gp = data.getPackageA().next();
        } else if (random <= chanceB) {
            // Add miss times
            this.missTimesB = 0;
            
            // Get B package
            gp = data.getPackageB().next();
        } else {
            // Get C package
            gp = data.getPackageC().next();
        }
        
        // Sanity check
        if (gp == null) {
            return 0;
        }
        
        // Get package
        var pkg = GachaPkgDef.getPackageById(gp.getId());
        if (pkg == null) {
            return 0;
        }
        
        // Add total pulls
        this.total++;
        
        // Get random id
        return pkg.next();
    }
}
