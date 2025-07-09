package wiki.zhr.zouaiagent.utils;

import wiki.zhr.zouaiagent.reader.FeiShuConfig;

public class FeiShuUtils {
    private FeiShuUtils() {
        // Private constructor to prevent instantiation
    }

    public static String getFeiShuAppId() {
        return FeiShuConfig.FEISHU_APP_ID;
    }

    public static String getFeiShuAppSecret() {
        return FeiShuConfig.FEISHU_APP_SECRET;
    }

    public static String getFeiShuUserToken(String feiShuAppId) {
        // 获取UserToken前需要获取授权码
        String accessCodeToken = getaccessToken(feiShuAppId);

        String code = "2IDpx88dfzC1AC4wEwI85fx2b6b188C6";
        return code;
    }

    private static String getaccessToken(String feiShuAppId) {
        return "";
    }

    public static String getFeiShuDocumentId() {
        return FeiShuConfig.FEISHU_DOCUMENT_ID;
    }
}
