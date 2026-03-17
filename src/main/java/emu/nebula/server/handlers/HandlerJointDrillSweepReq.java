package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.JointDrillSweep.JointDrillSweepReq;
import emu.nebula.proto.JointDrillSweep.JointDrillSweepResp;
import emu.nebula.proto.Public.ItemTpls;
import emu.nebula.net.HandlerId;

import java.util.List;

import emu.nebula.game.activity.type.JointDrillActivity;
import emu.nebula.game.inventory.ItemParamMap;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.joint_drill_sweep_req)
public class HandlerJointDrillSweepReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Get joint drill activity
        var activity = session.getPlayer().getActivityManager().getFirstActivity(JointDrillActivity.class);
        
        if (activity == null) {
            return session.encodeMsg(NetMsgId.joint_drill_sweep_failed_ack);
        }
        
        // Parse request
        var req = JointDrillSweepReq.parseFrom(message);
        
        // Apply for joint drill stage
        var change = activity.sweep(req.getLevelId(), req.getCount());
        
        if (change == null) {
            return session.encodeMsg(NetMsgId.joint_drill_sweep_failed_ack);
        }
        
        // Create response packet
        var rsp = JointDrillSweepResp.newInstance()
                .setChange(change.toProto());
        
        // Encode sweep rewards to proto
        @SuppressWarnings("unchecked")
        var list = (List<ItemParamMap>) change.getExtraData();
        
        for (var rewards : list) {
            var templates = ItemTpls.newInstance();
            rewards.toItemTemplateStream().forEach(templates::addItems);
            rsp.addRewards(templates);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.joint_drill_sweep_succeed_ack, rsp);
    }

}
