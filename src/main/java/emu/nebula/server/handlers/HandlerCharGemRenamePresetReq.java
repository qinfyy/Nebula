package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.CharGemRenamePreset.CharGemRenamePresetReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.char_gem_rename_preset_req)
public class HandlerCharGemRenamePresetReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = CharGemRenamePresetReq.parseFrom(message);
        
        // Get character
        var character = session.getPlayer().getCharacters().getCharacterById(req.getCharId());
        
        if (character == null) {
            return session.encodeMsg(NetMsgId.char_gem_rename_preset_failed_ack);
        }
        
        // Rename
        boolean success = character.renameGemPreset(req.getPresetId(), req.getNewName());
        
        if (success == false) {
            return session.encodeMsg(NetMsgId.char_gem_rename_preset_failed_ack);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.char_gem_rename_preset_succeed_ack);
    }

}
