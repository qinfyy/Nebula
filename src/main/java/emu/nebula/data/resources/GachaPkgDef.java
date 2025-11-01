package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.ResourceType;
import emu.nebula.data.ResourceType.LoadPriority;
import emu.nebula.util.WeightedList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

@Getter
@ResourceType(name = "GachaPkg.json", loadPriority = LoadPriority.LOW)
public class GachaPkgDef extends BaseDef {
    private int PkgId;
    private int GoodsId;
    private int Weight;
    
    private static final Int2ObjectMap<WeightedList<Integer>> packages = new Int2ObjectOpenHashMap<>();
    
    @Override @Deprecated
    public int getId() {
        return PkgId;
    }
    
    public static WeightedList<Integer> getPackageById(int packageId) {
        return packages.get(packageId);
    }
    
    @Override
    public void onLoad() {
        // Add to package
        var list = packages.computeIfAbsent(this.getPkgId(), i -> new WeightedList<Integer>());
        list.add(this.getWeight(), this.getGoodsId());
    }
}
