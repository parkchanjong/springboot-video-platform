package com.videoservice.manager.redis.common;

public class CacheNames {

    public static final String SEPARATOR = ":";

    public static final String USER = "user";
    public static final String USER_SESSION = "user-session";
    public static final String USER_COMMENT_BLOCK = USER + SEPARATOR + "comment-block";

    public static final String CHANNEL = "channel";

    public static final String VIDEO = "video";
    public static final String VIDEO_LIST = VIDEO + SEPARATOR + "list";
    public static final String VIDEO_VIEW_COUNT = VIDEO + SEPARATOR + "view-count";
    public static final String VIDEO_VIEW_COUNT_SET = VIDEO + SEPARATOR + "view-count-set";
    public static final String VIDEO_LIKE = VIDEO + SEPARATOR + "like";

    public static final String SUBSCRIBE = "subscribe";
    public static final String SUBSCRIBE_CHANNEL_BY_USER =
            SUBSCRIBE + SEPARATOR + "channel-by-user";
    public static final String SUBSCRIBE_USER = SUBSCRIBE + SEPARATOR + "user";

    public static final String COMMENT = "comment";
    public static final String COMMENT_LIKE = COMMENT + SEPARATOR + "like";
    public static final String COMMENT_PINNED = COMMENT + SEPARATOR + "pinned";

    public static final String COUPON = "coupon";
    public static final String QUANTITY = "quantity";
    public static final String POLICY = "policy";
    public static final String STATE = "state";
    public static final String COUPON_QUANTITY = COUPON + SEPARATOR + QUANTITY;
    public static final String COUPON_POLICY = COUPON + SEPARATOR + POLICY;
    public static final String COUPON_STATE = COUPON + SEPARATOR + STATE;
}
