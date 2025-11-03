package emu.nebula.net;

import org.reflections.Reflections;

import com.esotericsoftware.reflectasm.MethodAccess;

import emu.nebula.Nebula;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.SneakyThrows;
import us.hebi.quickbuf.ProtoMessage;
import us.hebi.quickbuf.ProtoSink;

public class PacketHelper {
    private static Object2IntMap<Class<?>> methodIndexCache = new Object2IntOpenHashMap<>();
    
    // Next packages
    
    public static void cacheProtos() {
        var classes = new Reflections(Nebula.class.getPackage().getName()).getSubTypesOf(ProtoMessage.class);
        
        for (var cls : classes) {
            try {
                var access = MethodAccess.get(cls);
                int index = access.getIndex("setNextPackage");
                methodIndexCache.put(cls, index);
            } catch (Exception e) {
                
            }
        }
        
        Nebula.getLogger().info("Cached " + methodIndexCache.size() + " proto methods.");
    }
    
    public static boolean hasNextPackageMethod(Object obj) {
        return methodIndexCache.containsKey(obj.getClass());
    }
    
    public static void setNextPackage(ProtoMessage<?> proto, byte[] data) {
        int index = methodIndexCache.getInt(proto.getClass());
        MethodAccess.get(proto.getClass()).invoke(proto, index, data);
    }
    
    // Packet encoding

    public static byte[] encodeMsg(int msgId, byte[] packet) {
        // Create data array
        byte[] data = new byte[packet.length + 2];
        
        // Encode msgId
        short id = (short) msgId;
        data[0] = (byte) (id >> 8);
        data[1] = (byte) id;
        
        // Copy packet to data array
        System.arraycopy(packet, 0, data, 2, packet.length);
        
        // Complete
        return data;
    }

    @SneakyThrows
    public static byte[] encodeMsg(int msgId, ProtoMessage<?> proto) {
        // Create data array
        byte[] data = new byte[proto.getCachedSize() + 2];
        
        // Encode msgId
        short id = (short) msgId;
        data[0] = (byte) (id >> 8);
        data[1] = (byte) id;
        
        // Create proto sink
        var output = ProtoSink.newInstance(data, 2, proto.getCachedSize());
        
        // Copy packet to data array
        proto.writeTo(output);
        
        // Complete
        return data;
    }
    
    public static byte[] encodeMsg(int msgId) {
        // Create data array
        byte[] data = new byte[2];
        
        // Encode msgId
        short id = (short) msgId;
        data[0] = (byte) (id >> 8);
        data[1] = (byte) id;
        
        return data;
    }
    
}
