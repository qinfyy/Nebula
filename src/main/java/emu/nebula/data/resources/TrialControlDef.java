package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.Getter;

@Getter
@ResourceType(name = "TrialControl.json")
public class TrialControlDef extends BaseDef {
    private int Id;
    private IntOpenHashSet GroupIds;
    
    @Override
    public int getId() {
        return Id;
    }
}
