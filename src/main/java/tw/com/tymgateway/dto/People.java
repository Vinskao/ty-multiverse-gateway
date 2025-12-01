package tw.com.tymgateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * People DTO for Gateway
 *
 * <p>Simplified version for API requests/responses</p>
 *
 * @author TY Gateway Team
 * @version 1.0
 * @since 2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class People {

    private String nameOriginal;
    private String codeName;
    private String name;
    private Integer physicPower;
    private Integer magicPower;
    private Integer utilityPower;
    private String dob;
    private String race;
    private String attributes;
    private String gender;
    private String assSize;
    private String boobsSize;
    private Integer heightCm;
    private Integer weightKg;
    private String profession;
    private String combat;
    private String favoriteFoods;
    private String job;
    private String physics;
    private String knownAs;
    private String personality;
    private String interest;
    private String likes;
    private String dislikes;
    private String concubine;
    private String faction;
    private Integer armyId;
    private String armyName;
    private Integer deptId;
    private String deptName;
    private Integer originArmyId;
    private String originArmyName;
    private Boolean gaveBirth;
    private String email;
    private Integer age;
    private String proxy;
    private String baseAttributes;
    private String bonusAttributes;
    private String stateAttributes;
    private String embedding;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
