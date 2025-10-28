package emu.nebula.server.handlers;

import emu.nebula.net.NetHandler;
import emu.nebula.net.NetMsgId;
import emu.nebula.proto.StorySetInfo.StorySetChapter;
import emu.nebula.proto.StorySetInfo.StorySetInfoResp;
import emu.nebula.net.HandlerId;
import emu.nebula.data.resources.StorySetSectionDef;
import emu.nebula.net.GameSession;

@HandlerId(NetMsgId.story_set_info_req)
public class HandlerStorySetInfoReq extends NetHandler {

    @Override
    public byte[] handle(GameSession session, byte[] message) throws Exception {
        var rsp = StorySetInfoResp.newInstance();
        
        for (int chapterId : StorySetSectionDef.getChapterIds()) {
            var chapter = StorySetChapter.newInstance()
                    .setChapterId(chapterId);
            
            rsp.addChapters(chapter);
        }
        
        return this.encodeMsg(NetMsgId.story_set_info_succeed_ack, rsp);
    }

}
