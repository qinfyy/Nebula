package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.ActivityTrialRewardReceive.ActivityTrialRewardReceiveReq;
import emu.nebula.net.HandlerId;
import emu.nebula.game.activity.type.TrialActivity;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.activity_trial_reward_receive_req)
public class HandlerActivityTrialRewardReceiveReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = ActivityTrialRewardReceiveReq.parseFrom(message);
        
        // Get activity
        var activity = session.getPlayer().getActivityManager().getActivity(TrialActivity.class, req.getActivityId());
        
        if (activity == null) {
            return session.encodeMsg(NetMsgId.activity_trial_reward_receive_failed_ack);
        }
        
        // Recieve reward
        var change = activity.claimReward(req.getGroupId());
        
        // Encode and send
        return session.encodeMsg(NetMsgId.activity_trial_reward_receive_succeed_ack, change.toProto());
    }

}
