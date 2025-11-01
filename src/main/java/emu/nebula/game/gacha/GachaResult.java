package emu.nebula.game.gacha;

import emu.nebula.game.player.PlayerChangeInfo;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
public class GachaResult {
    private GachaBannerInfo info;
    private PlayerChangeInfo change;
    private IntList cards;
    
    public GachaResult(GachaBannerInfo info, PlayerChangeInfo change, IntList cards) {
        this.info = info;
        this.change = change;
        this.cards = cards;
    }
    
}
