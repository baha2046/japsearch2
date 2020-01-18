package org.nagoya.system.dialog;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nagoya.GUICommon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class DialogBuilder {
    private static Supplier<JFXDialog> dialogSupplier = JFXDialog::new;

    public static void init(Supplier<JFXDialog> supplier) {
        dialogSupplier = supplier;
    }

    @NotNull
    @Contract(" -> new")
    public static DialogBuilder create() {
        return new DialogBuilder(dialogSupplier.get());
    }

    public static DialogBuilder createWindow(String title) {
        return create().showInNewWindow(title);
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public static DialogBuilder create(JFXDialog dialog) {
        return new DialogBuilder(dialog);
    }

    private Node nHead;
    private Node nBody;
    private StackPane container;
    private List<Node> buttonList;
    private boolean useOverlayClose;

    private boolean ready;
    private final JFXDialog jfxDialog;

    WindowBuilder windowBuilder;

    private DialogBuilder(JFXDialog dialog) {
        this.nHead = null;
        this.nBody = null;
        this.container = null;
        this.buttonList = null;
        this.ready = false;
        this.useOverlayClose = true;
        this.jfxDialog = dialog;

        this.windowBuilder = null;
    }

    private DialogBuilder showInNewWindow(String title) {
        this.windowBuilder = WindowBuilder.create()
                .style(StageStyle.UTILITY)
                .title(title, false)
                .resizable(false)
                .runOnClose(this.jfxDialog::close);
        return this;
    }

    public JFXDialog getDialog() {
        return this.jfxDialog;
    }

    public DialogBuilder heading(String heading) {
        this.nHead = headingConverter(heading);
        return this;
    }

    public DialogBuilder heading(Node heading) {
        this.nHead = heading;
        return this;
    }

    public DialogBuilder body(String body) {
        this.nBody = new Text(body);
        return this;
    }

    public DialogBuilder body(Node body) {
        this.nBody = body;
        return this;
    }

    public DialogBuilder buttonClose() {
        return this.button("Close", null, null);
    }

    public DialogBuilder buttonYesNo(Runnable runnable) {
        return this.button("No", "Yes", runnable);
    }

    public DialogBuilder buttonOkCancel(Runnable runnable) {
        return this.button("Cancel", "Ok", runnable);
    }

    public DialogBuilder buttonSaveCancel(Runnable runnable) {
        return this.button("Cancel", "Save", runnable);
    }

    public DialogBuilder button(String n, String y, Runnable runnable) {
        return this.buttonCustom(buildButtonList(this.jfxDialog, n, y, runnable, this.windowBuilder));
    }

    public DialogBuilder buttonAdd(@NotNull Function<JFXDialog, JFXButton> dialogJFXButtonFunction) {
        return this.buttonAdd(dialogJFXButtonFunction.apply(this.jfxDialog));
    }

    public DialogBuilder buttonAdd(JFXButton button) {
        if (this.buttonList == null) {
            this.buttonList = new ArrayList<>();
        }
        this.buttonList.add(button);
        return this;
    }

    public DialogBuilder overlayClose(boolean use) {
        this.useOverlayClose = use;
        return this;
    }

    public DialogBuilder container(StackPane pane) {
        this.container = pane;
        return this;
    }

    public DialogBuilder buttonCustom(List<Node> btnList) {
        this.buttonList = btnList;
        return this;
    }

    public DialogBuilder closeOldWindowIfNeed(@NotNull Node node) {
        if (node.getScene() != null) {
            node.getScene().getWindow().getOnCloseRequest().handle(null);
        }
        return this;
    }

    public DialogBuilder build() {
        if (this.buttonList == null) {
            this.buttonClose();
        }
        JFXDialogLayout layout = buildLayout(this.nHead, this.nBody, this.buttonList);

        GUICommon.debugMessage("layout.getWidth()" + layout.getWidth());
        GUICommon.debugMessage("nBody.getPrefWidth()" + layout.getPrefWidth());
        GUICommon.debugMessage("layout.getMinWidth()" + layout.getMinWidth());
        GUICommon.debugMessage("layout.getMaxWidth()" + layout.getMaxWidth());


        this.jfxDialog.setContent(layout);
        this.jfxDialog.setOverlayClose(this.useOverlayClose);
        this.ready = true;
        return this;
    }

    public void showDialogInWindow(double w, double h) {
        if (this.ready && this.windowBuilder != null) {

            this.jfxDialog.setTransitionType(JFXDialog.DialogTransition.NONE);
            this.jfxDialog.setOverlayClose(false);

            var win = this.windowBuilder.prefSize(w + 15, h + 140).buildSingle();
            this.jfxDialog.show(win.getRoot());

            win.show();
        }
    }

    public void show() {
        if (this.ready) {
            Runnable run;

            if (this.windowBuilder != null) {
                run = () -> this.showDialogInWindow(600, 400);
            } else {
                run = () -> {
                    if (this.container != null) {
                        this.jfxDialog.show(this.container);
                    } else {
                        this.jfxDialog.show();
                    }
                };
            }

            if (!Thread.currentThread().getName().equals("JavaFX Application Thread")) {
                Platform.runLater(run);
            } else {
                run.run();
            }
        }
    }

    @Nullable
    private static Node headingConverter(String heading) {
        if (StringUtils.isEmpty(heading)) {
            return null;
        }

        if (heading.length() > 90) {
            heading = heading.substring(0, 90) + "...";
        }

        return new Text(heading);
    }

    @NotNull
    private static JFXDialogLayout buildLayout(Node head, Node body, List<Node> actions) {
        JFXDialogLayout layout = new JFXDialogLayout();

        if (head != null) {
            layout.setHeading(head);
        }

        layout.setBody(Objects.requireNonNullElseGet(body, Text::new));

        if (actions != null) {
            layout.setActions(actions);
        }

        return layout;
    }

    @NotNull
    private static List<Node> buildButtonList(JFXDialog useDialog, String strBtnCancel, String strBtnOkay, Runnable runnable, WindowBuilder newWindow) {

        if (null == strBtnCancel) {
            if (null == strBtnOkay) {
                strBtnCancel = "Close";
            } else {
                strBtnCancel = "Cancel";
            }
        }

        List<Node> buttonList = new ArrayList<>();

        EventHandler<ActionEvent> closeAction = (e) -> {
            useDialog.close();
            if (newWindow != null) {
                FXWindow.getWindow(newWindow.getTitle()).peek(FXWindow::hide);
            }
        };

        JFXButton buttonCancel = new JFXButton(strBtnCancel);
        buttonCancel.setOnAction(closeAction);
        buttonList.add(buttonCancel);

        if (null != strBtnOkay) {
            JFXButton buttonOk = new JFXButton(strBtnOkay);
            buttonOk.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.CHECK_CIRCLE));
            buttonOk.setStyle(" -fx-border-color: #AAAAAA; -fx-border-insets: 1; -fx-border-radius: 4;");
            buttonOk.setOnAction(event -> {
                closeAction.handle(event);
                if (runnable != null) {
                    runnable.run();
                }
            });
            buttonList.add(buttonOk);
        }
        return buttonList;
    }
}
