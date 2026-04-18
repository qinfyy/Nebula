package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PotentialPreselectionImport.PotentialPreselectionImportReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.potential_preselection_import_req)
public class HandlerPotentialPreselectionImportReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = PotentialPreselectionImportReq.parseFrom(message);

        // Import preset
        var preset = session.getPlayer().getStarTowerManager().importPreset(
            req.getName(),
            req.getPreference(),
            req.getCharPotentials()
        );

        // Check if there was an error with importing the preset
        if (preset == null) {
            return session.encodeMsg(NetMsgId.potential_preselection_import_failed_ack);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.potential_preselection_import_succeed_ack, preset.toProto());
    }

}
