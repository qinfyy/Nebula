package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.CharGemRefresh.CharGemRefreshReq;
import emu.nebula.proto.CharGemRefresh.CharGemRefreshResp;
import emu.nebula.server.error.NebulaException;
import emu.nebula.net.HandlerId;
import emu.nebula.game.character.CharacterGem;
import emu.nebula.game.player.PlayerChangeInfo;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.char_gem_refresh_req)
public class HandlerCharGemRefreshReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = CharGemRefreshReq.parseFrom(message);
        
        // Get character
        var character = session.getPlayer().getCharacters().getCharacterById(req.getCharId());
        
        if (character == null) {
            return session.encodeMsg(NetMsgId.char_gem_refresh_failed_ack);
        }
        
        // Refresh gem attributes
        PlayerChangeInfo change = null;
        
        try {
            change = character.refreshGem(req.getSlotId(), req.getGemIndex(), req.getLockAttrs());
        } catch (NebulaException e) {
            return session.encodeMsg(NetMsgId.char_gem_refresh_failed_ack, e.toProto());
        }
        
        var gem = (CharacterGem) change.getExtraData();
        
        // Build response
        var rsp = CharGemRefreshResp.newInstance()
                .setChangeInfo(change.toProto())
                .addAllAttributes(gem.getAlterAttributes());
        
        // Encode and send
        return session.encodeMsg(NetMsgId.char_gem_refresh_succeed_ack, rsp);
    }

}
