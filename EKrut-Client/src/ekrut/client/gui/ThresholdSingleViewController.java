package ekrut.client.gui;

import java.io.IOException;

import ekrut.client.managers.ClientInventoryManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;

/**
 * Threshold View Controller, shows the threshold data and manage threshold updating. 
 * 
 * @author Ofek Malka
 */
public class ThresholdSingleViewController extends HBox {

    @FXML
    private Label currThresholdLbl;

    @FXML
    private Label facilityNameLbl;

    @FXML
    private TextField thresholdTxt;

    @FXML
    private Button updateThresholdBtn;

    private ClientInventoryManager CIM;
    private String ekrutLocation;
    private int currThreshold;
	
    /**
     * Constructor for ThresholdSingleViewController class
     * Initializes the class variables and loads an FXML file
     * 
     * @param CIM the ClientInventoryManager object to be used in the class
     * @param ekrutLocation the ekrut location associated with the threshold
     * @param currThreshold the current threshold value
     */
	public ThresholdSingleViewController(ClientInventoryManager CIM, String ekrutLocation, int currThreshold) {
		this.CIM = CIM;
		this.ekrutLocation = ekrutLocation;
		this.currThreshold = currThreshold;
		
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ThresholdSingleView.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			
			fxmlLoader.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		currThresholdLbl.setText((currThreshold != 0) ? Integer.toString(currThreshold) : "N\\A");
		facilityNameLbl.setText(ekrutLocation);
		thresholdTxt.setText(Integer.toString(currThreshold));
	}
	
	/**
	 * Method to update the threshold value
	 * Validates the new threshold value and updates it if it's valid.
	 * 
	 * @param event the event that triggered this method. 
	 */
	@FXML
	void updateThreshold(ActionEvent event) {
		int newThreshold;
		try {
			newThreshold = Integer.parseInt(thresholdTxt.getText());
			if (newThreshold < 0)
				throw new NumberFormatException();
		} catch (NumberFormatException e) {
	    	Alert alert = new Alert(AlertType.ERROR, "Invalid value was entered.", ButtonType.OK);
	    	alert.showAndWait();
	    	thresholdTxt.setText(Integer.toString(currThreshold));
	    	return;
		}
		
		if (newThreshold == currThreshold) {
			Alert alert = new Alert(AlertType.WARNING, "New and current threshold values are equal.", ButtonType.OK);
    		alert.showAndWait();
    		return;
    	}
		
		
		try {
			CIM.updateItemThreshold(-1, ekrutLocation, newThreshold);
			currThreshold = newThreshold;
			currThresholdLbl.setText("Currently: " + Integer.toString(currThreshold));
			Alert alert = new Alert(AlertType.INFORMATION, "Threshold updated succesfuly.", ButtonType.OK);
    		alert.showAndWait();
		} catch(RuntimeException e) {
			Alert alert = new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK);
    		alert.showAndWait();
		}
		
	}
	
	
}
