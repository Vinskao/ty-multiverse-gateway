package tw.com.tymgateway.dto;

/**
 * Gateway專用的用戶資訊DTO
 * 用於HTTP API響應，不依賴backend的generated classes
 */
public class UserInfo {
    private String username;
    private String email;
    private String name;
    private String firstName;
    private String lastName;

    public UserInfo() {}

    public UserInfo(String username, String email, String name, String firstName, String lastName) {
        this.username = username;
        this.email = email;
        this.name = name;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}
