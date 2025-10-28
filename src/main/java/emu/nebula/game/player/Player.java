package emu.nebula.game.player;

import java.util.HashSet;
import java.util.Set;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;

import emu.nebula.GameConstants;
import emu.nebula.Nebula;
import emu.nebula.data.GameData;
import emu.nebula.database.GameDatabaseObject;
import emu.nebula.game.account.Account;
import emu.nebula.game.character.CharacterStorage;
import emu.nebula.game.formation.FormationManager;
import emu.nebula.game.inventory.Inventory;
import emu.nebula.game.mail.Mailbox;
import emu.nebula.game.story.StoryManager;
import emu.nebula.game.tower.StarTowerManager;
import emu.nebula.net.GameSession;
import emu.nebula.proto.PlayerData.PlayerInfo;
import emu.nebula.proto.Public.NewbieInfo;
import emu.nebula.proto.Public.QuestType;
import emu.nebula.proto.Public.Story;
import emu.nebula.proto.Public.WorldClass;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import lombok.Getter;

@Getter
@Entity(value = "players", useDiscriminator = false)
public class Player implements GameDatabaseObject {
    @Id private int uid;
    @Indexed private String accountUid;
    
    private transient Account account;
    private transient Set<GameSession> sessions;
    
    // Details
    private String name;
    private boolean gender;
    private int headIcon;
    private int skinId;
    private int titlePrefix;
    private int titleSuffix;
    private int level;
    private int exp;
    
    private int energy;
   
    private IntSet boards;
    private IntSet titles;
    
    private long createTime;
    
    // Managers
    private final transient CharacterStorage characters;
    private final transient Inventory inventory;
    
    // Referenced data
    private transient FormationManager formations;
    private transient Mailbox mailbox;
    private transient StarTowerManager starTowerManager;
    private transient StoryManager storyManager;
    
    @Deprecated // Morphia only
    public Player() {
        this.sessions = new HashSet<>();
        this.characters = new CharacterStorage(this);
        this.inventory = new Inventory(this);
    }
    
    public Player(Account account, String name, boolean gender) {
        this();
        
        // Set uid first
        if (account.getReservedPlayerUid() > 0) {
            this.uid = account.getReservedPlayerUid();
        } else {
            this.uid = Nebula.getGameDatabase().getNextObjectId(Player.class);
        }
        
        // Set basic info
        this.accountUid = account.getUid();
        this.name = name;
        this.gender = gender;
        this.headIcon = 101;
        this.skinId = 10301;
        this.titlePrefix = 1;
        this.titleSuffix = 2;
        this.level = 1;
        this.boards = new IntOpenHashSet();
        this.titles = new IntOpenHashSet();
        this.createTime = Nebula.getCurrentTime();
        
        // Add starter characters
        this.getCharacters().addCharacter(103);
        this.getCharacters().addCharacter(112);
        this.getCharacters().addCharacter(113);
        
        // Add starter discs
        this.getCharacters().addDisc(211001);
        this.getCharacters().addDisc(211005);
        this.getCharacters().addDisc(211007);
        this.getCharacters().addDisc(211008);
        
        // Add titles
        this.getTitles().add(this.getTitlePrefix());
        this.getTitles().add(this.getTitleSuffix());
        
        // Add board ids
        this.getBoards().add(410301);
    }
    
    public Account getAccount() {
        if (this.account == null) {
            this.account = Nebula.getAccountDatabase().getObjectByField(Account.class, "_id", this.getAccountUid());
        }
        
        return this.account;
    }
    
    public void addSession(GameSession session) {
        synchronized (this.sessions) {
            this.sessions.add(session);
        }
    }
    
    public void removeSession(GameSession session) {
        synchronized (this.sessions) {
            this.sessions.remove(session);
        }
    }
    
    public boolean hasSessions() {
        synchronized (this.sessions) {
            return !this.sessions.isEmpty();
        }
    }
    
    public boolean getGender() {
        return this.gender;
    }

    public boolean editName(String newName) {
        // Sanity check
        if (newName == null || newName.isEmpty() || newName.equals(this.getName())) {
            return false;
        }
        
        // Limit name length
        if (newName.length() > 20) {
            newName = newName.substring(0, 19);
        }
        
        // Set name
        this.name = newName;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "name", this.getName());
        
        // Success
        return true;
    }
    
    public void editGender() {
        // Set name
        this.gender = !this.gender;
        
        // Update in database
        Nebula.getGameDatabase().update(this, this.getUid(), "gender", this.getGender());
    }
    
    public void setNewbieInfo(int groupId, int stepId) {
        // TODO 
    }
    
    public int getMaxExp() {
        var data = GameData.getWorldClassDataTable().get(this.level + 1);
        return data != null ? data.getExp() : 0;
    }
    
    public PlayerChangeInfo addExp(int amount, PlayerChangeInfo changes) {
        // Check if changes is null
        if (changes == null) {
            changes = new PlayerChangeInfo();
        }
        
        // Sanity
        if (amount <= 0) {
            return changes;
        }
        
        // Setup
        int oldLevel = this.getLevel();
        int oldExp = this.getExp();
        int expRequired = this.getMaxExp();

        // Add exp
        this.exp += amount;

        // Check for level ups
        while (this.exp >= expRequired && expRequired > 0) {
            this.level += 1;
            this.exp -= expRequired;
            
            expRequired = this.getMaxExp();
        }
        
        // Save to database
        Nebula.getGameDatabase().update(
                this, 
                this.getUid(), 
                "level", 
                this.getLevel(), 
                "exp", 
                this.getExp()
        );
        
        // Calculate changes
        var proto = WorldClass.newInstance()
                .setAddClass(this.getLevel() - oldLevel)
                .setExpChange(this.getExp() - oldExp);
        
        changes.add(proto);
        
        return changes;
    }
    
    public void sendMessage(String string) {
        // Empty
    }
    
    // Login
    
    public void onLoad() {
        // Load from database
        this.getCharacters().loadFromDatabase();
        this.getInventory().loadFromDatabase();
        
        // Load referenced classes
        this.formations = Nebula.getGameDatabase().getObjectByField(FormationManager.class, "_id", this.getUid());
        if (this.formations == null) {
            this.formations = new FormationManager(this);
        } else {
            this.formations.setPlayer(this);
        }
        
        this.mailbox = Nebula.getGameDatabase().getObjectByField(Mailbox.class, "_id", this.getUid());
        if (this.mailbox == null) {
            this.mailbox = new Mailbox(this);
        }
        
        this.starTowerManager = Nebula.getGameDatabase().getObjectByField(StarTowerManager.class, "_id", this.getUid());
        if (this.starTowerManager == null) {
            this.starTowerManager = new StarTowerManager(this);
        } else {
            this.starTowerManager.setPlayer(this);
        }
        
        this.storyManager = Nebula.getGameDatabase().getObjectByField(StoryManager.class, "_id", this.getUid());
        if (this.storyManager == null) {
            this.storyManager = new StoryManager(this);
        } else {
            this.storyManager.setPlayer(this);
        }
    }
    
    // Proto

    public PlayerInfo toProto() {
        PlayerInfo proto = PlayerInfo.newInstance();
        
        var acc = proto.getMutableAcc()
            .setNickName(this.getName())
            .setGender(this.getGender())
            .setId(this.getUid())
            .setHeadIcon(this.getHeadIcon())
            .setSkinId(this.getSkinId())
            .setTitlePrefix(this.getTitlePrefix())
            .setTitleSuffix(this.getTitleSuffix())
            .setCreateTime(this.getCreateTime());
        
        proto.getMutableWorldClass()
            .setStage(3)
            .setCur(this.getLevel())
            .setLastExp(this.getExp());
        
        proto.getMutableEnergy()
            .getMutableEnergy()
                .setUpdateTime(Nebula.getCurrentTime())
                .setNextDuration(60)
                .setPrimary(240)
                .setIsPrimary(true);
        
        // Add characters/discs/res/items
        for (var character : getCharacters().getCharacterCollection()) {
            proto.addChars(character.toProto());
        }
        
        for (var disc : getCharacters().getDiscCollection()) {
            proto.addDiscs(disc.toProto());
        }
        
        for (var item : getInventory().getItems().values()) {
            proto.addItems(item.toProto());
        }
        
        for (var res : getInventory().getResources().values()) {
            proto.addRes(res.toProto());
        }
        
        // Formations
        for (var f : this.getFormations().getFormations().values()) {
            proto.getMutableFormation().addInfo(f.toProto());
        }
        
        // Set state
        var state = proto.getMutableState()
            .setStorySet(true);
        
        state.getMutableMail();
        state.getMutableBattlePass();
        state.getMutableWorldClassReward();
        state.getMutableFriendEnergy();
        state.getMutableMallPackage();
        state.getMutableAchievement();
        state.getMutableTravelerDuelQuest()
            .setType(QuestType.TravelerDuel);
        state.getMutableTravelerDuelChallengeQuest()
            .setType(QuestType.TravelerDuelChallenge);
        state.getMutableStarTower();
        state.getMutableStarTowerBook();
        state.getMutableScoreBoss();
        state.getMutableCharAffinityRewards();
        
        // Force complete tutorials
        for (var guide : GameData.getGuideGroupDataTable()) {
            var info = NewbieInfo.newInstance()
                    .setGroupId(guide.getId())
                    .setStepId(-1);
            
            acc.addNewbies(info);
        }
        
        acc.addNewbies(NewbieInfo.newInstance().setGroupId(GameConstants.INTRO_GUIDE_ID).setStepId(-1));
        
        // Story
        var story = proto.getMutableStory();
        
        for (int storyId : this.getStoryManager().getCompletedStories()) {
            var storyProto = Story.newInstance()
                    .setIdx(storyId);
            
            story.addStories(storyProto);
        }

        //
        proto.addBoard(410301);
        proto.setServerTs(Nebula.getCurrentTime());
        
        // Extra
        proto.setAchievements(new byte[64]);
        
        proto.getMutableVampireSurvivorRecord()
            .getMutableSeason();
        
        proto.getMutableQuests();
        proto.getMutableAgent();
        proto.getMutablePhone();
        
        return proto;
    }
}
