package emu.nebula.game.activity;

import emu.nebula.game.GameContext;
import emu.nebula.game.GameContextModule;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

@Getter
public class ActivityModule extends GameContextModule {
    private IntList activities;

    public ActivityModule(GameContext context) {
        super(context);
        this.activities = new IntArrayList();
        
        // Hardcode these activities for now
        this.activities.add(700102);
        this.activities.add(700103);
        this.activities.add(700104);
        this.activities.add(700107);
        
        //this.activities.add(101002);
        //this.activities.add(101003);
    }
    
}
