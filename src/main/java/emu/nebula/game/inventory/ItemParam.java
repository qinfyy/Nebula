package emu.nebula.game.inventory;

import com.google.gson.annotations.SerializedName;

import dev.morphia.annotations.Entity;
import emu.nebula.proto.Public.ItemTpl;
import lombok.Getter;

@Getter
@Entity(useDiscriminator = false)
public class ItemParam {
    @SerializedName("Tid")
    public int id;
    
    @SerializedName("Qty")
    public int count;
    
    @Deprecated // Morphia only
    public ItemParam() {
        
    }
    
    public ItemParam(int id, int count) {
        this.id = id;
        this.count = count;
    }

    public ItemTpl toProto() {
        var proto = ItemTpl.newInstance()
                .setTid(this.getId())
                .setQty(this.getCount());
        
        return proto;
    }
}
