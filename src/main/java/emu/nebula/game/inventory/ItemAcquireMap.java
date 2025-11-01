package emu.nebula.game.inventory;

import emu.nebula.data.GameData;
import emu.nebula.game.player.Player;
import emu.nebula.proto.Public.AcqInfo;
import emu.nebula.proto.Public.Acquire;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;

import lombok.Getter;

@Getter
public class ItemAcquireMap {
    private final Int2ObjectMap<ItemAcquireParam> items;
    
    public ItemAcquireMap(Player player, IntList list) {
        this.items = new Int2ObjectOpenHashMap<>();
        
        for (int id : list) {
            // Get item data
            var data = GameData.getItemDataTable().get(id);
            if (data == null) {
                continue;
            }
            
            // Add to acquire map
            if (!this.getItems().containsKey(id)) {
                // Get starting count
                int count = 0;
                
                // Check item type
                if (data.getItemType() == ItemType.Char) {
                    var character = player.getCharacters().getCharacterById(id);
                    
                    if (character != null) {
                        count = 1;
                    }
                } else if (data.getItemType() == ItemType.Disc) {
                    var disc = player.getCharacters().getDiscById(id);
                    
                    if (disc != null) {
                        count = 1;
                        count += disc.getStar();
                        count += player.getInventory().getItemCount(disc.getData().getTransformItemId());
                    }
                }
                
                var acquireInfo = new ItemAcquireParam(data.getItemType(), count);
                acquireInfo.add();
                
                this.getItems().put(id, acquireInfo);
            } else {
                this.getItems().get(id).add();
            }
        }
    }
    
    // Proto
    
    public Acquire toProto() {
        var proto = Acquire.newInstance();
        
        for (var entry : this.items.int2ObjectEntrySet()) {
            var a = AcqInfo.newInstance()
                    .setTid(entry.getIntKey())
                    .setBegin(entry.getValue().getBegin())
                    .setCount(entry.getValue().getCount());
            
            proto.addList(a);
        }
        
        return proto;
    }
    
    @Getter
    public static class ItemAcquireParam {
        private ItemType type;
        private int begin;
        private int count;

        public ItemAcquireParam(ItemType type, int i) {
            this.type = type;
            this.begin = i;
        }
        
        public void add() {
            this.count++;
        }
    }
}
