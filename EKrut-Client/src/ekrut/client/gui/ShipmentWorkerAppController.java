package ekrut.client.gui;

import java.io.IOException;

import ekrut.client.EKrutClientUI;
import ekrut.client.managers.ClientShipmentManager;
import ekrut.entity.Order;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * This class is used to handle the shipment worker approval process, it
 * contains the order information and confirm button.
 * 
 * @author Nir Betesh
 */
public class ShipmentWorkerAppController extends HBox {

	@FXML
	private Button confirmBtn;

	@FXML
	private Text orderDateTxt;

	@FXML
	private Text orderIdTxt;

	private Order order;
	private ClientShipmentManager clientShipmentManager;

	/**
	 * Constructor that initializes the ShipmentWorkerAppController object and sets
	 * the order, orderId and orderDate information.
	 * 
	 * @param order order object that contains the information to be displayed in
	 *              the controller.
	 */
	public ShipmentWorkerAppController(Order order) {
		this.order = order;
		clientShipmentManager = EKrutClientUI.getEkrutClient().getClientShipmentManager();
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ShipmentWorkerApp.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);

		try {
			fxmlLoader.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		orderIdTxt.setText(order.getOrderId().toString());
		StringBuilder date = new StringBuilder(order.getDate().toString());
		date.setCharAt(10, ' ');
		orderDateTxt.setText(date + " o'clock");
	}

	/**
	 * Method that is called when the confirm button is pressed. It confirms the
	 * shipment and removes the current object from its parent container.
	 * 
	 * @param event the event that triggers the method call
	 */
	@FXML
	void confirmShipment(ActionEvent event) {
		clientShipmentManager.confirmShipment(order);
		((VBox) getParent()).getChildren().remove(this);
	}
}
