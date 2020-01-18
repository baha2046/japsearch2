package org.nagoya.model.dataitem;

import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.file.Path;

public class MakerData {
    private String makerName;
    private String makerDesc;
    private String makerDMMUrl;
    private URL makerLogoUrl;
    private transient Option<FxThumb> makerLogo;
    private transient int movieCount;
    private String makerJavUrl;
    private Path path;


    @Contract("_, _, _, _, _, _ -> new")
    @NotNull
    public static MakerData of(Path path, String makerName, String makerDesc, String makerDmmUrl, String makerJavUrl, String makerLogoUrl) {
        return new MakerData(path, makerName, makerDesc, makerDmmUrl, makerJavUrl, makerLogoUrl);
    }

   /* public static MakerData of(@NotNull Path path) {
        MakerData makerData = SettingsV2.readSetting(MakerData.class, path.resolve(fileName));
        makerData.setPath(path);
        makerData.setMakerJavUrl("");
        GUICommon.debugMessage(makerData.getMakerName());
        Systems.getDirectorySystem().getMakerDB().putData(path.toString(), makerData);
        return makerData;
    }

    public static boolean isExist(Path path) {
        return Files.isDirectory(path) && Files.exists(path.resolve(fileName));
    }*/

    @Contract(pure = true)
    public MakerData(Path path) {
        this.makerName = "";
        this.makerDesc = "";
        this.makerDMMUrl = "";
        this.makerJavUrl = "";
        this.makerLogoUrl = null;
        this.makerLogo = Option.none();
        this.path = path;
        this.movieCount = 0;
    }

    private MakerData(Path path, String makerName, String makerDesc, String makerDMMUrl, String makerJavUrl, String makerLogo) {
        this.makerName = makerName;
        this.makerDesc = makerDesc;
        this.makerDMMUrl = makerDMMUrl;
        this.makerJavUrl = makerJavUrl;
        this.path = path;
        this.setMakerLogoUrl(makerLogo);
        this.makerLogo = FxThumb.of(makerLogo);
        this.movieCount = 0;
    }

    public String getMakerName() {
        return this.makerName;
    }

    public void setMakerName(String makerName) {
        this.makerName = makerName;
    }

    public String getMakerDesc() {
        return this.makerDesc;
    }

    public void setMakerDesc(String makerDesc) {
        this.makerDesc = makerDesc;
    }

    public String getMakerDMMUrl() {
        return this.makerDMMUrl;
    }

    public void setMakerDMMUrl(String makerDMMUrl) {
        this.makerDMMUrl = makerDMMUrl;
    }

    public String getMakerJavUrl() {
        return this.makerJavUrl;
    }

    public void setMakerJavUrl(String makerJavUrl) {
        this.makerJavUrl = makerJavUrl;
    }

    public Path getPath() {
        return this.path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getMakerLogoUrl() {
        return this.makerLogoUrl == null ? "" : this.makerLogoUrl.toString();
    }

    public void setMakerLogoUrl(String makerLogoUrl) {
        this.makerLogoUrl = Try.of(() -> new URL(makerLogoUrl)).getOrNull();
    }

    public Option<FxThumb> getMakerLogo() {
        if (this.makerLogo == null) {
            this.makerLogo = Option.none();
        }
        if (this.makerLogo.isEmpty()) {
            if (this.makerLogoUrl != null) {
                this.makerLogo = Option.of(FxThumb.of(this.makerLogoUrl));
            }
        }
        return this.makerLogo;
    }

    public void setMakerLogo(Option<FxThumb> makerLogo) {
        this.makerLogo = makerLogo;
    }

    //public void writeToFile() {
    //    SettingsV2.writeSetting(this, this.path.resolve(fileName));
    //}


    public int getMovieCount() {
        return this.movieCount;
    }

    public void setMovieCount(int movieCount) {
        this.movieCount = movieCount;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        MakerData makerData = (MakerData) o;

        if (!this.makerName.equals(makerData.makerName)) {
            return false;
        }
        return this.path.equals(makerData.path);

    }

    @Override
    public int hashCode() {
        int result = this.makerName.hashCode();
        result = 31 * result + this.path.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MakerData{" +
                "makerName='" + this.makerName + '\'' +
                ", makerDesc='" + this.makerDesc + '\'' +
                ", makerDMMUrl='" + this.makerDMMUrl + '\'' +
                ", makerJavUrl='" + this.makerJavUrl + '\'' +
                ", makerLogoUrl=" + this.makerLogoUrl + '\'' +
                ", path=" + this.path +
                '}';
    }
}
