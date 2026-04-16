package emu.nebula.game.jointdrill;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.player.Player;
import emu.nebula.game.tower.StarTowerBuild;
import emu.nebula.game.character.GameCharacter;
import emu.nebula.proto.JointDrillRank.JointDrillRankChar;
import emu.nebula.proto.JointDrillRank.JointDrillRankData;
import emu.nebula.proto.JointDrillRank.JointDrillRankTeam;
import emu.nebula.proto.Public.HonorInfo;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity(value = "joint_drill_rank", useDiscriminator = false)
public class JointDrillRankEntry implements GameDatabaseObject {
    @Id
    private int playerUid;
    
    private String name;
    private int level;
    private int headIcon;
    private int titlePrefix;
    private int titleSuffix;
    private int[] honor;
    private int[] honorAff;
    private int score;
    private IntSet claimedRewards;
    
    private int activityId;
    private List<JointDrillTeamEntry> teams;
    
    @Setter
    private transient int rank;
    
    @Deprecated // Morphia only
    public JointDrillRankEntry() {
        this.rank = 999;
    }
    
    public JointDrillRankEntry(Player player, int activityId) {
        this.playerUid = player.getUid();
        this.activityId = activityId;
        this.teams = new ArrayList<>();
    }
    
    public IntSet getClaimedRewards() {
        if (this.claimedRewards == null) {
            this.claimedRewards = new IntOpenHashSet();
        }
        
        return this.claimedRewards;
    }
    
    public void update(Player player) {
        this.name = player.getName();
        this.level = player.getLevel();
        this.headIcon = player.getHeadIcon();
        this.titlePrefix = player.getTitlePrefix();
        this.titleSuffix = player.getTitleSuffix();
        this.honor = player.getHonor();
        this.honorAff = new int[this.honor.length];
        
        for (int i = 0; i < this.honor.length; i++) {
            var honor = GameData.getHonorDataTable().get(this.honor[i]);
            if (honor == null || !honor.isCharacterHonor()) {
                continue;
            }
            
            var character = player.getCharacters().getCharacterById(honor.getCharacterId());
            if (character != null) {
                this.honorAff[i] = character.getAffinityLevel();
            }
        }
    }
    
    public void settle(Player player, List<JointDrillBuild> builds, int activityId, int score) {
        // Update player data
        this.update(player);
        
        // Reset score entry if activity id doesn't match
        if (this.activityId != activityId) {
            this.activityId = activityId;
            this.reset();
        }
        
        // Add teams
        for (var build : builds) {
            var team = new JointDrillTeamEntry(player, build.getBuild(), build.getTime(), build.getDamage());
            this.getTeams().add(team);
        }
        
        // Calculate score
        this.score = score;
    }
    
    private void reset() {
        this.score = 0;
        this.getClaimedRewards().clear();
        this.getTeams().clear();
    }
    
    // Proto
    
    public JointDrillRankData toProto() {
        var proto = JointDrillRankData.newInstance()
                .setId(this.getPlayerUid())
                .setNickName(this.getName())
                .setWorldClass(this.getLevel())
                .setHeadIcon(this.getHeadIcon())
                .setScore(this.getScore())
                .setTitlePrefix(this.getTitlePrefix())
                .setTitleSuffix(this.getTitleSuffix())
                .setRank(this.getRank());
        
        for (int i = 0; i < this.getHonor().length; i++) {
            int honorId = this.getHonor()[i];
            int affinityLevel = 0;
            
            if (this.getHonorAff() != null && i < this.getHonorAff().length) {
                affinityLevel = this.getHonorAff()[i];
            }
            
            var info = HonorInfo.newInstance()
                    .setId(honorId)
                    .setAffinityLV(affinityLevel);
            
            proto.addHonors(info);
        }
        
        for (var team : this.getTeams()) {
            proto.addTeams(team.toProto());
        }
        
        return proto;
    }
    
    // Extra classes
    
    @Getter
    @Entity(useDiscriminator = false)
    public static class JointDrillTeamEntry {
        private int buildId;
        private int buildScore;
        private int damage;
        private int time;
        private List<JointDrillCharEntry> characters;
        
        @Deprecated // Morphia only
        public JointDrillTeamEntry() {
            
        }
        
        public JointDrillTeamEntry(Player player, StarTowerBuild build, int time, int damage) {
            this.buildId = build.getUid();
            this.buildScore = build.getScore();
            this.time = time;
            this.damage = damage;
            this.characters = new ArrayList<>();
            
            for (var charId : build.getCharIds()) {
                var character = player.getCharacters().getCharacterById(charId);
                if (character != null) {
                    this.getCharacters().add(new JointDrillCharEntry(character));
                }
            }
        }
        
        public JointDrillRankTeam toProto() {
            var proto = JointDrillRankTeam.newInstance()
                    .setBuildScore(this.getBuildScore())
                    .setDamage(this.getDamage())
                    .setTime(this.getTime());
            
            for (var c : this.getCharacters()) {
                proto.addChars(c.toProto());
            }
            
            return proto;
        }
    }
    
    @Getter
    @Entity(useDiscriminator = false)
    public static class JointDrillCharEntry {
        private int id;
        private int level;
        
        @Deprecated // Morphia only
        public JointDrillCharEntry() {
            
        }
        
        public JointDrillCharEntry(GameCharacter character) {
            this.id = character.getCharId();
            this.level = character.getLevel();
        }
        
        public JointDrillRankChar toProto() {
            var proto = JointDrillRankChar.newInstance()
                    .setId(this.getId())
                    .setLevel(this.getLevel());
            
            return proto;
        }
    }
}
