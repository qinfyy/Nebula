package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PotentialPreselectionNameSet.PotentialPreselectionNameSetReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.potential_preselection_name_set_req)
public class HandlerPotentialPreselectionNameSetReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = PotentialPreselectionNameSetReq.parseFrom(message);

        // Get preset
        var preset = session.getPlayer().getStarTowerManager().getPresetById(req.getId());

        if (preset == null) {
            return session.encodeMsg(NetMsgId.potential_preselection_name_set_failed_ack);
        }

        // Update name
        preset.setName(req.getName());

        // Encode and send
        return session.encodeMsg(NetMsgId.potential_preselection_name_set_succeed_ack);
    }

}
