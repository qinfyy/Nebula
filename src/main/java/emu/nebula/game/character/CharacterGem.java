package emu.nebula.game.character;

import dev.morphia.annotations.Entity;
import emu.nebula.proto.Public.CharGem;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class CharacterGem {
    private boolean locked;
    private int[] attributes;
    private int[] alterAttributes;
    
    @Deprecated // Morphia only
    public CharacterGem() {
        
    }
    
    public CharacterGem(IntList attributes) {
        this.attributes = attributes.toIntArray();
        this.alterAttributes = new int[4];
    }
    
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setNewAttributes(IntList attributes) {
        this.alterAttributes = attributes.toIntArray();
    }
    
    public boolean hasAlterAttributes() {
        for (int i = 0; i < this.alterAttributes.length; i++) {
            if (this.alterAttributes[i] <= 0) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean replaceAttributes() {
        // Make sure gem has alter attributes
        if (!this.hasAlterAttributes()) {
            return false;
        }
        
        // Replace attributes
        this.attributes = this.alterAttributes;
        this.alterAttributes = new int[4];
        
        // Success
        return true;
    }
    
    // Proto
    
    public CharGem toProto() {
        var proto = CharGem.newInstance()
            .setLock(this.isLocked())
            .addAllAttributes(this.getAttributes())
            .addAllAlterAttributes(this.getAlterAttributes());
        
        return proto;
    }
}
