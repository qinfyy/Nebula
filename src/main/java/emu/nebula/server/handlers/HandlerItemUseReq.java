package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ItemUse.ItemUseReq;
import emu.nebula.net.HandlerId;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.item_use_req)
public class HandlerItemUseReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = ItemUseReq.parseFrom(message);
        
        // Sanity check
        if (req.getUse() == null) {
            return session.encodeMsg(NetMsgId.item_use_failed_ack);
        }
        
        // Create change info
        var change = new PlayerChangeInfo();
        
        // Use item
        for (var param : req.getUse().getList()) {
            session.getPlayer().getInventory().useItem(param.getTid(), param.getQty(), 0, change);
        }
        
        // Pick item
        for (var param : req.getPick().getList()) {
            session.getPlayer().getInventory().useItem(param.getTid(), param.getQty(), param.getSelectTid(), change);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.item_use_succeed_ack, change.toProto());
    }

}
