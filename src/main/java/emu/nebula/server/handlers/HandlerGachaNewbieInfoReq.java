package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.GachaNewbieInfoOuterClass.GachaNewbieInfo;
import emu.nebula.proto.GachaNewbieInfoOuterClass.GachaNewbieInfoResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.gacha_newbie_info_req)
public class HandlerGachaNewbieInfoReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var rsp = GachaNewbieInfoResp.newInstance();
        var info = GachaNewbieInfo.newInstance()
                .setId(5)
                .setReceive(true);
        
        rsp.addList(info);
        
        return session.encodeMsg(NetMsgId.gacha_newbie_info_succeed_ack, rsp);
    }

}
