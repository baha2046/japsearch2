package org.nagoya.view.customcell;

import com.jfoenix.controls.JFXListCell;
import io.vavr.control.Option;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.FontWeight;
import org.jetbrains.annotations.NotNull;
import org.nagoya.GUICommon;
import org.nagoya.UtilCommon;
import org.nagoya.model.DirectoryEntry;
import org.nagoya.model.MovieFolder;
import org.nagoya.model.dataitem.FxThumb;
import org.nagoya.model.dataitem.MakerData;
import org.nagoya.view.FXImageViewWrapper;

import java.util.function.BiConsumer;

public class FileListCell extends JFXListCell<DirectoryEntry> {
    private final FXFileListCell normalRectCell;
    private final FXFileListMovieCell movieRectCell;
    private final FXFileListMakerCell makerCell;

    private Option<DirectoryEntry> item;

    public FileListCell() {
        this.normalRectCell = new FXFileListCell();
        this.movieRectCell = new FXFileListMovieCell();
        this.makerCell = new FXFileListMakerCell();
        this.item = Option.none();

        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(Insets.EMPTY);
        this.setPrefWidth(480);
        //system.out.println("------------ Creating Cell ------------");
    }

    public FileListCell(ContextMenu menuFolder, ContextMenu menuFile) {
        this();
        this.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                var dr = this.getDirectoryEntry();
                if (dr.isDefined()) {
                    if (dr.map(DirectoryEntry::isDirectory).get()) {
                        menuFile.hide();
                        menuFolder.show(this, Side.RIGHT, 0, 0);
                    } else {
                        menuFolder.hide();
                        menuFile.show(this, Side.RIGHT, 0, 0);
                    }
                }
            }
        });
    }

    @Override
    public void updateItem(DirectoryEntry item, boolean empty) {
        super.updateItem(item, empty);

        this.setText("");
        this.item = Option.of(item);

        if (!empty) {
            if (!item.getNeedCheck() && item.hasNfo()) {
                this.ShowMovie(item);
            } else {
                if (item.getMakerData().isDefined()) {
                    this.ShowMaker(item.getMakerData().get());
                } else {
                    this.ShowNormal(item);
                }
            }
        } else {
            this.setGraphic(null);
        }
    }

    public Option<DirectoryEntry> getDirectoryEntry() {
        return this.item;
    }

    private void ShowMovie(DirectoryEntry item) {
        this.setPrefHeight(80);
        this.movieRectCell.setModel(item);
        this.setGraphic(this.movieRectCell);
    }

    private void ShowNormal(DirectoryEntry item) {
        this.setPrefHeight(52);
        this.normalRectCell.setModel(item);
        this.normalRectCell.iconView.setImage(item.getFileIcon());
        this.setGraphic(this.normalRectCell);
    }

    private void ShowMaker(MakerData item) {
        this.setPrefHeight(80);
        this.makerCell.setModel(item);
        this.setGraphic(this.makerCell);
    }
}

class FXFileListCell extends AnchorPane {
    @FXML
    ImageView iconView;
    @FXML
    Label nameLabel;
    @FXML
    Label sizeLabel;
    @FXML
    Label typeLabel;
    @FXML
    Rectangle rect;

    private final PseudoClass isFolder, hasMovie;

    private final ObjectProperty<DirectoryEntry> model = new SimpleObjectProperty<>(this, "model") {
        private DirectoryEntry currentModel;

        @Override
        protected void invalidated() {
            if (this.currentModel != null) {
                FXFileListCell.this.unbind();
            }
            this.currentModel = this.get();
            if (this.currentModel != null) {
                FXFileListCell.this.bind(this.currentModel);
            }
        }
    };

    FXFileListCell() {
        this.setId("file_list_cell");
        this.isFolder = PseudoClass.getPseudoClass("folder");
        this.hasMovie = PseudoClass.getPseudoClass("hasMovie");
        GUICommon.loadFXMLRoot(this);
        this.rect.setId("file_list_cell_rect");
        this.nameLabel.setId("file_list_cell_title");
    }

    void setModel(DirectoryEntry model) {
        this.model.set(model);

        this.rect.pseudoClassStateChanged(this.hasMovie, model.hasMovie());
        this.pseudoClassStateChanged(this.isFolder, model.isDirectory());

        //this.dirMask.setVisible(model.isDirectory());

        if (model.getFileExtension() != null) {
            this.sizeLabel.setText(model.getMovieSize().map(/* -> s.toString() + " MB"*/UtilCommon::fileSizeString).getOrElse(""));
        } else {
            this.sizeLabel.setText("");
        }
    }

    private void bind(@NotNull DirectoryEntry model) {
        //genderImageView.imageProperty().bind(Bindings.<Image>select(modelProperty(), "gender", "image"));
        this.nameLabel.textProperty().bind(model.getFileNameProperty());
        this.typeLabel.textProperty().bind(model.getFileExtensionProperty());
    }

    private void unbind() {
        this.nameLabel.textProperty().unbind();
        this.typeLabel.textProperty().unbind();
    }
}

class FXFileListMakerCell extends AnchorPane {
    @FXML
    Label txtName, txtDesc;
    @FXML
    ImageView imgLogo;

    private final FXImageViewWrapper imageViewWrapper;

    FXFileListMakerCell() {
        this.setId("movie_list_cell");
        GUICommon.loadFXMLRoot(this);
        this.txtName.setId("file_list_cell_title");
        this.imageViewWrapper = new FXImageViewWrapper(this.imgLogo);
    }

    void setModel(@NotNull MakerData makerData) {
        this.imageViewWrapper.setKey(makerData.getMakerName());

        this.txtName.setText(makerData.getMakerName());
        this.txtDesc.setText(makerData.getMakerDesc());
        makerData.getMakerLogo().peek(i -> i.getImage(this.imageViewWrapper::setImage, makerData.getMakerName()));
    }
}

class FXFileListMovieCell extends AnchorPane {
    @FXML
    Label nameLabel;
    @FXML
    Label idLabel;
    @FXML
    Label studioLabel;
    @FXML
    Label typeLabel;
    @FXML
    Label pathLabel;
    @FXML
    ImageView imageView;

    @FXML
    Rectangle isHd, isWater;

    private final PseudoClass isVirtual;
    private final FXImageViewWrapper imageViewWrapper;

    FXFileListMovieCell() {
        this.setId("movie_list_cell");//.getStyleClass().add("pane_movie");
        this.isVirtual = PseudoClass.getPseudoClass("virtual");
        GUICommon.loadFXMLRoot(this);

        this.nameLabel.setId("file_list_cell_title");
        this.imageView.setPreserveRatio(true);
        this.imageViewWrapper = new FXImageViewWrapper(this.imageView);
        this.isHd.setOpacity(0.7);
        this.isWater.setOpacity(0.7);
    }

    void setModel(@NotNull DirectoryEntry model) {
        //GUICommon.debugMessage("t" + " || " + model.getMovieData().getMovieTitle().getTitle());

        //this.model.set(model);
        this.pseudoClassStateChanged(this.isVirtual, model.getMovieFolder().map(MovieFolder::isVirtual).getOrElse(false));

        if (model.getMovieData() != null) {
            //    this.titledPane.setText(model.getMovieData().getId().getId());
            this.idLabel.setText(model.getMovieData().getMovieID());
            this.nameLabel.setText(model.getMovieData().getMovieTitle());
            this.studioLabel.setText(model.getMovieData().getStudio());
            this.pathLabel.setText(model.getFilePath().toString());
            if (model.getFileExtension() != null) {
                if (model.getMovieFolder().map(MovieFolder::isVirtual).getOrElse(false)) {
                    this.typeLabel.setText("VIRTUAL");
                } else {
                    this.typeLabel.setText(model.getMovieSize().map(UtilCommon::fileSizeString).getOrElse(""));
                }
                // } else {
                //     int x = model.getFileExtension().indexOf("/");
                //this.typeLabel.setText(x > 0 ? model.getFileExtension().substring(0, x) : model.getFileExtension());
                //     this.typeLabel.setText(x > 0 ? model.getFileExtension().substring(x+1) : model.getFileExtension());
                // }
            } else {
                this.typeLabel.setText("");
            }

            this.imageViewWrapper.setKey(model.getFileName());
            BiConsumer<Image, ImageView> customProcessFront = (image, imageView) -> GUICommon.runOnFx(() -> {
                imageView.setImage(image);
                imageView.setViewport(null);
            });
            BiConsumer<Image, ImageView> customProcessBack = (image, imageView) -> GUICommon.runOnFx(() -> {
                imageView.setVisible(false);
                imageView.setImage(image);
                imageView.setViewport(FxThumb.getCoverCrop(image.getWidth(), image.getHeight(),
                        model.getMovieData().getMovieID().toString()));
                imageView.setVisible(true);
            });

            if (model.getMovieData().hasFrontCover()) {
                this.imageViewWrapper.customProcessor(customProcessFront);
                model.getMovieData().getImgFrontCover().peek(t -> t.getImage(this.imageViewWrapper::setImage, model.getFileName()));
            } else if (model.getMovieData().hasBackCover()) {
                this.imageViewWrapper.customProcessor(customProcessBack);
                model.getMovieData().getImgBackCover().peek(t -> t.getImage(this.imageViewWrapper::setImage, model.getFileName()));
            } else {
                this.imageView.setImage(null);
            }

            var flag = model.getMovieFolder().flatMap(MovieFolder::getCustomFlag);
            if (flag.isDefined()) {
                if (flag.get()._1) {
                    this.isHd.setFill(Color.web("#4e61cc"));
                } else {
                    this.isHd.setFill(Color.TRANSPARENT);
                }
                if (flag.get()._2) {
                    this.isWater.setFill(Color.web("#cc4f6c"));
                } else {
                    this.isWater.setFill(Color.TRANSPARENT);
                }
            } else {
                this.isHd.setFill(Color.web("#dfd0d6"));
                this.isWater.setFill(Color.web("#dfd0d6"));
            }
        }
    }

    void initializeComponent() {
        //FXController.of(this).fromDefaultLocation().load();
        this.nameLabel = new Label();
        this.nameLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-getLabel-overrun: ellipsis; -fx-effect: dropshadow( three-pass-box, rgba(0,0,0,0.4), 4, 0.3, 2, 2);");
        this.nameLabel.setPrefWidth(350);

        this.idLabel = new Label();
        this.idLabel.setFont(javafx.scene.text.Font.font(javafx.scene.text.Font.getDefault().getName(), FontWeight.BOLD, 12));
        this.idLabel.setPrefWidth(70);

        //this.studioLabel.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 12));

        this.typeLabel = new Label();
        this.typeLabel.setPrefWidth(60);
        this.typeLabel.setStyle("  -fx-font-size: 10px;\n" +
                "    -fx-background-color: linear-gradient(#D3D3D3, #778899);\n" +
                "    -fx-getLabel-fill: #ffffff;\n" +
                "  -fx-alignment: center;\n" +
                "  -fx-background-radius: 6;\n" +
                "    -fx-padding: 1px;");
        //this.typeLabel.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 12));
        //this.typeLabel.setTextFill(Color.GRAY);
        //nameLabel.setStroke(Color.web("#7080A0"));

        this.studioLabel = new Label();
        this.studioLabel.setStyle("-fx-font-size: 16;  -fx-getLabel-fill: linear-gradient(to bottom right, red, black); -fx-effect: dropshadow( three-pass-box, rgba(0,0,0,0.4), 3, 0.0, 2, 2);");
        this.studioLabel.setPrefWidth(130);

        this.idLabel.setPrefWidth(150);
        this.idLabel.setStyle("  -fx-border-color: derive(#A4C6FF, -20%);\n" +
                "  -fx-border-width: 2;\n" +
                "  -fx-alignment: center;\n" +
                "-fx-border-style: dotted solid dotted solid;\n" +
                //"  -fx-background-insets: 8;\n" +
                "  -fx-border-insets: 8;\n" +
                "  -fx-border-radius: 6;");

		    /*StackPane stack1 = new StackPane();
		    stack1.getChildren().addAll(rec1, nameLabel);
		    stack1.setAlignment(Pos.TOP_LEFT);     // Right-justify nodes in stack
		    StackPane.setMargin(nameLabel, new Insets(1, 10, 0, 28)); // Center "?"*/
        //this.add(this.statsRec, 0, 2);


        this.pathLabel = new Label();
        this.pathLabel.setStyle("-fx-font-size: 10; -fx-getLabel-overrun: leading-ellipsis;");
        this.pathLabel.setPrefWidth(375);

        HBox hBox1 = new HBox();
        hBox1.setSpacing(12);
        hBox1.setAlignment(Pos.CENTER_LEFT);
        hBox1.getChildren().addAll(this.idLabel, this.studioLabel, this.typeLabel);

        HBox hBox2 = new HBox();
        hBox2.setSpacing(12);
        hBox2.setAlignment(Pos.CENTER_LEFT);

        hBox2.setPadding(new Insets(0, 0, 0, 8));
        this.pathLabel.setPadding(new Insets(5, 0, 0, 10));

           /* this.titledPane = new TitledPane();
            this.titledPane.setCollapsible(false);//remove closing action
            this.titledPane.setAnimated(false);//stop animating
            this.titledPane.setPrefWidth(420);
            this.titledPane.setFocusTraversable(false);

            this.titledPane.setText("");
            this.titledPane.setContent(hBox);*/

        this.imageView = new ImageView();
        this.imageView.setFitHeight(60);
        this.imageView.setPreserveRatio(true);

        // this.add(separator, 1, 1);
        // this.add(this.typeLabel, 1, 2);

        //setMargin(separator, new Insets(4, 0, 0, 0));
        //setMargin(nameLabel, new Insets(2, 0, 0, 2));
    }

}

