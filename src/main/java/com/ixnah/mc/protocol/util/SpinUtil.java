package com.ixnah.mc.protocol.util;

import net.minecraftforge.fml.unsafe.UnsafeHacks;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static java.util.Objects.requireNonNull;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2021/7/19 15:21
 */
public class SpinUtil {

    private SpinUtil() {
    }

    public static <T> T spinRequireNonNull(Object object, String fieldName, String message) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            AtomicInteger count = new AtomicInteger(0);
            T result;
            while ((result = UnsafeHacks.getField(field, object)) == null && count.incrementAndGet() < 1000) {
                LockSupport.parkNanos("SpinRequireNonNull", 1000L); // 获取时可能为null, 自旋等待
            }
            return requireNonNull(result, "channel can't be null!");
        } catch (NoSuchFieldException e) {
            throw new NullPointerException(message);
        }
    }
}
