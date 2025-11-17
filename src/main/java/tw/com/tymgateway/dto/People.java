package tw.com.tymgateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private String name;
    private String nameOriginal;
    private Integer physicPower;
    private Integer magicPower;
    private Integer utilityPower;
    private String attributes;
    private String faction;
    private String armyName;
    private String baseAttributes;
    private String bonusAttributes;
    private String stateAttributes;

    // Add other fields as needed
}
