package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.net.HandlerId;
import emu.nebula.Nebula;
import emu.nebula.net.GameSession;
import emu.nebula.proto.GachaSpin.GachaCard;
import emu.nebula.proto.GachaSpin.GachaSpinReq;
import emu.nebula.proto.GachaSpin.GachaSpinResp;
import emu.nebula.proto.Public.ItemTpl;

@HandlerId(NetMsgId.gacha_spin_req)
public class HandlerGachaSpinReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = GachaSpinReq.parseFrom(message);
        
        // Do gacha
        var result = Nebula.getGameContext().getGachaModule().spin(
            session.getPlayer(),
            req.getId(),
            req.getMode()
        );
        
        if (result == null) {
            return session.encodeMsg(NetMsgId.gacha_spin_failed_ack);
        }
        
        // Build response
        var rsp = GachaSpinResp.newInstance()
                .setTime(Nebula.getCurrentTime())
                .setAMissTimes(result.getInfo().getMissTimesA())
                .setAupMissTimes(result.getInfo().getMissTimesA())
                .setTotalTimes(result.getInfo().getTotal())
                .setGachaTotalTimes(result.getInfo().getTotal())
                .setAupGuaranteeTimes(result.getInfo().isUsedGuarantee() ? 0 : 1)
                .setChange(result.getChange().toProto());
        
        for (int id : result.getCards()) {
            var card = GachaCard.newInstance()
                    .setCard(ItemTpl.newInstance().setTid(id).setQty(1));
            
            rsp.addCards(card);
        }
        
        // Encode and send response
        return session.encodeMsg(NetMsgId.gacha_spin_succeed_ack, rsp);
    }

}
