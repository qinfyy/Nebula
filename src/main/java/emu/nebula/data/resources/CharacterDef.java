package emu.nebula.data.resources;

import java.util.Arrays;
import java.util.List;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.game.character.ElementType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;

@Getter
@ResourceType(name = "Character.json")
public class CharacterDef extends BaseDef {
    private int Id;
    private boolean Visible;
    private boolean Available;
    private String Name;
    private int Grade;
    private int EET;
    
    private int DefaultSkinId;
    private int AdvanceSkinId;
    private int AdvanceSkinUnlockLevel;
    
    private int AdvanceGroup;
    private int[] SkillsUpgradeGroup;

    private int FragmentsId;
    private int TransformQty;
    
    private int[] GemSlots;
    
    private transient CharacterDesDef des;
    private transient ElementType elementType;
    private transient List<ChatDef> chats;
    
    @Override
    public int getId() {
        return Id;
    }
    
    protected void setDes(CharacterDesDef des) {
        this.des = des;
    }

    public int getSkillsUpgradeGroup(int index) {
        if (index < 0 || index >= this.SkillsUpgradeGroup.length) {
            return -1;
        }
        
        return this.SkillsUpgradeGroup[index];
    }
    
    public CharGemDef getCharGemData(int slotId) {
        int id = this.GemSlots[slotId - 1];
        return GameData.getCharGemDataTable().get(id);
    }
    
    @Override
    public void onLoad() {
        this.elementType = ElementType.getByValue(this.EET);
        this.chats = new ObjectArrayList<>();
        
        // Sort gem slots
        this.GemSlots = Arrays.stream(this.GemSlots).sorted().toArray();
    }
}
