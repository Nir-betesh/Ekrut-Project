package ekrut.client.managers;

import java.util.ArrayList;
import ekrut.client.EKrutClient;
import ekrut.entity.Order;
import ekrut.entity.OrderStatus;
import ekrut.net.ResultType;
import ekrut.net.ShipmentRequest;
import ekrut.net.ShipmentRequestType;
import ekrut.net.ShipmentResponse;
/**
 * This class represents the client side manager for handling shipments. It provides methods for fetching
 * orders for shipment in a specific area, confirming shipment, and confirming delivery of an order.
 * 
 * @author Nir Betesh
 */
public class ClientShipmentManager {
	
	private EKrutClient client;
	private Object lock = new Object();
	private ShipmentResponse shipmenResponse;
	

	/**
	 * Constructs a new ClientShipmentManager instance and registers an ShipmentResponse handler.
	 * 
	 * @param client the EKrutClient for which to register the ShipmentResponse handler
	 */
	public ClientShipmentManager(EKrutClient client) {
		this.client = client;
		client.registerHandler(ShipmentResponse.class, (res) -> {
			synchronized(lock) {
				shipmenResponse = res;
				lock.notify();
			}
		});
	}
	
	/**
	 * Fetches a list of orders that are ready for shipment in the specified area.
	 * 
	 * @param area the area to fetch orders for
	 * @return a list of orders ready for shipment
	 * @throws IllegalArgumentException if a null string is provided as the area
	 * @throws Exception if the result of the shipment request is not {@link ResultType#OK}
	 * or if an exception is thrown by the {@link #sendRequest(ShipmentRequest)} method
	 */
	public ArrayList<Order> fetchShipmentRequests(String area) throws Exception {
		
		if (area == null)
			throw new IllegalArgumentException("Null string was provided");
		
		// Prepare ShipmentRequest for sending.
		ShipmentRequest shipmentRequest = new ShipmentRequest(ShipmentRequestType.FETCH_SHIPMENT_ORDERS, area);
		ShipmentResponse shipmentResponse = sendRequest(shipmentRequest);
		
		// In case resultType isn't "OK" exception will throws.
		ResultType resultType = shipmentResponse.getResultCode();
		if (resultType != ResultType.OK)
			throw new Exception(resultType.toString()); // Q.Nir exception??
		
		return shipmentResponse.getOrdersForShipment();
	}
	
	/**
	 * Creates a request to the delivery department to approve sending a shipment for the specified order.
	 * 
	 * @param order the order to send a shipment request for
	 * @throws IllegalArgumentException if a null order is provided
	 * @throws Exception if the result of the shipment request is not {@link ResultType#OK}
	 * or if an exception is thrown by the {@link #sendRequest(ShipmentRequest)} method
	 */
	// Should return something? Q.Nir
	public void confirmShipment(Order order) throws Exception {
		if (order == null)
			throw new IllegalArgumentException("Null order was provided");

		// Prepare ShipmentRequest for sending.
		ShipmentRequest shipmentRequest = 
				new ShipmentRequest(ShipmentRequestType.UPDATE_STATUS, OrderStatus.AWAITING_DELIVERY, order.getOrderId());
		ShipmentResponse shipmentResponse = sendRequest(shipmentRequest);
		
		// In case resultType isn't "OK" exception will throws.
		ResultType resultType = shipmentResponse.getResultCode();
		if (resultType != ResultType.OK)
			throw new Exception(resultType.toString()); // Q.Nir exception??
	}

	/**
	 * Confirms the delivery of the specified order by sending a {@link ShipmentRequest} with the status set to 
	 * {@link OrderStatus#DELIVERY_CONFIRMED} and the order ID to the {@link #sendRequest(ShipmentRequest)} method.
	 * If the {@link ShipmentResponse#getResultCode()} is not {@link ResultType#OK},
	 * an {@link Exception} is thrown with the {@link ResultType} as the message.
	 * 
	 * @param order the order to confirm the delivery of
	 * @throws IllegalArgumentException if the provided order is null
	 * @throws Exception if the result of the shipment request is not {@link ResultType#OK} 
	 * or if an exception is thrown by the {@link #sendRequest(ShipmentRequest)} method
	 */
	public void confirmDelivery(Order order) throws Exception {
		if (order == null)
			throw new IllegalArgumentException("Null order was provided");
		
		ShipmentRequest shipmentRequest = 
				new ShipmentRequest(ShipmentRequestType.UPDATE_STATUS ,OrderStatus.DELIVERY_CONFIRMED, order.getOrderId());

		ShipmentResponse shipmentResponse = sendRequest(shipmentRequest);
		
		// In case resultType isn't "OK" exception will throws.
		ResultType resultType = shipmentResponse.getResultCode();
		if (resultType != ResultType.OK)
			throw new Exception(resultType.toString()); // Q.Nir exception??
	}
		
	/**
	 * Sets the status of the specified order to "done" by sending a {@link ShipmentRequest} with the
	 * status set to {@link OrderStatus#DONE} and the order ID to the {@link #sendRequest(ShipmentRequest)} method.
	 * In case the {@link ShipmentResponse#getResultCode()} is not {@link ResultType#OK}, an {@link Exception} is thrown.
	 * 
	 * @param order the order to set the status to "done"
	 * @throws IllegalArgumentException if the provided order is null
	 * @throws Exception if the result of the shipment request is not {@link ResultType#OK}
	 */
	public void setDone(Order order) throws Exception {
		if (order == null)
			throw new IllegalArgumentException("Null order was provided");
		
		ShipmentRequest shipmentRequest = 
				new ShipmentRequest(ShipmentRequestType.UPDATE_STATUS ,OrderStatus.DONE, order.getOrderId());
		ShipmentResponse shipmentResponse = sendRequest(shipmentRequest);
		
		// In case resultType isn't "OK" exception will throws.
		ResultType resultType = shipmentResponse.getResultCode();
		if (resultType != ResultType.OK)
			throw new Exception(resultType.toString()); // Q.Nir exception??
	}
	
	/**
	 * Sends a {@link ShipmentRequest} to the server and waits for a response.
	 * 
	 * @param shipmentRequest The {@link ShipmentRequest} to send to the server.
	 * @return The {@link ShipmentResponse} received from the server.
	 */
	private ShipmentResponse sendRequest(ShipmentRequest shipmentRequest) {
		this.shipmenResponse = null;
		client.sendRequestToServer(shipmentRequest);
		synchronized(lock) {
			while (shipmenResponse == null) {
				try {
					lock.wait();
				} catch (InterruptedException e) {}
			}
		}
		return shipmenResponse;
	}
}
