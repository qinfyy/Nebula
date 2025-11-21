package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.util.CustomIntArray;
import emu.nebula.util.WeightedList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
@ResourceType(name = "CharGemSlotControl.json")
public class CharGemSlotControlDef extends BaseDef {
    private int Id;
    private int Position;
    private int MaxAlterNum;
    private int UnlockLevel;
    
    private int GeneratenCostQty;
    private int RefreshCostQty;
    
    private int UniqueAttrGroupProb;
    private int UniqueAttrGroupId;
    private int GuaranteeCount;
    
    private int[] AttrGroupId;
    
    private int LockableNum;
    private int LockItemTid;
    private int LockItemQty;
    
    @Override
    public int getId() {
        return Id;
    }
    
    public IntList generateAttributes() {
        return this.generateAttributes(new CustomIntArray());
    }
    
    public IntList generateAttributes(CustomIntArray list) {
        // Add unique attributes
        if (this.UniqueAttrGroupId > 0) {
            var group = GameData.getCharGemAttrGroupDataTable().get(this.UniqueAttrGroupId);
            int num = group.getRandomUniqueAttrNum();
            
            for (int i = 0; i < num; i++) {
                var attributeType = group.getRandomAttributeType(list);
                list.add(attributeType.getRandomValue());
            }
            
            if (list.getValueCount() >= this.MaxAlterNum) {
                return list;
            }
        }
        
        // Get random attributes
        var random = new WeightedList<CharGemAttrGroupDef>();
        
        for (var groupId : this.AttrGroupId) {
            var group = GameData.getCharGemAttrGroupDataTable().get(groupId);
            if (group == null || group.getWeight() == 0) {
                continue;
            }
            
            random.add(group.getWeight(), group);
        }
        
        // Add up to 4 attributes
        while (list.getValueCount() < this.MaxAlterNum) {
            var group = random.next();
            var attributeType = group.getRandomAttributeType(list);
            list.add(attributeType.getRandomValue());
        }
        
        // Complete
        return list;
    }
}
