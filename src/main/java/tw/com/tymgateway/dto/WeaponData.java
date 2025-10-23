package tw.com.tymgateway.dto;

import java.util.List;

/**
 * Gateway專用的武器數據DTO
 * 用於HTTP API響應，不依賴backend的generated classes
 */
public class WeaponData {
    // 基本信息
    private String name;
    private String owner;
    private String attributes;
    private Integer baseDamage;
    private Integer bonusDamage;

    // 數組屬性
    private List<String> bonusAttributes;
    private List<String> stateAttributes;

    // 元數據
    private String createdAt;
    private String updatedAt;
    private Long version;

    // 構造函數
    public WeaponData() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }

    public Integer getBaseDamage() { return baseDamage; }
    public void setBaseDamage(Integer baseDamage) { this.baseDamage = baseDamage; }

    public Integer getBonusDamage() { return bonusDamage; }
    public void setBonusDamage(Integer bonusDamage) { this.bonusDamage = bonusDamage; }

    public List<String> getBonusAttributes() { return bonusAttributes; }
    public void setBonusAttributes(List<String> bonusAttributes) { this.bonusAttributes = bonusAttributes; }

    public List<String> getStateAttributes() { return stateAttributes; }
    public void setStateAttributes(List<String> stateAttributes) { this.stateAttributes = stateAttributes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
