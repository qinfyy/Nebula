package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.HandlerId;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_tower_defense_level_settle_req)
public class HandlerActivityTowerDefenseLevelSettleReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Initialize change info
        // TODO: Handle this properly
        var changeInfo = new PlayerChangeInfo();

        // Encode and send
        return session.encodeMsg(NetMsgId.activity_tower_defense_level_settle_succeed_ack, changeInfo.toProto());
    }

}
