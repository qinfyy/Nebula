package emu.nebula.game.inventory;

import java.util.List;

import com.mongodb.client.model.Filters;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.DropPkgDef;
import emu.nebula.data.resources.MallShopDef;
import emu.nebula.data.resources.ResidentGoodsDef;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.PlayerManager;
import emu.nebula.game.quest.QuestCondition;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Notify.Skin;
import emu.nebula.proto.Public.Honor;
import emu.nebula.proto.Public.Item;
import emu.nebula.proto.Public.Res;
import emu.nebula.proto.Public.Title;
import emu.nebula.proto.Public.UI32;
import emu.nebula.util.Utils;
import emu.nebula.util.ints.String2IntMap;
import emu.nebula.game.achievement.AchievementCondition;
import emu.nebula.game.player.Player;
import emu.nebula.game.player.PlayerChangeInfo;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;

@Getter
@Entity(value = "inventory", useDiscriminator = false)
public class Inventory extends PlayerManager implements GameDatabaseObject {
    @Id
    private int uid;
    
    // Items/resources
    private ItemParamMap items;
    private ItemParamMap resources;
    
    // Database persistent data
    private IntSet extraSkins;
    private IntSet headIcons;
    private IntSet titles;
    private IntSet honorList;
    
    // Buy limit
    private ItemParamMap shopBuyCount;
    private String2IntMap mallBuyCount;
    
    @Deprecated
    public Inventory() {
        // Morphia only
    }
    
    public Inventory(Player player) {
        this();
        this.setPlayer(player);
        this.uid = player.getUid();
        
        // Setup
        this.resources = new ItemParamMap();
        this.items = new ItemParamMap();
        
        this.extraSkins = new IntOpenHashSet();
        this.headIcons = new IntOpenHashSet();
        this.titles = new IntOpenHashSet();
        this.honorList = new IntOpenHashSet();
        
        this.shopBuyCount = new ItemParamMap();
        this.mallBuyCount = new String2IntMap();
        
        // Add player heads
        this.getHeadIcons().add(101);
        this.getHeadIcons().add(102);
        
        // Add titles directly
        this.getTitles().add(player.getTitlePrefix());
        this.getTitles().add(player.getTitleSuffix());
        
        // Save to database
        this.save();
    }
    
    //
    
    public IntCollection getAllSkinIds() {
        // Setup int collection
        var skins = new IntOpenHashSet();
        
        // Add character skins
        for (var character : getPlayer().getCharacters().getCharacterCollection()) {
            // Add default skin id
            skins.add(character.getData().getDefaultSkinId());
            
            // Add advance skin
            if (character.getAdvance() >= character.getData().getAdvanceSkinUnlockLevel()) {
                skins.add(character.getData().getAdvanceSkinId());
            }
        }
        
        // Finally, add extra skins
        skins.addAll(this.getExtraSkins());
        
        // Complete and return
        return skins;
    }
    
    public boolean hasSkin(int id) {
        // Get skin data
        var skinData = GameData.getCharacterSkinDataTable().get(id);
        if (skinData == null) {
            return false;
        }
        
        // Get character
        var character = getPlayer().getCharacters().getCharacterById(skinData.getCharId());
        if (character == null) {
            return false;
        }
        
        // Check
        switch (skinData.getType()) {
            case 1:
                // Default skin, always allow
                break;
            case 2:
                // Ascension skin, only allow if the character has the right ascension level
                if (character.getAdvance() < character.getData().getAdvanceSkinUnlockLevel()) {
                    return false;
                }
                break;
            default:
                // Extra skin, only allow if we have the skin unlocked
                if (!getPlayer().getInventory().getExtraSkins().contains(id)) {
                    return false;
                }
                break;
        }
        
        // Unknown
        return true;
    }
    
    public boolean addSkin(int id) {
        // Make sure we are not adding duplicates
        if (this.getExtraSkins().contains(id)) {
            return false;
        }
        
        // Add
        this.getExtraSkins().add(id);
        
        // Save to database
        Nebula.getGameDatabase().addToSet(this, this.getUid(), "extraSkins", id);
        
        // Send packets
        this.getPlayer().addNextPackage(
            NetMsgId.character_skin_gain_notify, 
            Skin.newInstance().setNew(UI32.newInstance().setValue(id))
        );
        
        // Set flag for player to update character skins in their handbook
        this.getPlayer().getCharacters().setUpdateCharHandbook(true);
        
        // Success
        return true;
    }
    
    public IntCollection getAllHeadIcons() {
        // Setup int collection
        var icons = new IntOpenHashSet();
        
        // Add character skins
        for (var character : getPlayer().getCharacters().getCharacterCollection()) {
            // Add default head icon id
            icons.add(character.getData().getDefaultSkinId());
            
            // Add advance head icon
            if (character.getAdvance() >= character.getData().getAdvanceSkinUnlockLevel()) {
                icons.add(character.getData().getAdvanceSkinId());
            }
        }
        
        // Finally, add extra head icons
        icons.addAll(this.getHeadIcons());
        
        // Complete and return
        return icons;
    }
    
    public boolean hasHeadIcon(int id) {
        // Get head icon data
        var data = GameData.getPlayerHeadDataTable().get(id);
        if (data == null) {
            return false;
        }
        
        // Check to make sure we own this head icon
        if (data.getHeadType() == 3) {
            // Character skin icon
            return this.hasSkin(id);
        } else {
            // Extra head icon
            if (!this.getHeadIcons().contains(id)) {
                return false;
            }
        }
        
        // Unknown
        return true;
    }
    
    public boolean addHeadIcon(int id) {
        // Make sure we are not adding duplicates
        if (this.getHeadIcons().contains(id)) {
            return false;
        }
        
        // Add
        this.getHeadIcons().add(id);
        
        // Save to database
        Nebula.getGameDatabase().addToSet(this, this.getUid(), "headIcons", id);
        
        // Success
        return true;
    }
    
    public boolean addTitle(int id) {
        // Make sure we are not adding duplicates
        if (this.getTitles().contains(id)) {
            return false;
        }
        
        // Add
        this.getTitles().add(id);
        
        // Save to database
        Nebula.getGameDatabase().addToSet(this, this.getUid(), "titles", id);
        
        // Success
        return true;
    }
    
    public IntCollection getAllHonorTitles() {
        // Setup int collection
        var list = new IntOpenHashSet();
        
        // Add character honor titles
        for (var character : getPlayer().getCharacters().getCharacterCollection()) {
            // Check affinity level
            if (character.getAffinityLevel() < GameConstants.AFFINITY_HONOR_UNLOCK_LEVEL) {
                continue;
            }
            
            // Get honor data
            var honor = character.getData().getHonor();
            if (honor == null) continue;
            
            // Add to honor list
            list.add(honor.getId());
        }
        
        // Finally, add extra honor titles
        list.addAll(this.getHonorList());
        
        // Complete and return
        return list;
    }
    
    public boolean addHonor(int id) {
        // Make sure we are not adding duplicates
        if (this.getHonorList().contains(id)) {
            return false;
        }
        
        // Add
        this.getHonorList().add(id);
        
        // Save to database
        Nebula.getGameDatabase().addToSet(this, this.getUid(), "honorList", id);
        
        // Success
        return true;
    }
    
    public boolean hasHonor(int id) {
        // Check if honor title is default or if the player owns it
        if (id == GameConstants.DEFAULT_HONOR_ID || this.getHonorList().contains(id)) {
            return true;
        }
        
        // Get honor data
        var honor = GameData.getHonorDataTable().get(id);
        if (honor == null) return false;
        
        // Check if honor title is a trekker affinity title
        if (honor.isCharacterHonor()) {
            int charId = honor.getCharacterId();
            var character = this.getPlayer().getCharacters().getCharacterById(charId);
            
            if (character != null && character.getAffinityLevel() >= GameConstants.AFFINITY_HONOR_UNLOCK_LEVEL) {
                return true;
            }
        }
        
        // Failure
        return false;
    }
    
    // Resources
    
    public synchronized int getResourceCount(int id) {
        return this.resources.get(id);
    }
    
    // Items
    
    public synchronized int getItemCount(int id) {
        return this.getItems().get(id);
    }
    
    // Add/Remove items
    
    public PlayerChangeInfo addItem(int id, int count) {
        return this.addItem(id, count, null);
    }
    
    public synchronized PlayerChangeInfo addItem(int id, int count, PlayerChangeInfo change) {
        // Changes
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Sanity check
        if (id <= 0 || count == 0) {
            return change;
        }
        
        // Get game data
        var data = GameData.getItemDataTable().get(id);
        if (data == null) {
            return change;
        }
        
        // Set amount
        int amount = count;
        
        // Add item
        switch (data.getItemType()) {
            case Res -> {
                int oldAmount = this.resources.get(id);
                int newAmount = oldAmount;
                int diff = 0;
                
                if (amount > 0) {
                    // Add resource
                    newAmount = Utils.safeAdd(oldAmount, amount);
                    diff = newAmount - oldAmount;
                    
                    // Set
                    this.resources.put(id, newAmount);
                } else {
                    // Remove resource
                    newAmount = Utils.safeSubtract(oldAmount, Math.abs(amount));
                    diff = newAmount - oldAmount;
                    
                    // Set
                    if (newAmount > 0) {
                        this.resources.put(id, newAmount);
                    } else {
                        this.resources.remove(id);
                    }
                }
                
                // Update in database
                if (newAmount > 0) {
                    Nebula.getGameDatabase().update(this, this.getPlayerUid(), "resources." + id, newAmount);
                } else {
                    Nebula.getGameDatabase().updateUnset(this, this.getPlayerUid(), "resources." + id);
                }
                
                if (diff != 0) {
                    var proto = Res.newInstance()
                            .setTid(id)
                            .setQty(diff);
                    
                    change.add(proto);
                }
            }
            case Item -> {
                // Check if item is a random package
                if (data.getItemSubType() == ItemSubType.RandomPackage && data.getUseParams() != null) {
                    // Cannot remove packages
                    if (count <= 0) break;
                    
                    // Add random packages
                    for (var entry : data.getUseParams()) {
                        int pkgId = entry.getIntKey();
                        int pkgCount = entry.getIntValue() * count;
                        
                        for (int i = 0; i < pkgCount; i++) {
                            int pkgDropId = DropPkgDef.getRandomDrop(pkgId);
                            this.addItem(pkgDropId, 1, change);
                        }
                    }
                    
                    // End early
                    break;
                }
                
                // Get item
                int oldAmount = this.items.get(id);
                int newAmount = oldAmount;
                int diff = 0;
                
                if (amount > 0) {
                    // Add resource
                    newAmount = Utils.safeAdd(oldAmount, amount);
                    diff = newAmount - oldAmount;
                    
                    // Set
                    this.items.put(id, newAmount);
                } else {
                    // Remove resource
                    newAmount = Utils.safeSubtract(oldAmount, Math.abs(amount));
                    diff = newAmount - oldAmount;
                    
                    // Set
                    if (newAmount > 0) {
                        this.items.put(id, newAmount);
                    } else {
                        this.items.remove(id);
                    }
                }
                
                // Update in database
                if (newAmount > 0) {
                    Nebula.getGameDatabase().update(this, this.getPlayerUid(), "items." + id, newAmount);
                } else {
                    Nebula.getGameDatabase().updateUnset(this, this.getPlayerUid(), "items." + id);
                }
                
                if (diff != 0) {
                    var proto = Item.newInstance()
                            .setTid(id)
                            .setQty(diff);
                    
                    change.add(proto);
                }
            }
            case Disc -> {
                // Cannot remove discs
                if (amount <= 0) break;
                
                // Get disc data
                var discData = GameData.getDiscDataTable().get(id);
                if (discData == null) break;
                
                // Add transform item instead if we already have this disc
                if (getPlayer().getCharacters().hasDisc(id)) {
                    this.addItem(discData.getTransformItemId(), amount, change);
                    break;
                }
                
                // Add disc
                var disc = getPlayer().getCharacters().addDisc(discData);
                
                // Add to change info
                if (disc != null) {
                    change.add(disc.toProto());
                } else {
                    amount = 0;
                }
            }
            case Char -> {
                // Cannot remove characters
                if (amount <= 0) break;
                
                // Get character data
                var charData = GameData.getCharacterDataTable().get(id);
                if (charData == null) break;
                
                // Add transform item instead if we already have this character
                if (getPlayer().getCharacters().hasCharacter(id)) {
                    this.addItem(charData.getFragmentsId(), charData.getTransformQty(), change);
                    break;
                }
                
                // Add character
                var character = getPlayer().getCharacters().addCharacter(charData);
                
                // Add to change info
                if (character != null) {
                    change.add(character.toProto());
                } else {
                    amount = 0;
                }
            }
            case Energy -> {
                this.getPlayer().addEnergy(amount, change);
            }
            case WorldRankExp -> {
                this.getPlayer().addExp(amount, change);
            }
            case CharacterSkin -> {
                // Cannot remove skins
                if (amount <= 0) {
                    break;
                }
                
                // Get skin data
                var skinData = GameData.getCharacterSkinDataTable().get(id);
                if (skinData == null) {
                    break;
                }
                
                // Check
                if (skinData.getType() >= 3) {
                    this.addSkin(id);
                }
            }
            case Title -> {
                // Cannot remove titles
                if (amount <= 0) {
                    break;
                }
                
                // Get title data
                if (data.getTitleData() == null) {
                    break;
                }
                
                int titleId = data.getTitleData().getId();
                
                // Add title
                if (this.addTitle(titleId)) {
                    // Add to change info
                    var proto = Title.newInstance()
                            .setTitleId(titleId);
                    
                    change.add(proto);
                }
            }
            case Honor -> {
                // Cannot remove honor title
                if (amount <= 0) {
                    break;
                }
                
                // Add honor title
                if (this.addHonor(id)) {
                    // Add to change info
                    var proto = Honor.newInstance()
                            .setNewId(id);
                    
                    change.add(proto);
                }
            }
            default -> {
                // Not implemented
            }
        }
        
        // Trigger quest + achievement
        if (amount > 0) {
            this.getPlayer().trigger(QuestCondition.ItemsAdd, amount, id);
        } else if (amount < 0) {
            this.getPlayer().trigger(QuestCondition.ItemsDeplete, Math.abs(amount), id);
        }
        
        //
        return change;
    }

    @Deprecated
    public synchronized PlayerChangeInfo addItems(List<ItemParam> params, PlayerChangeInfo change) {
        // Changes
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        for (ItemParam param : params) {
            this.addItem(param.getId(), param.getCount(), change);
        }
        
        return change;
    }
    
    public synchronized PlayerChangeInfo addItems(ItemParamMap params) {
        return this.addItems(params, null);
    }
    
    public synchronized PlayerChangeInfo addItems(ItemParamMap params, PlayerChangeInfo change) {
        // Changes
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Sanity
        if (params == null || params.isEmpty()) {
            return change;
        }
        
        // Add items
        for (var param : params.entries()) {
            this.addItem(param.getIntKey(), param.getIntValue(), change);
        }
        
        return change;
    }
    
    public PlayerChangeInfo removeItem(int id, int count) {
        return this.removeItem(id, count, null);
    }
    
    public synchronized PlayerChangeInfo removeItem(int id, int count, PlayerChangeInfo change) {
        if (count > 0) {
            count = -count;
        }
        
        return this.addItem(id, count, change);
    }
    
    public synchronized PlayerChangeInfo removeItems(ItemParamMap params) {
        return this.removeItems(params, null);
    }
    
    public synchronized PlayerChangeInfo removeItems(ItemParamMap params, PlayerChangeInfo change) {
        // Changes
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Sanity
        if (params == null || params.isEmpty()) {
            return change;
        }
        
        // Remove items
        for (var param : params.entries()) {
            this.removeItem(param.getIntKey(), param.getIntValue(), change);
        }
        
        return change;
    }
    
    /**
     * Checks if the player has enough quanity of this item
     */
    public synchronized boolean hasItem(int id, int count) {
        // Sanity check
        if (count == 0) {
            return true;
        } else if (count < 0) {
            // Return false if we are trying to verify negative numbers
            return false;
        }
        
        // Get game data
        var data = GameData.getItemDataTable().get(id);
        if (data == null) {
            return false;
        }
        
        boolean result = switch (data.getItemType()) {
            case Res -> {
                yield this.getResourceCount(id) >= count;
            }
            case Item -> {
                yield this.getItemCount(id) >= count;
            }
            case Disc -> {
                yield getPlayer().getCharacters().hasDisc(id);
            }
            case Char -> {
                yield getPlayer().getCharacters().hasCharacter(id);
            }
            case CharacterSkin -> {
                yield this.hasSkin(id);
            }
            case Title -> {
                yield this.getTitles().contains(id);
            }
            default -> {
                // Not implemented
                yield false;
            }
        };
        
        //
        return result;
    }
    
    public synchronized boolean hasItems(ItemParamMap params) {
        boolean hasItems = true;
        
        for (var param : params.entries()) {
            hasItems = this.hasItem(param.getIntKey(), param.getIntValue());
            
            if (!hasItems) {
                return hasItems;
            }
        }
        
        return hasItems;
    }
    
    // Utility functions
    
    public PlayerChangeInfo produce(int id, int num, PlayerChangeInfo change) {
        // Init change info
        if (change == null) {
            change = new PlayerChangeInfo();
        }
        
        // Get production data
        var data = GameData.getProductionDataTable().get(id);
        if (data == null) {
            return change;
        }
        
        // Get materials
        var materials = data.getMaterials().mulitply(num);
        
        // Verify that we have the materials
        if (!this.hasItems(materials)) {
            return change;
        }
        
        // Remove items
        this.removeItems(materials, change);
        
        // Add produced items
        this.addItem(data.getProductionId(), data.getProductionPerBatch() * num, change);
        
        // Trigger achievement
        this.getPlayer().trigger(AchievementCondition.ItemsProductTotal, num);
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo buyEnergy(int count) {
        // Validate count
        if (count <= 0 || count > 6) {
            return null;
        }
        
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Make sure we have the gems
        int cost = 30 * count;
        
        if (!this.hasItem(GameConstants.ENERGY_BUY_ITEM_ID, cost)) {
            return change;
        }
        
        // Remove gems
        this.removeItem(GameConstants.ENERGY_BUY_ITEM_ID, cost, change);
        
        // Add energy
        this.getPlayer().addEnergy(60 * count, change);
        
        // Success
        return change;
    }
    
    public PlayerChangeInfo buyMallItem(MallShopDef data, int buyCount) {
        // Check stock
        int stock = data.getStock(this.getPlayer());
        if (buyCount > stock) {
            return null;
        }
        
        // Buy item
        var change = this.buyItem(data.getExchangeItemId(), data.getExchangeItemQty(), data.getProducts(), buyCount);
        
        if (change == null) {
            return null;
        }
        
        // Update purchase limit
        this.getMallBuyCount().addTo(data.getIdString(), buyCount);
        Nebula.getGameDatabase().update(
            this,
            getUid(),
            "mallBuyCount." + data.getIdString(),
            getMallBuyCount().get(data.getIdString())
        );
        
        // Return
        return change;
    }
    
    public PlayerChangeInfo buyShopItem(ResidentGoodsDef data, int buyCount) {
        // Check stock
        int stock = data.getStock(this.getPlayer());
        if (buyCount > stock) {
            return null;
        }
        
        // Buy item
        var change = this.buyItem(data.getCurrencyItemId(), data.getPrice(), data.getProducts(), buyCount);
        
        if (change == null) {
            return null;
        }
        
        // Update purchase limit
        this.getShopBuyCount().add(data.getId(), buyCount);
        Nebula.getGameDatabase().update(
            this,
            getUid(),
            "shopBuyCount." + data.getId(),
            getShopBuyCount().get(data.getId())
        );
        
        // Return
        return change;
    }
    
    public PlayerChangeInfo buyItem(int currencyId, int currencyCount, ItemParamMap buyItems, int buyCount) {
        // Sanity check
        if (buyCount <= 0) {
            return null;
        }
        
        // Make sure we have the currency
        int cost = buyCount * currencyCount;
        
        if (!this.hasItem(currencyId, cost)) {
            return null;
        }

        // Player change info
        var change = new PlayerChangeInfo();
        
        // Remove currency item
        this.removeItem(currencyId, cost, change);
        
        // Add items
        this.addItems(buyItems.mulitply(buyCount), change);
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo useItem(int id, int count, int selectId, PlayerChangeInfo change) {
        // Player change info
        if (change == null) {
            change = new PlayerChangeInfo();
        }

        // Sanity check
        count = Math.max(count, 1);
        
        // Get item data
        var data = GameData.getItemDataTable().get(id);
        if (data == null || data.getUseParams() == null) {
            return change;
        }
        
        // Make sure we have this item
        if (!this.hasItem(id, count)) {
            return change;
        }
        
        // Success
        boolean success = false;
        
        // Apply use
        switch (data.getUseAction()) {
            case 2 -> {
                // Add items
                this.addItems(data.getUseParams().mulitply(count), change);
                
                // Success
                success = true;
            }
            case 3 -> {
                // Selected item
                int selectCount = data.getUseParams().get(selectId) * count;
                
                if (selectCount <= 0) {
                    return change;
                }
                
                // Add selected item
                this.addItem(selectId, selectCount, change);
                
                // Success
                success = true;
            }
            default -> {
                // Not implemented
            }
        }

        // Consume item if successful
        if (success) {
            this.removeItem(id, count, change);
        }
        
        // Success
        return change.setSuccess(true);
    }
    
    public PlayerChangeInfo convertGems(int amount) {
        // Verify that we have the gems
        if (!this.hasItem(GameConstants.PREM_GEM_ITEM_ID, amount)) {
            return null;
        }
        
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Convert gems
        this.removeItem(GameConstants.PREM_GEM_ITEM_ID, amount, change);
        this.addItem(GameConstants.GEM_ITEM_ID, amount, change);
        
        // Success
        return change.setSuccess(true);
    }
    
    public void resetShopPurchases() {
        // Clear shop purchases if it's not empty
        if (!this.getShopBuyCount().isEmpty()) {
            this.getShopBuyCount().clear();
            Nebula.getGameDatabase().update(this, this.getUid(), "shopBuyCount", this.getShopBuyCount());
        }
        
        // Clear mall purchases if it's not empty
        if (!this.getMallBuyCount().isEmpty()) {
            this.getMallBuyCount().clear();
            Nebula.getGameDatabase().update(this, this.getUid(), "mallBuyCount", this.getMallBuyCount());
        }
    }
    
    // Database
    
    @SuppressWarnings("deprecation")
    public void migrateFromDatabase() {
        var db = Nebula.getGameDatabase();
        boolean save = false;
        
        // Check if we need to handle inventory migration
        if (this.items == null) {
            this.items = new ItemParamMap();
            
            // Get inventory items from database
            db.getObjects(GameItem.class, "playerUid", getPlayerUid()).forEach(item -> {
                // Get data
                var data = GameData.getItemDataTable().get(item.getItemId());
                if (data == null) return;
                
                // Add
                this.items.put(item.getItemId(), item.getCount());
            });
            
            // Delete all inventory items
            db.getDatastore().getCollection(GameItem.class).deleteMany(Filters.eq("playerUid", uid));
            
            // Set to save inventory to database
            save = true;
        }
        
        if (this.resources == null) {
            this.resources = new ItemParamMap();
            
            // Get inventory resources from database
            db.getObjects(GameResource.class, "playerUid", getPlayerUid()).forEach(res -> {
                // Get data
                var data = GameData.getItemDataTable().get(res.getResourceId());
                if (data == null) return;
                
                // Add
                this.resources.put(res.getResourceId(), res.getCount());
            });
            
            // Delete all inventory resources
            db.getDatastore().getCollection(GameResource.class).deleteMany(Filters.eq("playerUid", uid));
            
            // Set to save inventory to database
            save = true;
        }
        
        // Update in database
        if (save) {
            this.save();
        }
    }
}
