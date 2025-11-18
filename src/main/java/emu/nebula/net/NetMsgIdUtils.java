package emu.nebula.net;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.util.JsonUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class NetMsgIdUtils {

    private static Int2ObjectMap<String> msgIdMap;

    static {
        msgIdMap = new Int2ObjectOpenHashMap<>();

        Field[] fields = NetMsgId.class.getFields();

        for (Field f : fields) {
            if (f.getType().equals(int.class)) {
                try {
                    msgIdMap.put(f.getInt(null), f.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getMsgIdName(int msgId) {
        if (msgId <= 0) return "UNKNOWN";
        return msgIdMap.getOrDefault(msgId, "UNKNOWN");
    }

    public static void dumpPacketIds() {
        try (FileWriter writer = new FileWriter("./MsgIds_" + GameConstants.getGameVersion() + ".json")) {
            // Create sorted tree map
            Map<Integer, String> packetIds = msgIdMap.int2ObjectEntrySet().stream()
                    .filter(e -> e.getIntKey() > 0)
                    .collect(Collectors.toMap(Int2ObjectMap.Entry::getIntKey, Int2ObjectMap.Entry::getValue, (k, v) -> v, TreeMap::new));
            // Write to file
            writer.write(JsonUtils.encode(packetIds));
            Nebula.getLogger().info("Dumped packet ids.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
