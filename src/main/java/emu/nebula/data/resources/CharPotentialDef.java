package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
@ResourceType(name = "CharPotential.json")
public class CharPotentialDef extends BaseDef {
    private int Id;
    
    private int[] MasterSpecificPotentialIds;
    private int[] AssistSpecificPotentialIds;
    private int[] CommonPotentialIds;
    private int[] MasterNormalPotentialIds;
    private int[] AssistNormalPotentialIds;
    
    @Override
    public int getId() {
        return Id;
    }
    
    public IntList getPotentialList(boolean main, boolean special) {
        // Create list
        var list = new IntArrayList();
        
        //
        if (main) {
            if (special) {
                list.addElements(0, this.getMasterSpecificPotentialIds());
            } else {
                list.addElements(0, this.getMasterNormalPotentialIds());
            }
        } else {
            if (special) {
                list.addElements(0, this.getAssistSpecificPotentialIds());
            } else {
                list.addElements(0, this.getAssistNormalPotentialIds());
            }
        }
        
        if (!special) {
            list.addElements(0, this.getCommonPotentialIds()); 
        }
        
        // Complete
        return list;
    }
}
