package emu.nebula.game.tower;

import java.util.HashMap;
import java.util.LinkedHashMap;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.Player;
import emu.nebula.proto.PublicStarTower.PotentialPreselection;
import emu.nebula.proto.PublicStarTower.StarTowerBookCharPotential;
import emu.nebula.proto.PublicStarTower.StarTowerBookPotential;
import emu.nebula.util.Snowflake;

import lombok.Getter;

@Getter
@Entity(value = "presets", useDiscriminator = false)
public class StarTowerPotentialPreset implements GameDatabaseObject {
    @Id
    private int uid;
    @Indexed
    private int playerUid;
    
    private String name;
    private boolean preference;
    private long timestamp;
    private LinkedHashMap<Integer, HashMap<Integer, Integer>> charPotentials;
    
    @Deprecated
    public StarTowerPotentialPreset() {
        // Morphia only
    }
    
    public StarTowerPotentialPreset(Player player, String name, boolean preference, Iterable<StarTowerBookCharPotential> potentials) {
        this.uid = Snowflake.newUid();
        this.playerUid = player.getUid();
        this.name = name;
        this.preference = preference;
        this.charPotentials = new LinkedHashMap<>(); // Order is important
        this.timestamp = Nebula.getCurrentServerTime();
        
        this.updatePotentials(potentials);
    }

    public void setName(String newName) {
        // Clamp name length to prevent long names
        if (newName.length() > 32) {
            newName = newName.substring(0, 31);
        }
        
        this.name = newName;
        Nebula.getGameDatabase().update(this, this.getUid(), "name", this.getName());
    }
    
    public void setPreference(boolean state) {
        // Skip if no change detected
        if (this.preference == state) {
            return;
        }
        
        // Set preference (favorite toggle)
        this.preference = state;
        Nebula.getGameDatabase().update(this, this.getUid(), "preference", this.isPreference());
    }
    
    public void updatePotentials(Iterable<StarTowerBookCharPotential> charPotentials) {
        // Clear character potentials first
        this.getCharPotentials().clear();
        
        // Parse character potentials
        for (var charPotential : charPotentials) {
            int charId = charPotential.getCharId();
            var potentials = new HashMap<Integer, Integer>();
            
            for (var potential : charPotential.getPotentials()) {
                // Validate
                var data = GameData.getPotentialDataTable().get(potential.getId());
                if (data == null || data.getCharId() != data.getCharId()) {
                    continue;
                }
                
                // Check level
                int level = potential.getLevel();
                level = Math.max(level, 0);
                level = Math.min(level, data.getMaxLevel());
                
                // Set potential
                potentials.put(potential.getId(), potential.getLevel());
            }
            
            // Add character potential
            this.getCharPotentials().put(charId, potentials);
        }
    }
    
    // Proto
    
    public PotentialPreselection toProto() {
        var proto = PotentialPreselection.newInstance()
                .setId(this.getUid())
                .setName(this.getName())
                .setPreference(this.isPreference());
        
        for (var entry : this.getCharPotentials().entrySet()) {
            int charId = entry.getKey();
            var potentials = entry.getValue();
            
            var info = StarTowerBookCharPotential.newInstance()
                    .setCharId(charId);
            
            for (var potential : potentials.entrySet()) {
                int potentialId = potential.getKey();
                int level = potential.getValue();
                
                var book = StarTowerBookPotential.newInstance()
                        .setId(potentialId)
                        .setLevel(level);
                
                info.addPotentials(book);
            }
            
            proto.addCharPotentials(info);
        }

        return proto;
    }
    
    // Database

    public void delete() {
        Nebula.getGameDatabase().delete(this);
    }
}
