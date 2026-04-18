package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PotentialPreselectionUpdate.PotentialPreselectionUpdateReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.potential_preselection_update_req)
public class HandlerPotentialPreselectionUpdateReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = PotentialPreselectionUpdateReq.parseFrom(message);

        // Get preset
        var preset = session.getPlayer().getStarTowerManager().getPresetById(req.getId());

        if (preset == null) {
            return session.encodeMsg(NetMsgId.potential_preselection_update_failed_ack);
        }

        // Update potentials
        preset.updatePotentials(req.getCharPotentials());
        preset.save();

        // Encode and send
        return session.encodeMsg(NetMsgId.potential_preselection_update_succeed_ack, preset.toProto());
    }

}
