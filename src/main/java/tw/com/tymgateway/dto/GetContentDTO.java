package tw.com.tymgateway.dto;

/**
 * 獲取內容數據傳輸對象
 *
 * <p>用於 Gateway 層的內容獲取請求</p>
 *
 * @author TY Team
 * @version 1.0
 */
public class GetContentDTO {

    private String editor;

    public GetContentDTO() {
    }

    public GetContentDTO(String editor) {
        this.editor = editor;
    }

    public String getEditor() {
        return editor;
    }

    public void setEditor(String editor) {
        this.editor = editor;
    }
}
