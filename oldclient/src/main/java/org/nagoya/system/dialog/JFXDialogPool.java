package org.nagoya.system.dialog;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.events.JFXDialogEvent;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nagoya.GUICommon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A pool of {@link JFXDialog}
 *
 * @author Eric Chan
 * @version 1.0
 * @since 2018-11-15
 */
public class JFXDialogPool {

    /**************************************************************************
     *
     * Properties
     *
     **************************************************************************/
    private final List<JFXDialog> dialogs;
    private StackPane basePane;

    /**************************************************************************
     *
     * Constructors
     *
     **************************************************************************/
    @Contract(pure = true)
    public JFXDialogPool() {
        this.dialogs = new ArrayList<>();
        DialogBuilder.init(this::takeDialog);
    }

    public JFXDialogPool(int initPoolSize, StackPane stackPane) {
        this();
        this.initialize(initPoolSize, stackPane);
    }

    /**************************************************************************
     *
     * Methods
     *
     **************************************************************************/

    public void initialize(int initPoolSize, StackPane stackPane) {

        this.basePane = stackPane;

        for (int x = 0; x < initPoolSize; x++) {
            this.dialogs.add(this.createNewDialog());
        }
    }

    public int size() {
        return this.dialogs.size();
    }

    public synchronized JFXDialog takeDialog() {
        if (this.dialogs.size() == 0) {
            return this.createNewDialog();
        }
        JFXDialog useDialog = this.dialogs.remove(0);
        useDialog.setDialogContainer(this.basePane);
        return useDialog;
    }

    /*
    public void returnDialog(@NotNull JFXDialog dialog) {
        if (dialog.isVisible()) {
            dialog.setOnDialogClosed((e) -> this.dialogs.add((JFXDialog) e.getSource()));
            dialog.close();
        } else {
            this.dialogs.add(dialog);
        }
    }*/

    @Nullable
    @Contract(pure = true)
    private JFXDialog createNewDialog() {
        if (this.basePane == null) {
            return null;
        }

        JFXDialog newDialog = new JFXDialog(this.basePane, null, JFXDialog.DialogTransition.CENTER);

        //GUICommon.loadCssFile(newDialog, "customcell.css");

        this.resetDialog(newDialog);

        return newDialog;
    }

    public EventHandler<? super JFXDialogEvent> getOnClose() {
        return (e) -> {
            GUICommon.debugMessage("JFXDialog - onCloseEvent");
            JFXDialog dialogToReturn = (JFXDialog) e.getSource();
            this.resetDialog(dialogToReturn);
            this.dialogs.add(dialogToReturn);
        };
    }

    private void resetDialog(@NotNull JFXDialog dialog) {
        dialog.setOverlayClose(true);
        dialog.setOnDialogClosed(this.getOnClose());
        dialog.setTransitionType(JFXDialog.DialogTransition.CENTER);
        dialog.setContent(new Region());
    }

    @Contract("null -> null")
    private Node headingConverter(String heading) {
        if (StringUtils.isEmpty(heading)) {
            return null;
        }

        if (heading.length() > 90) {
            heading = heading.substring(0, 90) + "...";
        }

        return new Text(heading);
    }

    public JFXDialogLayout buildLayout(Node head, Node body, List<Node> actions) {
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

    public List<Node> buildButtonList(JFXDialog useDialog, String strBtnCancel, String strBtnOkay, Runnable runnable) {

        if (null == strBtnCancel) {
            if (null == strBtnOkay) {
                strBtnCancel = "Close";
            } else {
                strBtnCancel = "Cancel";
            }
        }

        List<Node> buttonList = new ArrayList<>();

        JFXButton buttonCancel = new JFXButton(strBtnCancel);
        buttonCancel.setOnAction(event -> useDialog.close());
        buttonList.add(buttonCancel);

        if (null != strBtnOkay) {
            JFXButton buttonOk = new JFXButton(strBtnOkay);
            buttonOk.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.CHECK_CIRCLE));
            buttonOk.setStyle(" -fx-border-color: #AAAAAA; -fx-border-insets: 1; -fx-border-radius: 4;");
            buttonOk.setOnAction(event -> {
                useDialog.close();
                if (runnable != null) {
                    runnable.run();
                }
            });
            buttonList.add(buttonOk);
        }
        return buttonList;
    }

    // Dialog with Body and Close Button
    public void showDialog(Node body) {
        this.showDialog(this.takeDialog(), null, body, "Close", null, null);
    }

    // Dialog with Heading getLabel, Body and Close Button
    public void showDialog(String heading, Node body) {
        this.showDialog(this.takeDialog(), this.headingConverter(heading), body, "Close", null, null);
    }

    // Dialog with Heading, Body and Close Button
    public void showDialog(Node heading, Node body) {
        this.showDialog(this.takeDialog(), heading, body, "Close", null, null);
    }

    public void showDialog(String heading, Node body, Runnable runnable) {
        this.showDialog(this.takeDialog(), this.headingConverter(heading), body, "Cancel", "Ok", runnable);
    }

    public void showDialog(Node heading, Node body, Runnable runnable) {
        this.showDialog(this.takeDialog(), heading, body, "Cancel", "Ok", runnable);
    }

    public void showDialog(String heading, Node body, String strBtnCancel, String strBtnOkay, Runnable runnable) {
        this.showDialog(this.takeDialog(), this.headingConverter(heading), body, strBtnCancel, strBtnOkay, runnable);
    }

    public void showDialog(Node heading, Node body, String strBtnCancel, String strBtnOkay, Runnable runnable) {
        this.showDialog(this.takeDialog(), heading, body, strBtnCancel, strBtnOkay, runnable);
    }

    public void showDialog(JFXDialog useDialog, Node heading, Node body, String strBtnCancel, String strBtnOkay, Runnable runnable) {
        this.showDialog(useDialog, heading, body, this.buildButtonList(useDialog, strBtnCancel, strBtnOkay, runnable));
    }

    public void showDialog(JFXDialog useDialog, Node heading, Node body, String strBtnCancel, String strBtnOkay, Runnable runnable, boolean useOverlayClose) {
        this.showDialog(useDialog, heading, body, this.buildButtonList(useDialog, strBtnCancel, strBtnOkay, runnable), useOverlayClose);
    }

    public void showDialog(JFXDialog useDialog, Node heading, Node body, List<Node> buttonList) {
        this.showDialog(useDialog, this.buildLayout(heading, body, buttonList), true);
    }

    public void showDialog(JFXDialog useDialog, Node heading, Node body, List<Node> buttonList, boolean useOverlayClose) {
        this.showDialog(useDialog, this.buildLayout(heading, body, buttonList), useOverlayClose);
    }

    public void showDialog(JFXDialog useDialog, JFXDialogLayout content, boolean useOverlayClose) {
        if (useDialog == null || content == null) {
            return;
        }

        useDialog.setContent(content);
        useDialog.setOverlayClose(useOverlayClose);

        if (!Thread.currentThread().getName().equals("JavaFX Application Thread")) {
            Platform.runLater(useDialog::show);
        } else {
            useDialog.show();
        }
    }
}
