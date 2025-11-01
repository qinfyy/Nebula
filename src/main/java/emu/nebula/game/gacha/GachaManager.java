package emu.nebula.game.gacha;

import java.util.Collection;

import emu.nebula.Nebula;
import emu.nebula.data.resources.GachaDef;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerManager;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class GachaManager extends PlayerManager {
    private final Int2ObjectMap<GachaBannerInfo> bannerInfos;
    private boolean loaded;
    
    public GachaManager(Player player) {
        super(player);
        this.bannerInfos = new Int2ObjectOpenHashMap<>();
    }
    
    public synchronized Collection<GachaBannerInfo> getBannerInfos() {
        return this.bannerInfos.values();
    }
    
    public synchronized GachaBannerInfo getBannerInfo(GachaDef gachaData) {
        if (!this.loaded) {
            this.loadFromDatabase();
        }
        
        return this.bannerInfos.computeIfAbsent(
                gachaData.getId(), 
                i -> new GachaBannerInfo(this.getPlayer(), gachaData)
        );
    }
    
    private void loadFromDatabase() {
        var db = Nebula.getGameDatabase();
        
        db.getObjects(GachaBannerInfo.class, "playerUid", getPlayerUid()).forEach(bannerInfo -> {
            this.bannerInfos.put(bannerInfo.getBannerId(), bannerInfo);
        });
        
        this.loaded = true;
    }
}
