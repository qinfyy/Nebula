package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParam;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.util.JsonUtils;
import lombok.Getter;

@Getter
@ResourceType(name = "Story.json")
public class StoryDef extends BaseDef {
    private int Id;
    private int Chapter;
    private String RewardDisplay;
    
    private transient ItemParamMap rewards;
    
    @Override
    public int getId() {
        return Id;
    }
    
    @Override
    public void onLoad() {
        var list = JsonUtils.decodeList(this.getRewardDisplay(), ItemParam.class);
        
        if (list != null) {
            rewards = ItemParamMap.fromItemParams(list);
        } else {
            rewards = new ItemParamMap();
        }
    }
}
