package emu.nebula.net;

import lombok.Getter;
import us.hebi.quickbuf.ProtoMessage;

@Getter
public class NetMsgPacket {
    private int msgId;
    private ProtoMessage<?> proto;
    
    public NetMsgPacket(int msgId, ProtoMessage<?> proto) {
        this.msgId = msgId;
        this.proto = proto;
    }
    
    public byte[] toByteArray() {
        return PacketHelper.encodeMsg(this.getMsgId(), this.getProto());
    }
}
