package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.Getter;

@Getter
@ResourceType(name = "CharacterDes.json", loadPriority = LoadPriority.LOW)
public class CharacterDesDef extends BaseDef {
    private int Id;
    private IntOpenHashSet Tag;
    private IntOpenHashSet PreferTags;
    private IntOpenHashSet HateTags;
    
    @Override
    public int getId() {
        return Id;
    }
    
    public boolean isPreferGift(AffinityGiftDef gift) {
        if (this.getPreferTags() == null) {
            return false;
        }
        
        for (int i = 0; i < gift.getTags().length; i++) {
            int tag = gift.getTags()[i];
            if (this.getPreferTags().contains(tag)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isHateGift(AffinityGiftDef gift) {
        if (this.getHateTags() == null) {
            return false;
        }
        
        for (int i = 0; i < gift.getTags().length; i++) {
            int tag = gift.getTags()[i];
            if (this.getHateTags().contains(tag)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void onLoad() {
        var character = GameData.getCharacterDataTable().get(this.getId());
        if (character != null) {
            character.setDes(this);
        }
    }
}
