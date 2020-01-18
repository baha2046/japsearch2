package org.nagoya.preferences;

import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.io.LocalVFS;
import org.nagoya.io.Setting;

import java.util.Properties;

public class AppSetting {
    private static final String xmlFileName = "setting.xml";
    private static AppSetting INSTANCE = null;

    public static synchronized AppSetting getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AppSetting();//Setting.readSetting(SettingsV2.class, fileName);
        }
        return INSTANCE;
    }

    private final Properties properties;

    private AppSetting() {
        this.properties = LocalVFS.resolveFile(xmlFileName)
                .flatMap(Setting::loadPropertiesOption)
                .getOrElse(new Properties());
    }

    public void saveSetting() {
        LocalVFS.resolveFile(xmlFileName)
                .flatMap(fo -> Setting.savePropertiesOption(fo, this.properties))
                .onEmpty(() -> GUICommon.debugMessage("Save properties failed"));
    }

    public Boolean getBooleanValue(String key, @NotNull Boolean defaultValue) {
        return Boolean.parseBoolean(this.getStringValue(key, defaultValue.toString()));
    }

    public void setBooleanValue(String key, @NotNull Boolean value) {
        this.setStringValue(key, value.toString());
    }

    public String getStringValue(String key, String defaultValue) {
        return this.properties.getProperty(key, defaultValue);
    }

    public void setStringValue(String key, String value) {
        this.properties.setProperty(key, value);
    }

    public Integer getIntValue(String key, @NotNull Integer defaultValue) {
        return Integer.parseInt(this.getStringValue(key, defaultValue.toString()));
    }

    public void setIntValue(String key, @NotNull Integer value) {
        this.setStringValue(key, value.toString());
    }
}
