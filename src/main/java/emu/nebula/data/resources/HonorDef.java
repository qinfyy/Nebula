package emu.nebula.data.resources;

import emu.nebula.Nebula;
import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;
import lombok.Getter;

@Getter
@ResourceType(name = "Honor.json", loadPriority = LoadPriority.LOW)
public class HonorDef extends BaseDef {
    private int Id;
    private int Type;
    private int[] Params;
    
    private transient boolean valid;
    
    @Override
    public int getId() {
        return Id;
    }
    
    @Override
    public void onLoad() {
        // Run AFTER character data has been loaded
        switch (this.Type) {
            case 1 -> {
                // Normal
                this.valid = true;
            }
            case 2 -> {
                // Character
                if (this.Params.length < 1) {
                    break;
                }
                
                int charId = this.Params[0];
                var charData = GameData.getCharacterDataTable().get(charId);
                
                if (charData == null || !charData.isAvailable()) {
                    break;
                }
                
                // Cache honor data for character data and set to valid
                charData.setHonor(this);
                this.valid = true;
            }
            case 3 -> {
                // Group
                this.valid = true;
            }
            default -> {
                // Unknown honor type
                Nebula.getLogger().warn("Honor " + this.getId() + " has an unknown type (" + this.getType() + ")");
            }    
        }
        
    }
}
