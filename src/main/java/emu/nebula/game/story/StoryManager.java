package emu.nebula.game.story;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.game.player.PlayerManager;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import lombok.Getter;

@Getter
@Entity(value = "story", useDiscriminator = false)
public class StoryManager extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    private IntSet completedStories;
    
    @Deprecated // Morphia only
    public StoryManager() {
        
    }
    
    public StoryManager(Player player) {
        super(player);
        this.uid = player.getUid();
        this.completedStories = new IntOpenHashSet();
    }

    public PlayerChangeInfo settle(IntList list) {
        var changes = new PlayerChangeInfo();
        
        for (int id : list) {
            // Get story data
            var data = GameData.getStoryDataTable().get(id);
            if (data == null) continue;
            
            // Check if we already completed the story
            if (this.getCompletedStories().contains(id)) {
                continue;
            }
            
            // Complete story and get rewards
            this.getCompletedStories().add(id);
            
            // Add rewards
            this.getPlayer().getInventory().addItems(data.getRewards(), changes);
            
            // Save to db
            Nebula.getGameDatabase().addToList(this, this.getPlayerUid(), "completedStories", id);
        }
        
        return changes;
    }
}
