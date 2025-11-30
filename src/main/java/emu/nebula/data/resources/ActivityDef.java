package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;

import lombok.Getter;

@Getter
@ResourceType(name = "Activity.json")
public class ActivityDef extends BaseDef {
    private int Id;
    private int ActivityType;
    
    private transient emu.nebula.game.activity.ActivityType type;
    
    @Override
    public int getId() {
        return Id;
    }

    @Override
    public void onLoad() {
        this.type = emu.nebula.game.activity.ActivityType.getByValue(this.ActivityType);
    }
}
