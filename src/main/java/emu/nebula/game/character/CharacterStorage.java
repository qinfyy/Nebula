package emu.nebula.game.character;

import java.util.ArrayList;
import java.util.Collection;

import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.CharacterDef;
import emu.nebula.data.resources.DiscDef;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.proto.Public.HandbookInfo;
import emu.nebula.util.Bitset;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;

@Getter
public class CharacterStorage extends PlayerManager {
    private final Int2ObjectMap<GameCharacter> characters;
    private final Int2ObjectMap<GameDisc> discs;
    
    @Setter private boolean updateCharHandbook;
    @Setter private boolean updateDiscHandbook;
    
    public CharacterStorage(Player player) {
        super(player);
        
        this.characters = new Int2ObjectOpenHashMap<>();
        this.discs = new Int2ObjectOpenHashMap<>();
    }
    
    // Characters

    public GameCharacter getCharacterById(int id) {
        if (id <= 0) {
            return null;
        }
        
        return this.characters.get(id);
    }
    
    public boolean hasCharacter(int id) {
        return this.characters.containsKey(id);
    }
    
    public GameCharacter addCharacter(int charId) {
        // Sanity check to make sure we dont have this character already
        if (this.hasCharacter(charId)) {
            return null;
        }
        
        return this.addCharacter(GameData.getCharacterDataTable().get(charId));
    }

    private GameCharacter addCharacter(CharacterDef data) {
        // Sanity check to make sure we dont have this character already
        if (this.hasCharacter(data.getId())) {
            return null;
        }
        
        // Create character
        var character = new GameCharacter(this.getPlayer(), data);
        
        // Save to database
        character.save();
        
        // Set flag for player to update character skins in their handbook
        this.setUpdateCharHandbook(true);
        
        // Add to characters
        this.characters.put(character.getCharId(), character);
        return character;
    }
    
    public Collection<GameCharacter> getCharacterCollection() {
        return this.getCharacters().values();
    }
    
    public int getNewPhoneMessageCount() {
        int count = 0;
        
        for (var character : this.getCharacterCollection()) {
            if (character.getContact().hasNew()) {
                count++;
            }
        }
        
        return count;
    }
    
    public HandbookInfo getCharacterHandbook() {
        var bitset = new Bitset();
        
        for (var skinId : getPlayer().getInventory().getAllSkinIds()) {
            // Get handbook data
            var data = GameData.getHandbookDataTable().get(400000 + skinId);
            if (data == null) continue;
            
            // Set flag
            bitset.setBit(data.getIndex());
        }
        
        var handbook = HandbookInfo.newInstance()
                .setType(1)
                .setData(bitset.toByteArray());
        
        return handbook;
    }
    
    // Discs

    public GameDisc getDiscById(int id) {
        if (id <= 0) {
            return null;
        }
        
        return this.discs.get(id);
    }
    
    public boolean hasDisc(int id) {
        return this.discs.containsKey(id);
    }
    
    public GameDisc addDisc(int discId) {
        // Sanity check to make sure we dont have this character already
        if (this.hasDisc(discId)) {
            return null;
        }
        
        return this.addDisc(GameData.getDiscDataTable().get(discId));
    }

    private GameDisc addDisc(DiscDef data) {
        // Sanity check to make sure we dont have this character already
        if (this.hasDisc(data.getId())) {
            return null;
        }
        
        // Create disc
        var disc = new GameDisc(this.getPlayer(), data);
        
        // Save to database
        disc.save();
        
        // Set flag for player to update discs in their handbook
        this.setUpdateDiscHandbook(true);
        
        // Add to discs
        this.discs.put(disc.getDiscId(), disc);
        return disc;
    }
    
    public Collection<GameDisc> getDiscCollection() {
        return this.getDiscs().values();
    }
    
    public HandbookInfo getDiscHandbook() {
        var bitset = new Bitset();
        
        for (var disc : this.getDiscCollection()) {
            // Get handbook
            var data = GameData.getHandbookDataTable().get(disc.getDiscId());
            if (data == null) continue;
            
            // Set flag
            bitset.setBit(data.getIndex());
        }
        
        var handbook = HandbookInfo.newInstance()
                .setType(2)
                .setData(bitset.toByteArray());
        
        return handbook;
    }
    
    public PlayerChangeInfo limitBreakAllDiscs() {
        // Create variables
        var change = new PlayerChangeInfo();
        var modifiedDiscs = new ArrayList<GameDisc>();
        
        // Try to limit all discs
        for (var disc : this.getDiscCollection()) {
            // Skip if at max stars
            if (disc.getStar() >= 5) {
                continue;
            }
            
            // Get transform id
            var transformId = disc.getData().getTransformItemId();
            var transformCount = this.getPlayer().getInventory().getItemCount(transformId);
            
            if (transformCount <= 0) {
                continue;
            }
            
            // Try to limit break
            var discChange = disc.limitBreak(transformCount);
            
            // Check if limit break was successful
            if (discChange == null) {
                continue;
            }
            
            // Merge any changes from the disc limit break to this one
            change.add(discChange);
            
            // Add to changed discs
            modifiedDiscs.add(disc);
        }
        
        // Success
        return change.setExtraData(modifiedDiscs);
    }
    
    // Database
    
    public void loadFromDatabase() {
        var db = Nebula.getGameDatabase();
        
        db.getObjects(GameCharacter.class, "playerUid", getPlayerUid()).forEach(character -> {
            // Get data
            var data = GameData.getCharacterDataTable().get(character.getCharId());
            
            // Validate
            if (data == null) {
                return;
            }
            
            character.setPlayer(this.getPlayer());
            character.setData(data);
            
            // Add to characters
            this.characters.put(character.getCharId(), character);
        });
        
        db.getObjects(GameDisc.class, "playerUid", getPlayerUid()).forEach(disc -> {
            // Get data
            var data = GameData.getDiscDataTable().get(disc.getDiscId());
            if (data == null) return;
            
            disc.setPlayer(this.getPlayer());
            disc.setData(data);
            
            // Add
            this.discs.put(disc.getDiscId(), disc);
        });
    }
}
