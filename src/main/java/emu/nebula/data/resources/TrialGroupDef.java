package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.game.inventory.ItemParamMap;
import lombok.Getter;

@Getter
@ResourceType(name = "TrialGroup.json")
public class TrialGroupDef extends BaseDef {
    private int Id;
    
    private int RewardId1;
    private int Qty1;
    private int RewardId2;
    private int Qty2;
    private int RewardId3;
    private int Qty3;
    
    private transient ItemParamMap rewards;
    
    @Override
    public int getId() {
        return Id;
    }

    @Override
    public void onLoad() {
        this.rewards = new ItemParamMap();
        
        if (this.RewardId1 > 0) {
            this.rewards.add(this.RewardId1, this.Qty1);
        }
        if (this.RewardId2 > 0) {
            this.rewards.add(this.RewardId2, this.Qty2);
        }
        if (this.RewardId3 > 0) {
            this.rewards.add(this.RewardId3, this.Qty3);
        }
    }
}
