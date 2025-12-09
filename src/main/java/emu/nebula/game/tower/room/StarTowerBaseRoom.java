package emu.nebula.game.tower.room;

import java.util.Arrays;
import java.util.Objects;

import emu.nebula.GameConstants;
import emu.nebula.data.GameData;
import emu.nebula.data.resources.StarTowerEventDef;
import emu.nebula.data.resources.StarTowerStageDef;
import emu.nebula.game.tower.StarTowerGame;
import emu.nebula.game.tower.StarTowerModifiers;
import emu.nebula.game.tower.cases.CaseType;
import emu.nebula.game.tower.cases.StarTowerBaseCase;
import emu.nebula.game.tower.cases.StarTowerNpcEventCase;
import emu.nebula.game.tower.cases.StarTowerSyncHPCase;
import emu.nebula.proto.PublicStarTower.InteractEnterReq;
import emu.nebula.proto.PublicStarTower.StarTowerRoomCase;
import emu.nebula.proto.PublicStarTower.StarTowerRoomData;
import emu.nebula.proto.StarTowerApply.StarTowerApplyReq;
import emu.nebula.util.Utils;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import lombok.Getter;

import us.hebi.quickbuf.RepeatedMessage;

@Getter
public class StarTowerBaseRoom {
    // Game
    private transient StarTowerGame game;
    private transient StarTowerStageDef stage;
    
    // Map info
    private int mapId;
    private int mapTableId;
    private String mapParam;
    private int paramId;
    
    // Cases
    private int lastCaseId = 0;
    private Int2ObjectMap<StarTowerBaseCase> cases;
    
    // Misc
    private boolean hasDoor;
    
    public StarTowerBaseRoom(StarTowerGame game, StarTowerStageDef stage) {
        this.game = game;
        this.stage = stage;
        this.cases = new Int2ObjectLinkedOpenHashMap<>();
    }
    
    public RoomType getType() {
        return stage.getRoomType();
    }
    
    public boolean hasDoor() {
        return this.hasDoor;
    }
    
    public StarTowerModifiers getModifiers() {
        return this.getGame().getModifiers();
    }
    
    public StarTowerBaseCase createExit() {
        return this.getGame().createExit();
    }
    
    // Map info
    
    public void setMapInfo(StarTowerApplyReq req) {
        this.mapId = req.getMapId();
        this.mapTableId = req.getMapTableId();
        this.mapParam = req.getMapParam();
        this.paramId = req.getParamId();
    }
    
    public void setMapInfo(InteractEnterReq req) {
        this.mapId = req.getMapId();
        this.mapTableId = req.getMapTableId();
        this.mapParam = req.getMapParam();
        this.paramId = req.getParamId();
    }
    
    // NPC events

    private StarTowerEventDef getRandomEvent() {
        /*
        var list = GameData.getStarTowerEventDataTable()
                .values()
                .stream()
                .toList();
        */
        
        var list = Arrays.stream(GameConstants.TOWER_EVENTS_IDS)
                .mapToObj(GameData.getStarTowerEventDataTable()::get)
                .filter(Objects::nonNull)
                .toList();
        
        if (list.isEmpty()) {
            return null;
        }
        
        return Utils.randomElement(list);
    }
    
    public StarTowerBaseCase createNpcEvent() {
        // Get random event
        var event = this.getRandomEvent();
        
        if (event == null) {
            return null;
        }
        
        // Get random npc
        int npcId = Utils.randomElement(event.getRelatedNPCs());
        
        // Create case with event
        return new StarTowerNpcEventCase(npcId, event);
    }
    
    // Cases
    
    public int getNextCaseId() {
        return ++this.lastCaseId;
    }
    
    public StarTowerBaseCase getCase(int id) {
        return this.getCases().get(id);
    }
    
    public StarTowerBaseCase addCase(StarTowerBaseCase towerCase) {
        return this.addCase(null, towerCase);
    }
    
    public StarTowerBaseCase addCase(RepeatedMessage<StarTowerRoomCase> cases, StarTowerBaseCase towerCase) {
        // Sanity check
        if (towerCase == null) {
            return null;
        }
        
        // Set game for tower case
        towerCase.register(this);
        
        // Add to cases list
        this.getCases().put(towerCase.getId(), towerCase);
        
        // Add case to proto
        if (cases != null) {
            cases.add(towerCase.toProto());
        }
        
        // Check if door case
        if (towerCase.getType() == CaseType.OpenDoor) {
            this.hasDoor = true;
        }
        
        // Complete
        return towerCase;
    }
    
    // Events
    
    public void onEnter() {
        // Create sync hp case
        this.addCase(new StarTowerSyncHPCase());
        
        // Create door case
        this.createExit();
    }
    
    // Proto
    
    public emu.nebula.proto.PublicStarTower.StarTowerRoom toProto() {
        var proto = emu.nebula.proto.PublicStarTower.StarTowerRoom.newInstance()
                .setData(this.getDataProto());
        
        for (var towerCase : this.getCases().values()) {
            proto.addCases(towerCase.toProto());
        }
        
        return proto;
    }
    
    private StarTowerRoomData getDataProto() {
        var proto = StarTowerRoomData.newInstance()
                .setFloor(this.getGame().getFloorCount())
                .setMapId(this.getMapId())
                .setRoomType(this.getType().getValue())
                .setMapTableId(this.getMapTableId());
        
        if (this.getMapParam() != null && !this.getMapParam().isEmpty()) {
            proto.setMapParam(this.getMapParam());
        }
        
        if (this.getParamId() != 0) {
            proto.setParamId(this.getParamId());
        }
        
        return proto;
    }
}
