package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.star_tower_info_req)
public class HandlerStarTowerInfoReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Get star tower game
        var game = session.getPlayer().getStarTowerManager().getGame();
        
        if (game == null) {
            return session.encodeMsg(NetMsgId.star_tower_info_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.star_tower_info_succeed_ack, game.toProto());
    }

}
