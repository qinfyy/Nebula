package emu.nebula.game.character;

import dev.morphia.annotations.Entity;
import emu.nebula.proto.Public.CharGemPreset;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class CharacterGemPreset {
    private String name;
    private int[] gems;
    
    @Deprecated // Morphia only
    public CharacterGemPreset() {
        
    }
    
    public CharacterGemPreset(Character character) {
        this.gems = new int[] {-1, -1, -1};
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getLength() {
        return this.getGems().length;
    }
    
    public int getGemIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.getLength()) {
            return -1;
        }
        
        return this.getGems()[slotIndex];
    }

    public boolean setGemIndex(int slotId, int gemIndex) {
        int slotIndex = slotId - 1;
        
        if (slotIndex < 0 || slotIndex >= this.getLength()) {
            return false;
        }
        
        this.getGems()[slotIndex] = gemIndex;
        
        return true;
    }
    
    // Proto
    
    public CharGemPreset toProto() {
        var proto = CharGemPreset.newInstance()
                .addAllSlotGem(this.getGems());
        
        if (this.getName() != null) {
            proto.setName(this.getName());
        }
        
        return proto;
    }
}
