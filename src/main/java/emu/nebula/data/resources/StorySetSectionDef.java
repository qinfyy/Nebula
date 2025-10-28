package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;

@Getter
@ResourceType(name = "StorySetSection.json")
public class StorySetSectionDef extends BaseDef {
    private int Id;
    private int ChapterId;
    
    @Getter
    private static IntSet chapterIds = new IntOpenHashSet();
    
    @Override
    public int getId() {
        return Id;
    }
    
    @Override
    public void onLoad() {
        chapterIds.add(this.getChapterId());
    }
}
