package org.nagoya.controls;

import javafx.stage.StageStyle;
import org.nagoya.model.dataitem.ActorV2;
import org.nagoya.system.dialog.FXWindow;
import org.nagoya.system.dialog.WindowBuilder;

public class FXActorDetailWindow {
    private static FXActorDetailView fxActorDetailView = null;

    public static void show(ActorV2 actorV2) {
        if (fxActorDetailView == null) {
            fxActorDetailView = new FXActorDetailView();
        }

        var node = fxActorDetailView.show(actorV2);

        if (node != null) {
            FXWindow window = WindowBuilder.create()
                    .style(StageStyle.UTILITY)
                    .title("Actor Detail", false)
                    .body(node)
                    .resizable(false)
                    .buildSingle()
                    .pos(0, 100);

            fxActorDetailView.setWindowContainer(window.getRoot());
            window.show();
        }
    }

}
