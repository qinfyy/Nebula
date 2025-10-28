package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ActivityDetail.ActivityMsg;
import emu.nebula.proto.ActivityDetail.ActivityResp;
import emu.nebula.proto.Public.ActivityTrial;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@SuppressWarnings("unused")
@HandlerId(NetMsgId.activity_detail_req)
public class HandlerActivityDetailReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var rsp = ActivityResp.newInstance();
        
        /*
        var activity = ActivityMsg.newInstance()
                .setId(700101)
                .setTrial(ActivityTrial.newInstance());
        
        rsp.addList(activity);
        */
        
        return this.encodeMsg(NetMsgId.activity_detail_succeed_ack, rsp);
    }

}
