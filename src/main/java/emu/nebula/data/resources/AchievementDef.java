package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.achievement.AchievementHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;

@Getter
@ResourceType(name = "Achievement.json")
public class AchievementDef extends BaseDef {
    private int Id;
    private int Type;
    private int CompleteCond;
    private int AimNumShow;
    private int[] Prerequisites;
    
    // Reward
    private int Tid1;
    private int Qty1;
    
    // Custom params
    private transient int param1;
    private transient int param2;
    
    @Override
    public int getId() {
        return Id;
    }

    public void setParams(int param1, int param2) {
        this.param1 = param1;
        this.param2 = param2;
    }

    public boolean hasParam1() {
        return this.param1 > 0;
    }
    
    public boolean hasParam2() {
        return this.param2 > 0;
    }

    @Override
    public void onLoad() {
        // Add to cached achievement list
        var list = AchievementHelper.getCache().computeIfAbsent(this.CompleteCond, i -> new ObjectArrayList<>());
        list.add(this);
    }
}
