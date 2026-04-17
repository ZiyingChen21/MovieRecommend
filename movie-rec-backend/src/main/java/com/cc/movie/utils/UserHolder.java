package com.cc.movie.utils;

/**
 * 利用 ThreadLocal 保存当前登录用户的 UID
 */
public class UserHolder {
    private static final ThreadLocal<Integer> UID_THREAD_LOCAL = new ThreadLocal<>();

    public static void setUid(Integer uid) {
        UID_THREAD_LOCAL.set(uid);
    }
    public static Integer getUid() {
        return UID_THREAD_LOCAL.get();
    }

    public static void remove() {
        UID_THREAD_LOCAL.remove();
    }
}
