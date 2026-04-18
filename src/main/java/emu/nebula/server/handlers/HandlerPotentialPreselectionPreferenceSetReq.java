package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.PotentialPreselectionPreferenceSet.PotentialPreselectionPreferenceSetReq;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

import java.util.HashSet;

import emu.nebula.game.tower.StarTowerPotentialPreset;

@HandlerId(NetMsgId.potential_preselection_preference_set_req)
public class HandlerPotentialPreselectionPreferenceSetReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Parse request
        var req = PotentialPreselectionPreferenceSetReq.parseFrom(message);

        // Get list of presets
        var checkInPresets = new HashSet<StarTowerPotentialPreset>();
        var checkOutPresets = new HashSet<StarTowerPotentialPreset>();

        // Get check-in presets
        for (long id : req.getCheckInIds()) {
            var preset = session.getPlayer().getStarTowerManager().getPresetById(id);

            if (preset == null) {
                return session.encodeMsg(NetMsgId.potential_preselection_preference_set_failed_ack);
            }

            checkInPresets.add(preset);
        }

        // Get check-out presets
        for (long id : req.getCheckOutIds()) {
            var preset = session.getPlayer().getStarTowerManager().getPresetById(id);

            if (preset == null) {
                return session.encodeMsg(NetMsgId.potential_preselection_preference_set_failed_ack);
            }

            checkOutPresets.add(preset);
        }

        // Set preference to true for check-in presets
        for (var preset : checkInPresets) {
            preset.setPreference(true);
        }

        // Set preference to false for check-out presets
        for (var preset : checkOutPresets) {
            preset.setPreference(false);
        }

        // Encode and send
        return session.encodeMsg(NetMsgId.potential_preselection_preference_set_succeed_ack);
    }

}
