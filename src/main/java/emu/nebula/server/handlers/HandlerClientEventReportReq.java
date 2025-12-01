package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.Public.Nil;
import emu.nebula.net.HandlerId;
import emu.nebula.game.quest.QuestCondition;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.client_event_report_req)
public class HandlerClientEventReportReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Interact
        session.getPlayer().trigger(QuestCondition.ClientReport, 1, 1005);
        
        // Encode response
        return session.encodeMsg(NetMsgId.client_event_report_succeed_ack, Nil.newInstance());
    }

}
