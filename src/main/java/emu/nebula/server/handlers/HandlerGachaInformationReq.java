package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.GachaInformation.GachaInfo;
import emu.nebula.proto.GachaInformation.GachaInformationResp;
import emu.nebula.net.HandlerId;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.gacha_information_req)
public class HandlerGachaInformationReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        // Build response
        var rsp = GachaInformationResp.newInstance();
        
        for (var bannerInfo : session.getPlayer().getGachaManager().getBannerInfos()) {
            var info = GachaInfo.newInstance()
                    .setId(bannerInfo.getBannerId())
                    .setGachaTotalTimes(bannerInfo.getTotal())
                    .setTotalTimes(bannerInfo.getTotal())
                    .setAupMissTimes(bannerInfo.getMissTimesA())
                    .setAMissTimes(bannerInfo.getMissTimesA())
                    .setReveFirstTenReward(true)
                    .setRecvGuaranteeReward(bannerInfo.isUsedGuarantee());
            
            rsp.addInformation(info);
        }
        
        // Encode and send
        return session.encodeMsg(NetMsgId.gacha_information_succeed_ack, rsp);
    }

}
