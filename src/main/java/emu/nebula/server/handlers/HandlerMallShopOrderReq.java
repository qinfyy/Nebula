package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.MallShopOrder.MallShopOrderReq;
import emu.nebula.net.HandlerId;
import emu.nebula.data.GameData;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.mall_shop_order_req )
public class HandlerMallShopOrderReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = MallShopOrderReq.parseFrom(message);
        
        // Get package data
        var data = GameData.getMallShopDataTable().get(req.getId().hashCode());
        if (data == null) {
            return session.encodeMsg(NetMsgId.mall_shop_order_failed_ack);
        }
        
        // Buy items
        var change = session.getPlayer().getInventory().buyItem(
                data.getExchangeItemId(),
                data.getExchangeItemQty(),
                data.getProducts(),
                req.getQty()
        );
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.mall_shop_order_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.mall_shop_order_succeed_ack, change.toProto());
    }

}
