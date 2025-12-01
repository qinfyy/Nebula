package emu.nebula.game.achievement;

import java.util.List;

import emu.nebula.GameConstants;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.AchievementDef;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;

// Because achievements in the data files do not have params, we will hardcode them here
public class AchievementHelper {
    // Cache
    private static IntSet isTotalAchievementSet = new IntOpenHashSet();
    
    @Getter
    private static Int2ObjectMap<List<AchievementDef>> cache = new Int2ObjectOpenHashMap<>();
    
    public static List<AchievementDef> getAchievementsByCondition(int condition) {
        return cache.get(condition);
    }
    
    //
    
    public static boolean isTotalAchievement(int condition) {
        return isTotalAchievementSet.contains(condition);
    }
    
    // Fix params
    
    public static void init() {
        // Cache total achievements
        for (var condition : AchievementCondition.values()) {
            if (condition.name().endsWith("Total")) {
                isTotalAchievementSet.add(condition.getValue());
            }
        }
        
        isTotalAchievementSet.add(AchievementCondition.ItemsAdd.getValue());
        isTotalAchievementSet.add(AchievementCondition.ItemsDeplete.getValue());
        
        // Fix params
        fixParams();
    }
    
    private static void fixParams() {
        // Monolith
        addParam(78, 0, 2);
        addParam(79, 0, 4);
        addParam(498, 0, 1);
        
        // Money
        addParam(25, GameConstants.GOLD_ITEM_ID, 0);
        addParam(26, GameConstants.GOLD_ITEM_ID, 0);
        addParam(27, GameConstants.GOLD_ITEM_ID, 0);
        addParam(28, GameConstants.GOLD_ITEM_ID, 0);
        addParam(29, GameConstants.GOLD_ITEM_ID, 0);
    }
    
    private static void addParam(int achievementId, int param1, int param2) {
        var data = GameData.getAchievementDataTable().get(achievementId);
        if (data == null) return;
        
        data.setParams(param1, param2);
    }
}
