package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.CharGemUpdateGemLockStatus.CharGemUpdateGemLockStatusReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.char_gem_update_gem_lock_status_req)
public class HandlerCharGemUpdateGemLockStatusReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = CharGemUpdateGemLockStatusReq.parseFrom(message);
        
        // Get character
        var character = session.getPlayer().getCharacters().getCharacterById(req.getCharId());
        
        if (character == null) {
            return session.encodeMsg(NetMsgId.char_gem_update_gem_lock_status_failed_ack);
        }
        
        // Lock gem
        boolean success = character.lockGem(req.getSlotId(), req.getGemIndex(), req.getLock());
        
        if (success == false) {
            return session.encodeMsg(NetMsgId.char_gem_update_gem_lock_status_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.char_gem_update_gem_lock_status_succeed_ack);
    }

}
