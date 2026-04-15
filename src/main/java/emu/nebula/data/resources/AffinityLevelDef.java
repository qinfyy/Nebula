package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;

import lombok.Getter;

@Getter
@ResourceType(name = "AffinityLevel.json")
public class AffinityLevelDef extends BaseDef {
    private int AffinityLevel;
    private int NeedExp;
    
    @Getter
    private static transient int maxLevel;
    
    @Override
    public int getId() {
        return AffinityLevel;
    }
    
    @Override
    public void onLoad() {
        // Calculate max affinity level
        if (this.AffinityLevel > maxLevel) {
            maxLevel = this.AffinityLevel;
        }
    }
}
