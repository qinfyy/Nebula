package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ActivityDetail.ActivityResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_detail_req)
public class HandlerActivityDetailReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Build response
        var rsp = ActivityResp.newInstance();
        
        for (var activity : session.getPlayer().getActivityManager().getActivities().values()) {
            rsp.addList(activity.toMsgProto());
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.activity_detail_succeed_ack, rsp);
    }

}
