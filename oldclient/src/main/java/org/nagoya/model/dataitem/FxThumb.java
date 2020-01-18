package org.nagoya.model.dataitem;

import io.vavr.control.Option;
import io.vavr.control.Try;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.apache.commons.vfs2.FileObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.commons.ExecuteSystem;
import org.nagoya.preferences.CustomOptions;
import org.reactfx.util.TriConsumer;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FxThumb extends MovieDataItem {
    public static final Function<URL, String> DEFAULT_REFER_MODIFY = GUICommon.referrerModifyFunction;
    public static final String DEFAULT_USER_AGENT = "Wget/1.13.4 (linux-gnu)";

    protected static final FxThumb BLANK_THUMB = new FxThumb();
    protected static Scheduler asyncScheduler = ExecuteSystem.get().getNormalScheduler();
    protected static final int RETRY_TIMES = 5;

    private static boolean LOCAL_ASYNC = true;
    private static boolean NET_ASYNC = true;
    public static final CustomOptions OPTIONS = getOptions();

    protected static final long serialVersionUID = -5172283156078860446L;

    protected Option<URL> thumbURL;
    protected Option<Path> localPath;
    protected Option<String> strCookie;
    protected Option<FileObject> localFileObject;

    protected transient Function<URL, String> referrerModifier;
    protected transient Option<Mono<Image>> imageMono;

    protected double widthLimit;
    protected double heightLimit;

    // private Eval<Image> fximage = Eval.later(this::tryLoadImage).mergeMap(i -> i);

    protected String thumbLabel;

    public static void setAsyncScheduler(Scheduler asyncScheduler) {
        FxThumb.asyncScheduler = asyncScheduler;
    }

    public static Scheduler getAsyncScheduler() {
        return asyncScheduler;
    }

    @NotNull
    private static CustomOptions getOptions() {

        CustomOptions customOptions = new CustomOptions("Image Load");

        customOptions.addOption(FxThumb.class.getSimpleName() + "-isLocalAsync", LOCAL_ASYNC,
                (b) -> LOCAL_ASYNC = b, "Local Async : ");

        customOptions.addOption(FxThumb.class.getSimpleName() + "-isNetAsync", NET_ASYNC,
                (b) -> NET_ASYNC = b, "Net Async : ");

        return customOptions;
    }

    @NotNull
    @Contract(" -> new")
    public static FxThumb create() {
        return new FxThumb();
    }

    public static FxThumb empty() {
        return BLANK_THUMB;
    }

    @NotNull
    public static FxThumb of(URL url) {
        FxThumb thumb = create();
        thumb.setThumbURL(url);
        thumb.setThumbLabel(url.toString());
        return thumb;
    }

    public static Option<FxThumb> of(String url) {
        return Try.of(() -> new URL(url) /* MalformedURLException.class*/)
                .map(FxThumb::of)
                .peek(t -> {
                    if (url.startsWith("file:")) {
                        t.setLocalPath(new File(URI.create(url)).toPath());
                    }
                })
                .onFailure(Throwable::printStackTrace)
                .toOption();
    }

    @NotNull
    public static FxThumb of(Path path) {
        FxThumb thumb = create();
        thumb.setLocalPath(path);
        thumb.setThumbURL(UtilCommon.pathToUrl(path));
        thumb.setThumbLabel(path.getFileName().toString());
        return thumb;
    }

    @NotNull
    public static FxThumb of(FileObject fileObject) {
        FxThumb thumb = create();
        thumb.setLocalFileObject(fileObject);
        thumb.setThumbURL(Try.of(fileObject::getURL).getOrNull());
        thumb.setThumbLabel(fileObject.getName().getBaseName());
        return thumb;
    }

    @NotNull
    public static FxThumb of(@NotNull File file) {
        return FxThumb.of(file.toPath());
    }

    public static final WritableImage EMPTY_IMAGE = new WritableImage(10, 10);

    /**
     * Utility function to of the last part of a URL formatted string (the filename) and return it. Usually used in conjunction with
     *
     * @param url
     * @return
     */
    public static String fileNameFromURL(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    /**
     * Crops a JAV DVD jacket image so that only the cover is returned. This usually means the left half of the jacket image is cropped out.
     *
     * @param -        Image you wish to crop
     * @param filename - filename of the image. If you have a URL, you can of this from
     * @return A new BufferedImage object with the back part of the jacket cover cropped out
     */
    //        Rectangle2D viewportRect = new Rectangle2D(img.getWidth()/2, 0, maximumPosterSizeX, img.getHeight());
    public static Rectangle2D getCoverCrop(double width, double height, String filename) {
        //BufferedImage tempImage;
        int croppedWidth = (int) (width / 2.11);

        //Presets

        //SOD (SDMS, SDDE) - crop 3 pixels
        if (filename.contains("SDDE") || filename.contains("SDMS")) {
            croppedWidth = croppedWidth - 3;
        }
        //Natura High - crop 2 pixels
        if (filename.contains("NHDT")) {
            croppedWidth = croppedWidth - 2;
        }
        //HTY - crop 1 pixel
        if (filename.contains("HTV")) {
            croppedWidth = croppedWidth - 1;
        }
        //Prestige (EVO, DAY, ZER, EZD, DOM) crop 1 pixel
        if (filename.contains("EVO") || filename.contains("DAY") || filename.contains("ZER") || filename.contains("EZD") || filename.contains("DOM") && height == 522) {
            croppedWidth = croppedWidth - 1;
        }
        //DOM - overcrop a little
        if (filename.contains("DOM") && height == 488) {
            croppedWidth = croppedWidth + 13;
        }
        //DIM - crop 5 pixels
        if (filename.contains("DIM")) {
            croppedWidth = croppedWidth - 5;
        }
        //DNPD - the front is on the left and a different crop routine will be used below
        //CRZ - crop 5 pixels
        if (filename.contains("CRZ") && height == 541) {
            croppedWidth = croppedWidth - 5;
        }
        //FSET - crop 2 pixels
        if (filename.contains("FSET") && height == 675) {
            croppedWidth = croppedWidth - 2;
        }
        //Moodyz (MIRD dual discs - the original code says to center the overcropping but provides no example so I'm not dooing anything for now)
        //Opera (ORPD) - crop 1 pixel
        if (filename.contains("DIM")) {
            croppedWidth = croppedWidth - 1;
        }
        //Jade (P9) - crop 2 pixels
        if (filename.contains("P9")) {
            croppedWidth = croppedWidth - 2;
        }
        //Rocket (RCT) - Crop 2 Pixels
        if (filename.contains("RCT")) {
            croppedWidth = croppedWidth - 2;
        }
        //SIMG - crop 10 pixels
        if (filename.contains("SIMG") && height == 864) {
            croppedWidth = croppedWidth - 10;
        }
        //SIMG - crop 4 pixels
        if (filename.contains("SIMG") && height == 541) {
            croppedWidth = croppedWidth - 4;
        }
        //SVDVD - crop 2 pixels
        if (filename.contains("SVDVD") && height == 950) {
            croppedWidth = croppedWidth - 4;
        }
        //XV-65 - crop 6 pixels
        if (filename.contains("XV-65") && height == 750) {
            croppedWidth = croppedWidth - 6;
        }
        //800x538 - crop 2 pixels
        if (height == 538 && width == 800) {
            croppedWidth = croppedWidth - 2;
        }
        //800x537 - crop 1 pixel
        if (height == 537 && width == 800) {
            croppedWidth = croppedWidth - 1;
        }
        if (height == 513 && width == 800) {
            croppedWidth = croppedWidth - 14;
        }

	/*
		if (filename.contains("DNPD")) {
			tempImage = originalImage.getSubimage(0, 0, croppedWidth, height);
		} else
			tempImage = originalImage.getSubimage(width - croppedWidth, 0, croppedWidth, height);*/

        return new Rectangle2D(width - croppedWidth, 0, croppedWidth, height);
    }

/*
    @NotNull
    public static Future<Image> loadImageFromURL(@NotNull URL url, double w, double h, String strUserAgent, String strCookie, String strReferer) {
        return Future.fromTry(Try.of(url::openConnection)
                .map(conn -> setupURLConnection(conn, strUserAgent, strCookie, strReferer))
                .flatMap(conn -> tryLoadImageFromURLConnection(conn, w, h))
        );
    }

    public static Future<Image> loadImageFromURLAsync(Executor ex, @NotNull URL url, double w, double h, String strUserAgent, String strCookie, String strReferer) {
        return Try.of(url::openConnection)
                .map(conn -> setupURLConnection(conn, strUserAgent, strCookie, strReferer))
                .map(conn -> loadImageFromURLConnectionAsync(ex, conn, w, h))
                .fold(Future::<Image>failed, i -> i)
                .onFailure((e) -> GUICommon.debugMessage("FxThumb loadImageFromURLAsync >> Cannot get image from : " + url.toString()))
                ;
    }*/

    public static Mono<Image> monoLoadImageFromURLImp(@NotNull URL url, double w, double h, String strUserAgent, String strCookie, String strReferer) {
        return UtilCommon.tryToMono(() -> tryLoadImageFromURL(url, w, h, strUserAgent, strCookie, strReferer))
                .retry(RETRY_TIMES)
                .cache();
    }

    public static Mono<Image> monoLoadImageFromPathImp(Path path, double w, double h) {
        return UtilCommon.tryToMono(() -> tryLoadImageFromPath(path, w, h))
                .retry(RETRY_TIMES)
                .cache();
    }

    public static Mono<Image> monoLoadImageFromFileObjectImp(FileObject fileObject, double w, double h) {
        return UtilCommon.tryToMono(() -> tryLoadImageFromFileObject(fileObject, w, h))
                .retry(RETRY_TIMES)
                .cache();
    }

    @NotNull
    private static Try<Image> tryLoadImageFromURL(@NotNull URL url, double w, double h, String strUserAgent, String strCookie, String strReferer) {
        return Try.of(url::openConnection)
                .map(conn -> setupURLConnection(conn, strUserAgent, strCookie, strReferer))
                .flatMap(conn -> tryLoadImageFromURLConnection(conn, w, h))
                .onFailure((e) -> GUICommon.debugMessage("FxThumb tryLoadImageFromURL >> Fail : " + url.toString()));
    }

    @NotNull
    @Contract("_, _, _, _ -> param1")
    private static URLConnection setupURLConnection(@NotNull URLConnection conn, String strUserAgent, String strCookie, String strReferer) {
        conn.setRequestProperty("User-Agent", strUserAgent);
        conn.setRequestProperty("Referer", strReferer);
        conn.setRequestProperty("Cookie", strCookie);
        return conn;
    }

    @NotNull
    private static Try<Image> tryLoadImageFromURLConnection(@NotNull URLConnection conn, double w, double h) {
        return Try.withResources(conn::getInputStream)
                .of(stream -> new javafx.scene.image.Image(stream, w, h, true, true));
    }

    @NotNull
    private static Try<Image> tryLoadImageFromPath(@NotNull Path path, double w, double h) {
        return Try.withResources(() -> new FileInputStream(path.toFile()))
                .of(stream -> new javafx.scene.image.Image(stream, w, h, true, true))
                .onFailure((e) -> GUICommon.debugMessage("FxThumb tryLoadImageFromPath >> Fail : " + path.getFileName().toString()));
    }

    private static Try<Image> tryLoadImageFromFileObject(@NotNull FileObject fileObject, double w, double h) {
        return Try.withResources(fileObject::getContent)
                .of(fileContent ->
                        Try.withResources(fileContent::getInputStream)
                                .of(stream -> new javafx.scene.image.Image(stream, w, h, true, true)))
                .flatMap(t -> t)
                .onFailure((e) -> GUICommon.debugMessage("FxThumb tryLoadImageFromPath >> Fail : " + fileObject.getPublicURIString()));
    }

   /* @NotNull
    public static Future<Image> loadImageFromPath(@NotNull Path path, double w, double h) {
        return Future.fromTry(tryLoadImageFromPath(path, w, h));
    }

    @NotNull
    public static Future<Image> loadImageFromPathAsync(Executor ex, @NotNull Path path, double w, double h) {
        return futureFromTry(ex, () -> tryLoadImageFromPath(path, w, h));
    }*/


   /* @NotNull
    private static Future<Image> loadImageFromURLConnectionAsync(Executor ex, @NotNull URLConnection conn, double w, double h) {
        return futureFromTry(ex, () -> tryLoadImageFromURLConnection(conn, w, h));
    }*/

    public static Boolean writeImageToFile(Image image, File file) {
        BufferedImage bufImageARGB = SwingFXUtils.fromFXImage(image, null);
        BufferedImage bufImageRGB = new BufferedImage(bufImageARGB.getWidth(), bufImageARGB.getHeight(), BufferedImage.OPAQUE);

        Graphics2D graphics = bufImageRGB.createGraphics();
        graphics.drawImage(bufImageARGB, 0, 0, null);

        return Try.of(() -> ImageIO.write(bufImageRGB, "jpg", file))
                .andFinally(graphics::dispose)
                .onFailure(e -> GUICommon.debugMessage(() -> "FAIL at FxThumb writeImageToFile >> " + e.getMessage()))
                .getOrElse(false);
    }

    public static Boolean writeURLToFile(@NotNull URL url, File file, String strUserAgent, String strCookie, String strReferer) {
        return Try.of(url::openConnection)
                .map(conn -> setupURLConnection(conn, strUserAgent, strCookie, strReferer))
                .flatMap(conn -> tryWriteURLConnectionToFile(conn, file))
                .onFailure((e) -> GUICommon.debugMessage(() -> "FAIL at FxThumb writeImageToFile >>  " + e.getMessage()))
                .getOrElse(false)
                ;
    }

    private static Try<Boolean> tryWriteURLConnectionToFile(@NotNull URLConnection conn, File file) {
        return Try.withResources(conn::getInputStream)
                .of(ImageIO::read)
                .flatMap(pictureLoaded -> Try.of(() -> ImageIO.write(pictureLoaded, "jpg", file)));
    }
    /*
    public static Future<Boolean> writeImageToFileAsync(Executor ex, @NotNull URL url, File file, String strUserAgent, String strCookie, String strReferer) {
        return Try.of(url::openConnection)
                .map(conn -> setupURLConnection(conn, strUserAgent, strCookie, strReferer))
                .map(conn -> futureFromTry(ex, () -> tryWriteImageToFile(conn, file)))
                .fold(Future::<Boolean>failed, i -> i)
                .onFailure((e) -> GUICommon.debugMessage(() -> "FAIL at FxThumb writeImageToFileAsync >>  " + e.getMessage()))
                ;
    }*/

   /* @NotNull
    private static <T> Future<T> futureFromTry(@NotNull Executor ex, Supplier<Try<T>> s) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        ex.execute(() -> s.get().fold(cf::completeExceptionally, cf::complete));
        return Future.fromCompletableFuture(cf);
    }*/

    public Option<FileObject> getLocalFileObject() {
        return this.localFileObject;
    }

    public void setLocalFileObject(FileObject localFileObject) {
        this.localFileObject = Option.of(localFileObject);
    }

    public static boolean fileExistsAtUrl(String URLName) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            // note : you may also need
            //        HttpURLConnection.setInstanceFollowRedirects(false)
            URL url = new URL(URLName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("HEAD");
            con.setRequestProperty("User-Agent", "Wget/1.13.4 (linux-gnu)");
            con.setRequestProperty("Referer", GUICommon.customReferrer(url, null));
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            System.out.println("------------ URL FILE NOT FOUND ------------");
            return false;
        }
    }

    public static void fitImageView(@NotNull ImageView view, Supplier<Image> supplier, Option<Double> maxWidth, Option<Double> maxHeight) {
        fitImageView(view, supplier.get(), maxWidth, maxHeight);
    }

    public static void fitImageView(@NotNull ImageView view, Image img, Option<Double> maxWidth, Option<Double> maxHeight) {
        view.setImage(img);
        view.setPreserveRatio(true);

        if (img != null) {
            maxWidth.peek(mw -> {
                if (img.getWidth() > mw) {
                    view.setFitWidth(mw);
                }
            });
            maxHeight.peek(mh -> {
                if (img.getHeight() > mh) {
                    view.setFitHeight(mh);
                }
            });
        }
    }

    /**************************************************************************
     *
     * Constructors
     *
     **************************************************************************/
    public FxThumb() {
        this.thumbURL = Option.none();
        this.localPath = Option.none();
        this.localFileObject = Option.none();
        this.strCookie = Option.none();
        this.referrerModifier = DEFAULT_REFER_MODIFY;
        this.widthLimit = 0;
        this.heightLimit = 0;
        this.thumbLabel = "";
        this.imageMono = Option.none();
    }

    public void setLocalPath(Path path) {
        this.localPath = Option.of(path);
    }

    public boolean isLocal() {
        return this.localPath.isDefined() || this.localFileObject.isDefined();
    }

    /**
     * Blocking getImage
     */
    public Image getImage() {
        //this.loadImageIfNeed(this.isLocal() ? this::loadPathSync : this::loadURLSync);
        this.prepareImage();
        return this.imageMono.map(Mono::block).getOrElse(() -> null);
    }

    /**
     * non-blocking getImage
     */
    public void getImage(Consumer<Image> imageConsumer) {
        this.prepareImage();
        this.imageMono.peek(mono -> mono.subscribeOn(this.chooseScheduler()).subscribe(imageConsumer))
                .onEmpty(() -> imageConsumer.accept(null));
    }

    public <T> void getImage(BiConsumer<Image, T> imageConsumer, T param) {
        this.prepareImage();
        this.imageMono.peek(mono -> mono.subscribeOn(this.chooseScheduler()).subscribe(i -> imageConsumer.accept(i, param)))
                .onEmpty(() -> imageConsumer.accept(null, param));
    }

    public <T, U> void getImage(TriConsumer<Image, T, U> imageConsumer, T param1, U param2) {
        this.prepareImage();
        this.imageMono.peek(mono -> mono.subscribeOn(this.chooseScheduler()).subscribe(i -> imageConsumer.accept(i, param1, param2)))
                .onEmpty(() -> imageConsumer.accept(null, param1, param2));
    }

    public void getImageForceAsync(Consumer<Image> imageConsumer) {
        //this.loadImageIfNeed(this.isLocal() ? this::loadPathAsync : this::loadURLAsync);
        this.prepareImage();
        this.imageMono.peek(mono -> mono.subscribeOn(asyncScheduler).subscribe(imageConsumer))
                .onEmpty(() -> imageConsumer.accept(null));
    }

    public <T> void getImageForceAsync(BiConsumer<Image, T> imageConsumer, T param) {
        //this.loadImageIfNeed(this.isLocal() ? this::loadPathAsync : this::loadURLAsync);
        this.prepareImage();
        this.imageMono.peek(mono -> mono.subscribeOn(asyncScheduler).subscribe(i -> imageConsumer.accept(i, param)))
                .onEmpty(() -> imageConsumer.accept(null, param));
    }

    public <T, U> void getImageForceAsync(TriConsumer<Image, T, U> imageConsumer, T param1, U param2) {
        //this.loadImageIfNeed(this.isLocal() ? this::loadPathAsync : this::loadURLAsync);
        this.prepareImage();
        this.imageMono.peek(mono -> mono.subscribeOn(asyncScheduler).subscribe(i -> imageConsumer.accept(i, param1, param2)))
                .onEmpty(() -> imageConsumer.accept(null, param1, param2));
    }


    public ImageView getImageView() {
        ImageView imageView = new ImageView();
        this.getImage(imageView::setImage);
        return imageView;
    }

    public void getImageView(Consumer<ImageView> imageViewConsumer) {
        this.getImage(image -> {
            ImageView imageView = new ImageView(image);
            imageViewConsumer.accept(imageView);
        });
    }

    public void fitInImageView(ImageView view, Option<Double> maxWidth, Option<Double> maxHeight) {
        this.getImage((img) -> fitImageView(view, img, maxWidth, maxHeight));
    }

    public void fitInImageView(ImageView view, Double maxWidth, Double maxHeight) {
        this.getImage((img) -> {
            GUICommon.debugMessage("fit in " + view.toString());
            fitImageView(view, img, Option.of(maxWidth), Option.of(maxHeight));
        });
    }

    public void setImage(Image image) {
        //this.imageFuture = Future.successful(image);
        this.imageMono = Option.of(Mono.just(image));
    }

    public void releaseMemory() {
        this.imageMono = Option.none();
    }

    private void prepareImage() {
        if (this.imageMono.isEmpty()) {
            if (this.isLocal()) {
                if (this.localFileObject.isDefined()) {
                    this.imageMono = this.localFileObject.map(fileObject -> monoLoadImageFromFileObjectImp(fileObject, this.widthLimit, this.heightLimit));
                } else {
                    this.imageMono = this.localPath.map(path -> monoLoadImageFromPathImp(path, this.widthLimit, this.heightLimit));
                }
            } else if (this.thumbURL.isDefined()) {
                this.imageMono = Option.of(monoLoadImageFromURLImp(this.getThumbURL(), this.widthLimit, this.heightLimit, DEFAULT_USER_AGENT, this.getCookie(), this.getReferrerString()));
            }
        }
    }

    private Scheduler chooseScheduler() {
        if (this.isLocal()) {
            return LOCAL_ASYNC ? asyncScheduler : Schedulers.immediate();
        }
        return NET_ASYNC ? asyncScheduler : Schedulers.immediate();
    }

    /*
    private void loadImageIfNeed(Supplier<Future<Image>> supplier) {
        if (this.imageFuture == null) {
            this.imageFuture = supplier.get();
        } else if (this.imageFuture.isFailure() && this.retryTimes < RETRY_TIMES) {
            this.retryLoadImage(supplier);
        }
    }

    private void retryLoadImage(@NotNull Supplier<Future<Image>> supplier) {
        this.retryTimes++;
        this.thumbURL = Try.of(() -> new URL(
                this.thumbURL.get().getProtocol(),
                this.thumbURL.get().getHost(),
                this.thumbURL.get().getPort(),
                this.thumbURL.get().getFile())).toOption();
        GUICommon.debugMessage(() -> ">> FxThumb Try to reconnect : " + this.retryTimes);
        this.imageFuture = supplier.get();
    }

    private void sendImage(Consumer<Image> imageConsumer) {
        if (imageConsumer != null) {
            this.imageFuture.peek(imageConsumer)
                    .onFailure((e) -> GUICommon.debugMessage("FAILED at FxThumb >> " + e.getMessage()));
        }
    }

    private <T> void sendImage(BiConsumer<Image, T> imageConsumer, T param) {
        if (imageConsumer != null) {
            this.imageFuture.peek(i -> imageConsumer.accept(i, param))
                    .onFailure((e) -> GUICommon.debugMessage("FAILED at FxThumb >> " + e.getMessage()));
        }
    }

    private <T, U> void sendImage(TriConsumer<Image, T, U> imageConsumer, T param1, U param2) {
        if (imageConsumer != null) {
            this.imageFuture.peek(i -> imageConsumer.accept(i, param1, param2))
                    .onFailure((e) -> GUICommon.debugMessage("FAILED at FxThumb >> " + e.getMessage()));
        }
    }*/

    /*@NotNull
    private Future<Image> loadPathSync() {
        return loadImageFromPath(this.localPath.get(), this.widthLimit, this.heightLimit);
    }

    @NotNull
    private Future<Image> loadPathAsync() {
        return loadImageFromPathAsync(DEFAULT_EXECUTOR, this.localPath.get(), this.widthLimit, this.heightLimit);
    }

    private Future<Image> loadURLSync() {
        return this.thumbURL.isDefined()
                ?
                loadImageFromURL(this.thumbURL.get(), this.widthLimit, this.heightLimit, DEFAULT_USER_AGENT, this.getCookie(), this.getReferrerString())
                :
                Future.successful(null)
                ;
    }

    private Future<Image> loadURLAsync() {
        return this.thumbURL.isDefined()
                ?
                loadImageFromURLAsync(DEFAULT_EXECUTOR, this.thumbURL.get(), this.widthLimit, this.heightLimit, DEFAULT_USER_AGENT, this.getCookie(), this.getReferrerString())
                :
                Future.successful(null)
                ;
    }*/

    /**
     * Write the current stored image to file, if not yet exist then load it first
     */
    public Boolean writeImageToFile(File file) {
        return writeImageToFile(this.getImage(), file);
    }

    public void writeImageToFileAsync(File file, Consumer<Boolean> resultConsumer) {
        Mono.fromCallable(() -> this.writeImageToFile(file))
                .subscribeOn(asyncScheduler)
                .subscribe(resultConsumer);
    }


    /**
     * Download a fresh image from the url and write it to file
     */
    public Boolean writeURLToFile(File file) {
        return writeURLToFile(this.getThumbURL(), file,
                DEFAULT_USER_AGENT, this.getCookie(), this.getReferrerString());
    }

    public void writeURLToFileAsync(File file, Consumer<Boolean> resultConsumer) {
        Mono.fromCallable(() -> this.writeURLToFile(file))
                .subscribeOn(asyncScheduler)
                .subscribe(resultConsumer);
    }

    public URL getThumbURL() {
        return this.thumbURL.getOrNull();
    }

    public boolean isEmpty() {
        return this.thumbURL.isEmpty();
    }

    public boolean notEmpty() {
        return this.thumbURL.isDefined();
    }

    public void setThumbURL(Option<URL> url) {
        this.thumbURL = url;
    }

    public void setThumbURL(URL url) {
        this.thumbURL = Option.of(url);
    }

    public String toXML() {
        return "<thumb>" + this.thumbURL.map(URL::getPath) + "</thumb>";
    }

    @Override
    public String toString() {
        return Objects.toString(this.thumbURL.getOrNull());
    }

    public Path getLocalPath() {
        return this.localPath.getOrNull();
    }

    public String getThumbLabel() {
        return this.thumbLabel;
    }

    public void setWidthLimit(double widthLimit) {
        this.widthLimit = widthLimit;
    }

    public void setHeightLimit(double heightLimit) {
        this.heightLimit = heightLimit;
    }

    public void setThumbLabel(String thumbLabel) {
        this.thumbLabel = thumbLabel;
    }

    public void setCookie(String cookie) {
        this.strCookie = Option.of(cookie);
    }

    public String getCookie() {
        return this.strCookie.getOrElse("");
    }

    public String getReferrerString() {
        return this.referrerModifier.apply(this.getThumbURL());
    }

    public void setReferrerModifier(Function<URL, String> referrerModifier) {
        this.referrerModifier = referrerModifier;
    }

    public Option<URL> urlOption() {
        return this.thumbURL;
    }

    public Option<Path> localPathOption() {
        return this.localPath;
    }

    public Option<String> cookieOption() {
        return this.strCookie;
    }

}
