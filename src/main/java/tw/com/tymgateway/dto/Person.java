package tw.com.tymgateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人物 DTO - 用於 Gateway 與前端/後端通信
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    private String name;
    private Integer age;
    private Integer level;
    private String attributes;
}
