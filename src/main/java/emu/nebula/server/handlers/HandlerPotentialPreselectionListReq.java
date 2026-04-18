package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PotentialPreselectionListOuterClass.PotentialPreselectionList;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.potential_preselection_list_req)
public class HandlerPotentialPreselectionListReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Create base response
        var rsp = PotentialPreselectionList.newInstance();
        
        // Add potential presets
        var presets = session.getPlayer().getStarTowerManager().getPresets().values();
        for (var preset : presets) {
            rsp.addList(preset.toProto());
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.potential_preselection_list_succeed_ack, rsp);
    }

}
