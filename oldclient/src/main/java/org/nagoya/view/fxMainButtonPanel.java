package org.nagoya.view;


import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

public class fxMainButtonPanel extends GridPane 
{
	
	private static final int iconSizeX = 16;
	private static final int iconSizeY = 16;

	
	public fxMainButtonPanel()
	{
		initJFXComponents();
	}
	
	protected void initJFXComponents() 
	{
	    setHgap(10);
	    setVgap(10);
	    setPadding(new Insets(0, 10, 0, 10));
	    
	    //grid.setStyle("-fx-background-color: #336699;");		
		
		Button btn = new Button("Up");
		//HBox hbBtn = new HBox(10);
		//hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
		//hbBtn.getChildren().add(btn);
		add(btn , 0, 0);
	}
	
}
