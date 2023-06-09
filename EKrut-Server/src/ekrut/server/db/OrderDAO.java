package ekrut.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import com.mysql.cj.MysqlType;
import ekrut.entity.Item;
import ekrut.entity.Order;
import ekrut.entity.OrderItem;
import ekrut.entity.OrderStatus;
import ekrut.entity.OrderType;
import ekrut.entity.User;
import ekrut.server.managers.DeadlockException;

/**
 * Handles all direct database interactions with orders
 * 
 * @author Almog Khaikin
 */
public class OrderDAO {

	private DBController con;
	private ItemDAO itemDAO;
	private UserDAO userDAO;

	/**
	 * Constructs a new OrderDAO that uses the provided controller
	 * 
	 * @param con the database controller to use
	 */
	public OrderDAO(DBController con) {
		this.con = con;
		this.itemDAO = new ItemDAO(con);
		this.userDAO = new UserDAO(con);
	}

	/**
	 * Changes the default ItemDAO to use for item related database operations.
	 * Useful for testing with mocks.
	 * 
	 * @param dao the ItemDAO to use
	 */
	public void setItemDAO(ItemDAO dao) {
		this.itemDAO = dao;
	}

	/**
	 * Creates a new order entry in the database and sets the order object's orderId
	 * field.
	 * 
	 * @param order    The order object to be created
	 * @param username The username of the customer who is placing the order
	 * @return true if the operation succeeded, false otherwise
	 * @throws DeadlockException when a deadlock occurs while creating the order
	 */
	public boolean createOrder(Order order, String username) throws DeadlockException {

		// Pass true so that we can get the new order ID
		PreparedStatement p1 = con.getPreparedStatement("INSERT INTO orders "
				+ "(date,status,type,dueDate,clientAddress,location,username) " + "VALUES(?,?,?,?,?,?,?)", true);

		PreparedStatement p2 = con
				.getPreparedStatement("INSERT INTO orderitems (orderId,itemId,itemQuantity) " + "VALUES(?,?,?)");

		PreparedStatement p3 = con.getPreparedStatement("UPDATE customers SET orderedAsSub = 1 WHERE username = ?");
		try {
			p1.setObject(1, order.getDate(), MysqlType.DATETIME);
			p1.setString(2, order.getStatus().toString());
			p1.setString(3, order.getType().toString());
			// Due date is set when the operator approves the shipment
			p1.setNull(4, MysqlType.DATETIME.getJdbcType());
			p1.setString(7, order.getUsername());
			// Ekrut location is irrelevant for shipment type orders
			if (order.getType() == OrderType.SHIPMENT) {
				p1.setString(5, order.getClientAddress());
				p1.setNull(6, Types.VARCHAR);
			} else {
				p1.setNull(5, Types.VARCHAR);
				p1.setString(6, order.getEkrutLocation());
			}

			int count = con.executeUpdate(p1);
			if (count != 1) {
				return false;
			}

			// Get the new order's ID
			ResultSet rs = p1.getGeneratedKeys();
			if (!rs.next()) {
				return false;
			}

			int orderId = rs.getInt(1);
			rs.close();

			// Save the ID in the order object for later use
			order.setOrderId(orderId);

			// Insert all the items in the order
			for (OrderItem oi : order.getItems()) {
				p2.setInt(1, orderId);
				p2.setInt(2, oi.getItem().getItemId());
				p2.setInt(3, oi.getItemQuantity());
				p2.addBatch();
			}

			int[] results = p2.executeBatch();
			for (int i : results) {
				if (i != 1) {
					return false;
				}
			}

			if (username != null) {
				p3.setString(1, username);
				if (p3.executeUpdate() != 1) {
					return false;
				}
			}

			return true;

		} catch (SQLException e) {
			if (e.getSQLState().equals("40001"))
				throw new DeadlockException();
			return false;
		} finally {
			try {
				p1.close();
				p2.close();
				p3.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private ArrayList<OrderItem> fetchOrderItems(int orderId) {
		PreparedStatement p = con.getPreparedStatement("SELECT * FROM orderitems WHERE orderId = ?");
		ArrayList<OrderItem> items = new ArrayList<>();

		try {
			p.setInt(1, orderId);
			ResultSet rs = p.executeQuery();
			// Get the order items
			while (rs.next()) {
				Item item = itemDAO.fetchItem(rs.getInt(2));
				OrderItem orderItem = new OrderItem(item, rs.getInt(3));
				items.add(orderItem);
			}

			return items;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				p.close();
			} catch (SQLException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	/**
	 * Returns the order with the specified ID.
	 * 
	 * @param orderId the ID of the order we want to retrieve
	 * @return the desired order if found, null otherwise
	 */
	public Order fetchOrderById(int orderId) {
		PreparedStatement p = con.getPreparedStatement("SELECT * FROM orders WHERE orderId = ?");

		try {
			p.setInt(1, orderId);
			ResultSet rs = p.executeQuery();
			// No order with such an ID was found
			if (!rs.next())
				return null;

			// Construct the main order object
			Order order = new Order(rs.getInt(1), rs.getObject(2, LocalDateTime.class),
					OrderStatus.valueOf(rs.getString(3)), OrderType.valueOf(rs.getString(4)),
					rs.getObject(5, LocalDateTime.class), rs.getString(6), rs.getString(7), rs.getString(8));

			order.setItems(fetchOrderItems(orderId));
			return order;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				p.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Returns the orders that belong to the given user
	 * 
	 * @param username the username of the user whose orders should be fetched
	 * @return the list of orders that belong to the user
	 */
	public ArrayList<Order> fetchOrdersByUsername(String username) {
		PreparedStatement p = con.getPreparedStatement("SELECT * FROM orders WHERE username = ?");

		try {
			p.setString(1, username);
			ResultSet rs = p.executeQuery();
			ArrayList<Order> orders = new ArrayList<>();

			while (rs.next()) {
				int orderId = rs.getInt(1);
				Order order = new Order(orderId, rs.getObject(2, LocalDateTime.class),
						OrderStatus.valueOf(rs.getString(3)), OrderType.valueOf(rs.getString(4)),
						rs.getObject(5, LocalDateTime.class), rs.getString(6), rs.getString(7), rs.getString(8));
				order.setItems(fetchOrderItems(orderId));
				orders.add(order);
			}

			return orders;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				p.close();
			} catch (SQLException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	/**
	 * Updates the status of an order in the database.
	 * 
	 * @param orderId the ID of the order whose status should be updated
	 * @param status  the new order status to set
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean updateOrderStatus(int orderId, OrderStatus status) {
		PreparedStatement p = con.getPreparedStatement("UPDATE orders SET status = ? WHERE orderId = ?");
		try {
			p.setString(1, status.toString());
			p.setInt(2, orderId);

			if (p.executeUpdate() != 1)
				return false;
			return true;

		} catch (SQLException e) {
			return false;

		} finally {
			try {
				p.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public ArrayList<Order> fetchOrderShipmentListByArea(String area) {
		PreparedStatement ps = con
				.getPreparedStatement("SELECT orderId, date, status, type, dueDate, clientAddress, location, username "
						+ "FROM orders " + "WHERE type = 'SHIPMENT'");
		ArrayList<Order> orderList = new ArrayList<>();
		try {
			ResultSet rs = con.executeQuery(ps);
			while (rs.next()) {
				User user = userDAO.fetchUserByUsername(rs.getString(8));
				if (user.getArea().equals(area)) {
					orderList.add(new Order(rs.getInt(1), rs.getObject(2, LocalDateTime.class),
							OrderStatus.valueOf(rs.getString(3)), OrderType.valueOf(rs.getString(4)),
							rs.getObject(5, LocalDateTime.class), rs.getString(6), rs.getString(7), rs.getString(8)));
				}
			}
			if (orderList.size() == 0)
				return null;

			return orderList;
		} catch (SQLException e) {
			return null;
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public boolean updateOrderDueDate(int orderId, LocalDateTime dueDate) {
		PreparedStatement ps = con.getPreparedStatement("UPDATE orders SET dueDate = ? WHERE orderId = ?");

		try {
			ps.setObject(1, dueDate, MysqlType.DATETIME);
			ps.setInt(2, orderId);
			if (ps.executeUpdate() != 1)
				return false;

			return true;

		} catch (SQLException e) {
			return false;

		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

	}
}
