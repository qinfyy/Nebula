package emu.nebula.game.activity;

import dev.morphia.annotations.Entity;
import emu.nebula.Nebula;
import emu.nebula.data.resources.ActivityDef;
import emu.nebula.game.player.Player;
import emu.nebula.proto.ActivityDetail.ActivityMsg;
import emu.nebula.proto.Public.Activity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity(useDiscriminator = true)
public abstract class GameActivity {
    private int id;
    
    @Setter private transient ActivityManager manager;
    @Setter private transient ActivityDef data;
    
    @Deprecated // Morhpia only
    public GameActivity() {
        
    }
    
    public GameActivity(ActivityManager manager, ActivityDef data) {
        this.id = data.getId();
        this.manager = manager;
        this.data = data;
    }
    
    public Player getPlayer() {
        return this.getManager().getPlayer();
    }
    
    public void save() {
        Nebula.getGameDatabase().update(
            this.getManager(),
            this.getManager().getPlayerUid(),
            "activities." + this.getId(), 
            this
        );
    }
    
    // Proto
    
    public Activity toProto() {
        var proto = Activity.newInstance()
                .setId(this.getId())
                .setStartTime(1)
                .setEndTime(Integer.MAX_VALUE);
        
        return proto;
    }
    
    public ActivityMsg toMsgProto() {
        var proto = ActivityMsg.newInstance()
                .setId(this.getId());
        
        this.encodeActivityMsg(proto);
        
        return proto;
    }
    
    public abstract void encodeActivityMsg(ActivityMsg msg);
}
