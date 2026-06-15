package pk.ls.pasir.sosnowski_lukasz.info;

public class InfoResponse {
    private final String appName;
    private final String version;
    private final String message;

    public InfoResponse(String appName, String version, String message) {
        this.appName = appName;
        this.version = version;
        this.message = message;
    }

    public String getAppName() {
        return appName;
    }

    public String getVersion() {
        return version;
    }

    public String getMessage() {
        return message;
    }
}