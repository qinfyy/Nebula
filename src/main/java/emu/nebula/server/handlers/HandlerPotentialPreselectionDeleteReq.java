package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PotentialPreselectionDelete.PotentialPreselectionDeleteReq;
import emu.nebula.net.HandlerId;

import java.util.HashSet;

import emu.nebula.game.tower.StarTowerPotentialPreset;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.potential_preselection_delete_req)
public class HandlerPotentialPreselectionDeleteReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = PotentialPreselectionDeleteReq.parseFrom(message);

        // Get list of presets
        var presets = new HashSet<StarTowerPotentialPreset>();
        
        // Get preset
        for (long id : req.getIds()) {
            var preset = session.getPlayer().getStarTowerManager().getPresetById(id);
            
            if (preset == null) {
                return session.encodeMsg(NetMsgId.potential_preselection_delete_failed_ack);
            }
            
            presets.add(preset);
        }
        
        // Delete presets
        for (var preset : presets) {
            session.getPlayer().getStarTowerManager().deletePreset(preset);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.potential_preselection_delete_succeed_ack);
    }

}
