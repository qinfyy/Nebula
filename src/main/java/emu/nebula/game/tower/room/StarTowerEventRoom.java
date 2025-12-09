package emu.nebula.game.tower.room;

import emu.nebula.data.resources.StarTowerStageDef;
import emu.nebula.game.tower.StarTowerGame;
import emu.nebula.game.tower.cases.StarTowerSyncHPCase;

import lombok.Getter;

@Getter
public class StarTowerEventRoom extends StarTowerBaseRoom {
    
    public StarTowerEventRoom(StarTowerGame game, StarTowerStageDef stage) {
        super(game, stage);
    }

    @Override
    public void onEnter() {
        // Create npc
        this.addCase(this.createNpcEvent());
        
        // Create sync hp case
        this.addCase(new StarTowerSyncHPCase());
        
        // Create door case
        this.createExit();
    }
}
