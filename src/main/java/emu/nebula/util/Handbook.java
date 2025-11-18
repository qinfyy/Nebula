package emu.nebula.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.data.resources.CharacterDef;
import emu.nebula.data.resources.ItemDef;

public class Handbook {
    
    public static void generate() {
        // Temp vars
        Map<String, String> languageKey = null;
        List<Integer> list = null;
        
        // Save to file
        String file = "./Nebula Handbook.txt";

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), true)) {
            // Format date for header
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            var time = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).format(dtf);
            
            // Header
            writer.println("# Nebula " + GameConstants.getGameVersion() + " Handbook");
            writer.println("# Created " + time);
            
            // Dump characters
            writer.println(System.lineSeparator());
            writer.println("# Characters");
            list = GameData.getCharacterDataTable().keySet().intStream().sorted().boxed().toList();
            languageKey = loadLanguageKey(CharacterDef.class);
            for (int id : list) {
                CharacterDef data = GameData.getCharacterDataTable().get(id);
                writer.print(data.getId());
                writer.print(" : ");
                writer.println(languageKey.getOrDefault(data.getName(), data.getName()));
            }
            
            // Dump characters
            writer.println(System.lineSeparator());
            writer.println("# Items");
            list = GameData.getItemDataTable().keySet().intStream().sorted().boxed().toList();
            languageKey = loadLanguageKey(ItemDef.class);
            for (int id : list) {
                ItemDef data = GameData.getItemDataTable().get(id);
                writer.print(data.getId());
                writer.print(" : ");
                writer.print(languageKey.getOrDefault(data.getTitle(), data.getTitle()));
                
                writer.print(" [");
                writer.print(data.getItemType());
                writer.println("]");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static Map<String, String> loadLanguageKey(Class<?> resourceClass) {
        // Get type
        ResourceType type = resourceClass.getAnnotation(ResourceType.class);
        if (type == null) {
            return Map.of();
        }
        
        // Load
        Map<String, String> map = null;
        
        try {
            map = JsonUtils.loadToMap(Nebula.getConfig().getResourceDir() + "/language/en_US/" + type.name(), String.class, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (map == null) {
            return Map.of();
        }
        
        return map;
    }
}
