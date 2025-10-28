package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.StorySett.StorySettleReq;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.story_settle_req)
public class HandlerStorySettleReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = StorySettleReq.parseFrom(message);
        
        // Get list of settled story ids
        var list = new IntArrayList();
        
        for (var settle : req.getList()) {
            list.add(settle.getIdx());
        }
        
        // Settle
        var changes = session.getPlayer().getStoryManager().settle(list);
        
        // Finish
        return this.encodeMsg(NetMsgId.story_settle_succeed_ack, changes.toProto());
    }

}
