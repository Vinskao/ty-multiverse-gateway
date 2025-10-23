package tw.com.tymgateway.dto;

/**
 * 編輯內容值對象
 *
 * <p>用於 Gateway 層的內容編輯數據傳輸</p>
 *
 * @author TY Team
 * @version 1.0
 */
public class EditContentVO {

    private String editor;
    private String content;

    public EditContentVO() {
    }

    public EditContentVO(String editor, String content) {
        this.editor = editor;
        this.content = content;
    }

    public String getEditor() {
        return editor;
    }

    public void setEditor(String editor) {
        this.editor = editor;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
