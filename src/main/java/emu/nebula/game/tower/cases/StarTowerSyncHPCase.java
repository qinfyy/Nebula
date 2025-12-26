package emu.nebula.game.tower.cases;

import emu.nebula.proto.PublicStarTower.StarTowerRoomCase;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractReq;
import emu.nebula.proto.StarTowerInteract.StarTowerInteractResp;
import lombok.Getter;

@Getter
public class StarTowerSyncHPCase extends StarTowerBaseCase {

    @Override
    public CaseType getType() {
        return CaseType.SyncHP;
    }
    
    @Override
    public StarTowerInteractResp interact(StarTowerInteractReq req, StarTowerInteractResp rsp) {
        // Get hp
        int hp = req.getRecoveryHPReq().getHp();
        
        // Sync with game
        this.getGame().setHp(hp);
        
        // Complete
        return rsp;
    }
    
    // Proto
    
    @Override
    public void encodeProto(StarTowerRoomCase proto) {
        // Set field in the proto
        proto.getMutableSyncHPCase();
    }
}
