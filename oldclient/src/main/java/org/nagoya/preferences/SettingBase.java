package org.nagoya.preferences;

public abstract class SettingBase {
    protected final AppSetting settings;

    protected SettingBase() {
        this.settings = AppSetting.getInstance();
    }

    public void saveSetting() {
        this.settings.saveSetting();
    }
}
