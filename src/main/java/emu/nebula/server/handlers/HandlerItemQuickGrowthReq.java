package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ItemQuickGrowth.ItemGrowthReq;
import emu.nebula.net.HandlerId;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.item_quick_growth_req)
public class HandlerItemQuickGrowthReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = ItemGrowthReq.parseFrom(message);
        
        // Init change
        var change = new PlayerChangeInfo();
        
        // Create items
        for (var item : req.getList()) {
            // Craft item
            if (item.hasProduct()) {
                session.getPlayer().getInventory().produce(
                        item.getProduct().getId(),
                        item.getProduct().getNum(),
                        change
                );
            }
            // Select item from selector
            if (item.hasPick()) {
                for (var pick : item.getPick().getList()) {
                    session.getPlayer().getInventory().useItem(
                            pick.getTid(),
                            pick.getQty(),
                            pick.getSelectTid(),
                            change
                    );
                }
            }
        }
        
        if (change.isEmpty()) {
            return session.encodeMsg(NetMsgId.item_quick_growth_failed_ack);
        }
        
        // Send response
        return session.encodeMsg(NetMsgId.item_quick_growth_succeed_ack, change.toProto());
    }

}
