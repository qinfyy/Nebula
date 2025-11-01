package emu.nebula.data.resources;

import emu.nebula.data.BaseDef;
import emu.nebula.data.GameData;
import emu.nebula.data.ResourceType;
import emu.nebula.util.WeightedList;
import lombok.Getter;

@Getter
@ResourceType(name = "Gacha.json")
public class GachaDef extends BaseDef {
    private int Id;
    private int StorageId;
    private int GachaType;
    
    private int GuaranteeTimes;
    private int GuaranteeTid;
    private int GuaranteeQty;
    
    // Packages
    private int ATypePkg;
    private int BTypePkg;
    private int CTypePkg;
    
    private int ATypeUpPkg;
    private int BTypeUpPkg;
    private int CTypeUpPkg;
    
    private int BGuaranteePkg;
    
    private transient WeightedList<GachaPackage> packageA;
    private transient WeightedList<GachaPackage> packageB;
    private transient WeightedList<GachaPackage> packageC;
    
    @Override
    public int getId() {
        return Id;
    }
    
    public GachaStorageDef getStorageData() {
        return GameData.getGachaStorageDataTable().get(this.getStorageId());
    }
    
    @Override
    public void onLoad() {
        // Get storage
        var storage = this.getStorageData();
        
        // Package A
        this.packageA = new WeightedList<GachaPackage>();
        
        if (this.ATypePkg > 0) {
            packageA.add(
                    10000 - storage.getATypeUpProb(), 
                    new GachaPackage(GachaPackageType.A, this.ATypePkg)
            );
        } if (this.ATypeUpPkg > 0) {
            packageA.add(
                    storage.getATypeUpProb(),
                    new GachaPackage(GachaPackageType.A_UP, this.ATypeUpPkg)
            );
        }
        
        // Package B
        this.packageB = new WeightedList<GachaPackage>();
        
        if (this.BTypePkg > 0) {
            packageB.add(
                    10000 - storage.getBTypeUpProb(), 
                    new GachaPackage(GachaPackageType.B, this.BTypePkg)
            );
        } else if (this.BGuaranteePkg > 0) {
            packageB.add(
                    10000 - storage.getBTypeUpProb(),
                    new GachaPackage(GachaPackageType.B, this.BGuaranteePkg)
            );
        } if (this.BTypeUpPkg > 0) {
            packageB.add(
                    storage.getBTypeUpProb(),
                    new GachaPackage(GachaPackageType.B_UP, this.BTypeUpPkg)
            );
        }
        
        // Package C
        this.packageC = new WeightedList<GachaPackage>();
        
        if (this.CTypePkg > 0) {
            packageC.add(
                    10000,
                    new GachaPackage(GachaPackageType.C, this.CTypePkg)
            );
        }
    }
    
    @Getter
    public static class GachaPackage {
        private GachaPackageType type;
        private int id;
        
        public GachaPackage(GachaPackageType type, int id) {
            this.type = type;
            this.id = id;
        }
    }
    
    public enum GachaPackageType {
        A,
        A_UP,
        B,
        B_UP,
        C;
    }
}
