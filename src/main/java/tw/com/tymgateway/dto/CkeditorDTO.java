package tw.com.tymgateway.dto;

/**
 * CKEditor 數據傳輸對象
 *
 * <p>用於 Gateway 層與外部系統交換數據</p>
 *
 * @author TY Team
 * @version 1.0
 */
public class CkeditorDTO {

    private boolean success;
    private String message;
    private String editor;
    private String lastModified;

    public CkeditorDTO() {
    }

    public CkeditorDTO(boolean success, String message, String editor, String lastModified) {
        this.success = success;
        this.message = message;
        this.editor = editor;
        this.lastModified = lastModified;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEditor() {
        return editor;
    }

    public void setEditor(String editor) {
        this.editor = editor;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
}
