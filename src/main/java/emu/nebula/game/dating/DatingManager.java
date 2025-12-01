package emu.nebula.game.dating;

import emu.nebula.data.GameData;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.game.quest.QuestCondition;
import lombok.Getter;

@Getter
public class DatingManager extends PlayerManager {
    private DatingGame game;
    
    public DatingManager(Player player) {
        super(player);
    }

    public DatingGame selectLandmark(GameCharacter character, int landmarkId) {
        // Get landmark data
        var data = GameData.getDatingLandmarkDataTable().get(landmarkId);
        
        if (data == null) {
            return null;
        }
        
        // Set landmark + character
        this.game = new DatingGame(character, data);
        
        // Trigger quest/achievement
        this.getPlayer().trigger(QuestCondition.CharactersDatingTotal, 1);
        
        // Success
        return this.game;
    }
    
    public void endDatingGame() {
        this.game = null;
    }
}
