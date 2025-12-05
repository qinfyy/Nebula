package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_tower_defense_level_apply_req)
public class HandlerActivityTowerDefenseLevelApplyReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Encode and send
        return session.encodeMsg(NetMsgId.activity_tower_defense_level_apply_succeed_ack);
    }

}
