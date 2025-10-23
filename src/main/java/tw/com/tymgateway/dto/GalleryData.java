package tw.com.tymgateway.dto;

/**
 * Gateway專用的圖片數據DTO
 * 用於HTTP API響應，不依賴backend的generated classes
 */
public class GalleryData {
    private Integer id;
    private String imageBase64;
    private String uploadTime;
    private Long version;

    // 構造函數
    public GalleryData() {}

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getUploadTime() { return uploadTime; }
    public void setUploadTime(String uploadTime) { this.uploadTime = uploadTime; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
