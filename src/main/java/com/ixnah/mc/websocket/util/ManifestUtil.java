package com.ixnah.mc.websocket.util;

import java.io.IOException;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2021/7/19 11:07
 */
public class ManifestUtil {

    private ManifestUtil() {
    }

    public static Manifest getManifest(Class<?> class_) {
        try (JarInputStream jis = new JarInputStream(class_.getProtectionDomain().getCodeSource().getLocation().openStream())) {
            return jis.getManifest();
        } catch (IOException e) {
            e.printStackTrace();
            return new Manifest();
        }
    }

    public static String getValue(Class<?> class_, String key) {
        return getManifest(class_).getMainAttributes().getValue(key);
    }
}
