package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.VampireSurvivorSettle.VampireSurvivorSettleReq;
import emu.nebula.proto.VampireSurvivorSettle.VampireSurvivorSettleResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.vampire_survivor_settle_req)
public class HandlerVampireSurvivorSettleReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = VampireSurvivorSettleReq.parseFrom(message);
        
        // Get vampire survivor game
        var game = session.getPlayer().getVampireSurvivorManager().getGame();
        
        if (game == null) {
            session.encodeMsg(NetMsgId.vampire_survivor_settle_failed_ack);
        }
        
        // Settle area
        game.settleArea(req.getTime(), req.getKillCount().toArray());
        
        // Calculate victory
        boolean victory = !req.getDefeat();
        int score = game.getTotalScore();
        
        // Settle game
        session.getPlayer().getVampireSurvivorManager().settle(victory, score);
        
        // Build response
        var rsp = VampireSurvivorSettleResp.newInstance();
        
        if (victory) {
            rsp.getMutableVictory()
                .setFinalScore(score);
            
            for (var a : game.getAreas()) {
                rsp.getMutableVictory().addInfos(a.toProto());
            }
        } else {
            rsp.getMutableDefeat()
                .setFinalScore(score);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.vampire_survivor_settle_succeed_ack, rsp);
    }
}
