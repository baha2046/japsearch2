package org.nagoya.preferences;

import io.vavr.collection.HashMap;
import org.nagoya.io.Setting;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Settings {

    private final static Properties programPreferences = new Properties();
    private static final String fileNameOfPreferences = "settings.xml";

    private static Settings INSTANCE;

    protected interface Key {
        String getKey();
    }

    protected static final String fileName = "settingV2.ini";

    public static synchronized Settings getInstance() {
        if (INSTANCE == null) {
            INSTANCE = Setting.readSetting(Settings.class, fileName);
        }
        return INSTANCE;
    }

    protected HashMap<String, Boolean> booleanHashMap;
    protected HashMap<String, String> stringHashMap;
    protected HashMap<String, Integer> intHashMap;

    public void saveSetting() {
        Setting.writeSetting(getInstance(), fileName);
    }

    protected Boolean getBooleanValue(String key, Boolean defaultValue) {
        return this.booleanHashMap.get(key).getOrElse(defaultValue);
    }

    protected void setBooleanValue(String key, Boolean value) {
        this.booleanHashMap = this.booleanHashMap.put(key, value);
    }

    protected String getStringValue(String key, String defaultValue) {
        return this.stringHashMap.get(key).getOrElse(defaultValue);
    }

    protected void setStringValue(String key, String value) {
        this.stringHashMap = this.stringHashMap.put(key, value);
    }

    protected Integer getIntValue(String key, Integer defaultValue) {
        return this.intHashMap.get(key).getOrElse(defaultValue);
    }

    protected void setIntValue(String key, Integer value) {
        this.intHashMap = this.intHashMap.put(key, value);
    }

    /*
    //Initialization that only happens once
    static {
        try (FileInputStream settingsInputStream = new FileInputStream(fileNameOfPreferences)) {
            programPreferences.loadFromXML(settingsInputStream);

        } catch (InvalidPropertiesFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.out.println("Creating settings.xml since it was not found...");
            savePreferences(); //file doesn't exist. this will create the file
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }*/

    protected Settings() {
        this.booleanHashMap = HashMap.empty();
        this.stringHashMap = HashMap.empty();
        this.intHashMap = HashMap.empty();
    }

    public static void savePreferences() {
        try (FileOutputStream settingsOutputStream = new FileOutputStream(fileNameOfPreferences)) {
            programPreferences.storeToXML(settingsOutputStream, "");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected <K extends Key> void setBooleanValue(K preferenceName, Boolean preferenceValue) {
        String key = preferenceName.getKey();
        if (preferenceValue.booleanValue()) {
            programPreferences.setProperty(key, "true");
        } else {
            programPreferences.setProperty(key, "false");
        }

        savePreferences();
    }

    /**
     * @param preferenceName the preference field to set
     * @param defaultValue   the value to return if the preference has not been set
     * @return
     */
    protected <K extends Key> Boolean getBooleanValue(K preferenceName, Boolean defaultValue) {
        String fieldValue = preferenceName.getKey();
        String preferenceValue = programPreferences.getProperty(fieldValue);
        if (preferenceValue == null) {
            return defaultValue;
        }
        if (preferenceValue.equals("true")) {
            return true;
        } else if (preferenceValue.equals("false")) {
            return false;
        }

        return defaultValue;
    }

    protected <K extends Key> void setStringValue(K preferenceName, String preferenceValue) {
        programPreferences.setProperty(preferenceName.getKey(), preferenceValue);
        savePreferences();
    }

    protected static <K extends Key> String getStringValue(K preferenceName, String defaultValue) {
        String fieldValue = preferenceName.getKey();
        String preferenceValue = programPreferences.getProperty(fieldValue);
        if (preferenceValue != null) {
            return preferenceValue;
        }

        return defaultValue;
    }

    protected <K extends Key> void setIntegerValue(K preferenceName, Integer preferenceValue) {
        programPreferences.setProperty(preferenceName.getKey(), preferenceValue.toString());
        savePreferences();
    }

    protected static <K extends Key> Integer getIntegerValue(K preferenceName, Integer defaultValue) {
        String fieldValue = preferenceName.getKey();
        String preferenceValue = programPreferences.getProperty(fieldValue);
        if (preferenceValue == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(preferenceValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }

    }

}