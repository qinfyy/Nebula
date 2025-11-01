package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;
import lombok.Getter;

@Getter
@ResourceType(name = "GachaStorage.json", loadPriority = LoadPriority.HIGH)
public class GachaStorageDef extends BaseDef {
    private int Id;
    
    private int DefaultId;
    private int DefaultQty;
    private int CostId;
    private int CostQty;
    
    private int ATypeUpProb;
    private int BTypeUpProb;
    private int BTypeGuaranteeProb;
    
    @Override
    public int getId() {
        return Id;
    }
}
