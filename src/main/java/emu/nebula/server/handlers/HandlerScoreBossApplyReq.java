package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ScoreBossApply.ScoreBossApplyReq;
import emu.nebula.server.error.NebulaException;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.score_boss_apply_req)
public class HandlerScoreBossApplyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = ScoreBossApplyReq.parseFrom(message);
        
        // Apply
        try {
            session.getPlayer().getScoreBossManager().apply(req.getLevelId(), req.getBuildId());
        } catch (NebulaException e) {
            return session.encodeMsg(NetMsgId.score_boss_apply_failed_ack, e.toProto());
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.score_boss_apply_succeed_ack);
    }

}
