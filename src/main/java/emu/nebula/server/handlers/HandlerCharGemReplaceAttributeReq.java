package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.CharGemReplaceAttribute.CharGemReplaceAttributeReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.char_gem_replace_attribute_req)
public class HandlerCharGemReplaceAttributeReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = CharGemReplaceAttributeReq.parseFrom(message);
        
        // Get character
        var character = session.getPlayer().getCharacters().getCharacterById(req.getCharId());
        
        if (character == null) {
            return session.encodeMsg(NetMsgId.char_gem_replace_attribute_failed_ack);
        }
        
        // Replace attributes
        boolean success = character.replaceGemAttributes(req.getSlotId(), req.getGemIndex());
        
        if (success == false) {
            return session.encodeMsg(NetMsgId.char_gem_replace_attribute_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.char_gem_replace_attribute_succeed_ack);
    }

}
