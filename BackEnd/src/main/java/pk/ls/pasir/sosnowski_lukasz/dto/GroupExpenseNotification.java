package pk.ls.pasir.sosnowski_lukasz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

@Getter
@AllArgsConstructor
public class GroupExpenseNotification {

    private String type;
    private Long groupId;
    private String groupName;
    private String title;
    private Double amount;
    private Double userShare;
    private String createdByEmail;
    private String message;

    public String toJson() {
        return String.format(
                Locale.US,
                "{\"type\":\"%s\",\"groupId\":%d,\"groupName\":\"%s\",\"title\":\"%s\",\"amount\":%.2f,\"userShare\":%.2f,\"createdByEmail\":\"%s\",\"message\":\"%s\"}",
                escape(type),
                groupId,
                escape(groupName),
                escape(title),
                amount,
                userShare,
                escape(createdByEmail),
                escape(message)
        );
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}