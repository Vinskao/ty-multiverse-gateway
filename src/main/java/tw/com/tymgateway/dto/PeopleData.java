package tw.com.tymgateway.dto;

/**
 * Gateway專用的人物數據DTO
 * 用於HTTP API響應，不依賴backend的generated classes
 */
public class PeopleData {
    // 基本信息
    private String name;
    private String nameOriginal;
    private String codeName;

    // 力量屬性
    private Integer physicPower;
    private Integer magicPower;
    private Integer utilityPower;

    // 基本信息
    private String dob;
    private String race;
    private String attributes;
    private String gender;
    private String profession;
    private Integer age;

    // 身體特徵
    private String assSize;
    private String boobsSize;
    private Integer heightCm;
    private Integer weightKg;

    // 職業和技能
    private String combat;
    private String job;
    private String physics;

    // 個性特徵
    private String knownAs;
    private String personality;
    private String interest;
    private String likes;
    private String dislikes;
    private String favoriteFoods;

    // 關係和組織
    private String concubine;
    private String faction;
    private Integer armyId;
    private String armyName;
    private Integer deptId;
    private String deptName;
    private Integer originArmyId;
    private String originArmyName;

    // 其他信息
    private Boolean gaveBirth;
    private String email;
    private String proxy;

    // JSON屬性
    private String baseAttributes;
    private String bonusAttributes;
    private String stateAttributes;

    // 元數據
    private String createdAt;
    private String updatedAt;
    private Long version;

    // 構造函數
    public PeopleData() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameOriginal() { return nameOriginal; }
    public void setNameOriginal(String nameOriginal) { this.nameOriginal = nameOriginal; }

    public String getCodeName() { return codeName; }
    public void setCodeName(String codeName) { this.codeName = codeName; }

    public Integer getPhysicPower() { return physicPower; }
    public void setPhysicPower(Integer physicPower) { this.physicPower = physicPower; }

    public Integer getMagicPower() { return magicPower; }
    public void setMagicPower(Integer magicPower) { this.magicPower = magicPower; }

    public Integer getUtilityPower() { return utilityPower; }
    public void setUtilityPower(Integer utilityPower) { this.utilityPower = utilityPower; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getRace() { return race; }
    public void setRace(String race) { this.race = race; }

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getAssSize() { return assSize; }
    public void setAssSize(String assSize) { this.assSize = assSize; }

    public String getBoobsSize() { return boobsSize; }
    public void setBoobsSize(String boobsSize) { this.boobsSize = boobsSize; }

    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }

    public Integer getWeightKg() { return weightKg; }
    public void setWeightKg(Integer weightKg) { this.weightKg = weightKg; }

    public String getCombat() { return combat; }
    public void setCombat(String combat) { this.combat = combat; }

    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }

    public String getPhysics() { return physics; }
    public void setPhysics(String physics) { this.physics = physics; }

    public String getKnownAs() { return knownAs; }
    public void setKnownAs(String knownAs) { this.knownAs = knownAs; }

    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }

    public String getInterest() { return interest; }
    public void setInterest(String interest) { this.interest = interest; }

    public String getLikes() { return likes; }
    public void setLikes(String likes) { this.likes = likes; }

    public String getDislikes() { return dislikes; }
    public void setDislikes(String dislikes) { this.dislikes = dislikes; }

    public String getFavoriteFoods() { return favoriteFoods; }
    public void setFavoriteFoods(String favoriteFoods) { this.favoriteFoods = favoriteFoods; }

    public String getConcubine() { return concubine; }
    public void setConcubine(String concubine) { this.concubine = concubine; }

    public String getFaction() { return faction; }
    public void setFaction(String faction) { this.faction = faction; }

    public Integer getArmyId() { return armyId; }
    public void setArmyId(Integer armyId) { this.armyId = armyId; }

    public String getArmyName() { return armyName; }
    public void setArmyName(String armyName) { this.armyName = armyName; }

    public Integer getDeptId() { return deptId; }
    public void setDeptId(Integer deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public Integer getOriginArmyId() { return originArmyId; }
    public void setOriginArmyId(Integer originArmyId) { this.originArmyId = originArmyId; }

    public String getOriginArmyName() { return originArmyName; }
    public void setOriginArmyName(String originArmyName) { this.originArmyName = originArmyName; }

    public Boolean getGaveBirth() { return gaveBirth; }
    public void setGaveBirth(Boolean gaveBirth) { this.gaveBirth = gaveBirth; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getProxy() { return proxy; }
    public void setProxy(String proxy) { this.proxy = proxy; }

    public String getBaseAttributes() { return baseAttributes; }
    public void setBaseAttributes(String baseAttributes) { this.baseAttributes = baseAttributes; }

    public String getBonusAttributes() { return bonusAttributes; }
    public void setBonusAttributes(String bonusAttributes) { this.bonusAttributes = bonusAttributes; }

    public String getStateAttributes() { return stateAttributes; }
    public void setStateAttributes(String stateAttributes) { this.stateAttributes = stateAttributes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

