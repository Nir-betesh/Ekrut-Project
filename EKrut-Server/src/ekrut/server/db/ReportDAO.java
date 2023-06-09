package ekrut.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.mysql.cj.MysqlType;
import ekrut.entity.Order;
import ekrut.entity.OrderType;
import ekrut.entity.Report;
import ekrut.entity.ReportType;

/**
 * Handles all direct database interactions with reports
 * 
 * @author Tal Gaon
 */
public class ReportDAO {
	private DBController con;
	private OrderDAO orderDAO;

	/**
	 * Constructs a new reportDAO that uses the provided controller
	 * 
	 * @param con      the database controller to use
	 * @param orderDAO to use it to fetch orders
	 */
	public ReportDAO(DBController con, OrderDAO orderDAO) {
		this.con = con;
		this.orderDAO = orderDAO;
	}

	public ReportDAO(DBController con) {
		this.con = con;
	}

	/**
	 * Gets the report ID for a report with the specified date, ekrut location,
	 * area, and type.
	 * 
	 * @param date          The date of the report.
	 * @param ekrutLocation The ekrut location of the report.
	 * @param area          The area of the report.
	 * @param type          The type of the report.
	 * @return The report ID of the report, or -1 if the report cannot be found.
	 * @throws Exception if an error occurs while executing the query.
	 */

	public Integer getReportID(LocalDateTime date, String ekrutLocation, String area, ReportType type)
			throws Exception {
		int reportid = -1;
		PreparedStatement ps = con.getPreparedStatement(
				"SELECT * FROM reports WHERE" + " EXTRACT(MONTH FROM date) = EXTRACT(MONTH FROM ?)"
						+ " AND EXTRACT(YEAR FROM date) = EXTRACT(YEAR FROM ?)"
						+ " AND ekrutLocation = ? AND type = ? AND area = ?");

		try {
			ps.setObject(1, date, MysqlType.DATETIME);
			ps.setObject(2, date, MysqlType.DATETIME);
			ps.setString(3, ekrutLocation);
			ps.setString(4, type.toString());
			ps.setString(5, area);
			ResultSet rs = con.executeQuery(ps);

			if (rs.first()) {
				reportid = rs.getInt("reportID");
			}

			if (reportid == -1)
				return null;

			return reportid;

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

	/**
	 * Fetches a report with the specified date, ekrut location, area, and type.
	 * 
	 * @param date          The date of the report.
	 * @param ekrutLocation The ekrut location of the report.
	 * @param area          The area of the report.
	 * @param type          The type of the report.
	 * @return The report with the specified date, ekrut location, area, and type,
	 *         or null if the report cannot be found.
	 */

	public Report fetchReport(LocalDateTime date, String ekrutLocation, String area, ReportType type) {
		try {
			Integer reportID = getReportID(date, ekrutLocation, area, type);

			// If there is not such report
			if (reportID == null)
				return null;

			Report report = null;
			// We will use the corresponding function according to the report type
			switch (type) {
			case ORDER:
				report = fetchOrderReportByID(reportID);
				break;
			case INVENTORY:
				report = fetchInventoryReportByID(reportID);
				break;
			case CUSTOMER:
				report = fetchCustomerReportByID(reportID);
				break;
			default:
				break;
			}

			return report;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Fetches a customer report with the specified report ID.
	 * 
	 * @param reportID The ID of the report to fetch.
	 * @return The customer report with the specified ID, or null if the report
	 *         cannot be found.
	 */
	public Report fetchCustomerReportByID(Integer reportID) {
		PreparedStatement ps1 = con.getPreparedStatement("SELECT * FROM reports WHERE reportID = ?");
		PreparedStatement ps2 = con.getPreparedStatement("SELECT * FROM customers_report_data WHERE reportID = ?");
		PreparedStatement ps3 = con.getPreparedStatement("SELECT * FROM customer_reports WHERE reportID = ?");

		try {
			ps1.setInt(1, reportID);
			ResultSet rs1 = con.executeQuery(ps1);

			if (!rs1.next())
				return null;

			ps2.setInt(1, reportID);
			ResultSet rs2 = con.executeQuery(ps2);

			Map<String, Integer> customersHistogram = new HashMap<>();

			// Add each row to customerReportData
			if (rs2.first()) {
				customersHistogram.put("1", rs2.getInt("1"));
				customersHistogram.put("2", rs2.getInt("2"));
				customersHistogram.put("3", rs2.getInt("3"));
				customersHistogram.put("4", rs2.getInt("4"));
				customersHistogram.put("5", rs2.getInt("5"));
				customersHistogram.put("6+", rs2.getInt("6+"));
			}

			ps3.setInt(1, reportID);
			ResultSet rs3 = con.executeQuery(ps3);

			Map<Integer, Integer> customersOrdersByDate = new HashMap<>();

			if (rs3.first()) {
				for (int i = 1; i <= 31; i++) {
					customersOrdersByDate.put(i, rs3.getInt(String.valueOf(i)));
				}
			}

			// Create a report instance
			Report report = new Report(rs1.getInt("reportID"), ReportType.valueOf(rs1.getString("type")),
					rs1.getObject(("date"), LocalDateTime.class), rs1.getString("ekrutLocation"), rs1.getString("area"),
					customersHistogram, customersOrdersByDate);

			return report;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				ps1.close();
				ps2.close();
				ps3.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Fetches an order report with the specified report ID.
	 * 
	 * @param reportID The ID of the report to fetch.
	 * @return The order report with the specified ID, or null if the report cannot
	 *         be found.
	 */
	public Report fetchOrderReportByID(Integer reportID) {
		PreparedStatement ps1 = con.getPreparedStatement("SELECT * FROM reports WHERE reportID = ?");
		PreparedStatement ps2 = con.getPreparedStatement("SELECT * FROM orders_report_data WHERE reportID = ?");
		PreparedStatement ps3 = con.getPreparedStatement("SELECT * FROM top_sellers WHERE reportID = ?");
		PreparedStatement ps4 = con.getPreparedStatement("SELECT * FROM monthly_orders WHERE reportID = ?");

		try {

			ps1.setInt(1, reportID);
			ResultSet rs1 = con.executeQuery(ps1);

			if (!rs1.next())
				return null;

			ps2.setInt(1, reportID);
			ResultSet rs2 = con.executeQuery(ps2);

			Map<String, ArrayList<Integer>> orderReportData = new HashMap<>();

			while (rs2.next()) {
				ArrayList<Integer> temp = new ArrayList<>();
				temp.add(rs2.getInt("totalOrders"));
				temp.add(rs2.getInt("pickup"));
				temp.add(rs2.getInt("remote"));
				temp.add(rs2.getInt("totalOrdersInILS"));
				temp.add(rs2.getInt("pickupInILS"));
				temp.add(rs2.getInt("remoteInILS"));
				orderReportData.put(rs2.getString("ekrutLocation"), temp);
			}

			ps3.setInt(1, reportID);
			ResultSet rs3 = con.executeQuery(ps3);

			// fetch top sellers
			Map<String, Integer> topSellersData = new HashMap<>();

			while (rs3.next()) {
				topSellersData.put(rs3.getString("itemName"), rs3.getInt("totalSales"));
			}

			ps4.setInt(1, reportID);
			ResultSet rs4 = con.executeQuery(ps4);

			if (!rs4.next())
				return null;

			Report report = new Report(rs1.getInt("reportID"), ReportType.valueOf(rs1.getString("type")),
					rs1.getObject(("date"), LocalDateTime.class), rs1.getString("ekrutLocation"), rs1.getString("area"),
					rs4.getInt("totalOrders"), rs4.getInt("totalOrdersInILS"), rs4.getInt("totalShipmentOrders"),
					rs4.getInt("totalShipmentOrdersInILS"), orderReportData, topSellersData);

			return report;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				ps1.close();
				ps2.close();
				ps3.close();
				ps4.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Return an inventory report with the given report ID.
	 * 
	 * @param reportID the ID of the report to retrieve
	 * @return a {@link Report} object containing the report data, or null if the
	 *         report does not exist
	 */
	public Report fetchInventoryReportByID(Integer reportID) {
		PreparedStatement ps1 = con.getPreparedStatement("SELECT * FROM reports WHERE reportID = ?");
		PreparedStatement ps2 = con.getPreparedStatement("SELECT * FROM inventory_report_data WHERE reportID = ?");
		try {
			ps1.setInt(1, reportID);
			ResultSet rs1 = con.executeQuery(ps1);

			if (!rs1.next())
				return null;

			ps2.setInt(1, reportID);
			ResultSet rs2 = con.executeQuery(ps2);

			Integer threshold = null;

			Map<String, ArrayList<Integer>> inventoryReportData = new HashMap<>();
			// Put items into the map: for each itemName -> thresholdBreaches
			while (rs2.next()) {
				ArrayList<Integer> temp = new ArrayList<>();
				temp.add(rs2.getInt("thresholdBreaches"));
				inventoryReportData.put(rs2.getString("itemName"), temp);
				threshold = rs2.getInt("threshold");
			}
			// Create a report instance
			Report report = new Report(rs1.getInt("reportID"), ReportType.valueOf(rs1.getString("type")),
					rs1.getObject(("date"), LocalDateTime.class), rs1.getString("ekrutLocation"), rs1.getString("area"),
					inventoryReportData, threshold);

			return report;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				ps1.close();
				ps2.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Retrieves a list of all orders made at a specific location in a specific
	 * month.
	 *
	 * @param date     the month to retrieve orders for
	 * @param location the location to retrieve orders for
	 * @return a list of orders made at the specified location in the specified
	 *         month, or null if an error occurred
	 */
	public ArrayList<Order> fetchAllMonthlyOrdersByLocation(LocalDateTime date, String location) {

		PreparedStatement ps1 = con.getPreparedStatement(
				"SELECT orderId FROM orders WHERE" + " EXTRACT(MONTH FROM date) = EXTRACT(MONTH FROM ?) AND"
						+ " EXTRACT(YEAR FROM date) = EXTRACT(YEAR FROM ?) AND location = ?");
		try {
			ps1.setObject(1, date, MysqlType.DATETIME);
			ps1.setObject(2, date, MysqlType.DATETIME);
			ps1.setString(3, location);
			ResultSet rs1 = con.executeQuery(ps1);

			ArrayList<Order> orders = new ArrayList<>();
			// Fetch all the orders by ID and add them to orders ArrayList
			while (rs1.next()) {
				orders.add(orderDAO.fetchOrderById(rs1.getInt("orderId")));
			}
			return orders;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				ps1.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}

		}
	}

	/**
	 * Retrieves a list of all shipment orders made in a specific month.
	 *
	 * @param date the month to retrieve orders for
	 * @return a list of shipment orders made in the specified month, or null if an
	 *         error occurred
	 */
	public ArrayList<Order> fetchAllMonthlyShipmentOrders(LocalDateTime date) {

		PreparedStatement ps1 = con.getPreparedStatement(
				"SELECT orderId FROM orders WHERE" + " EXTRACT(MONTH FROM date) = EXTRACT(MONTH FROM ?) AND"
						+ " EXTRACT(YEAR FROM date) = EXTRACT(YEAR FROM ?) AND type = ?");
		try {
			ps1.setObject(1, date, MysqlType.DATETIME);
			ps1.setObject(2, date, MysqlType.DATETIME);
			ps1.setString(3, OrderType.SHIPMENT.toString());
			ResultSet rs1 = con.executeQuery(ps1);

			ArrayList<Order> orders = new ArrayList<>();
			// Fetch all the orders by ID and add them to orders ArrayList
			while (rs1.next()) {
				orders.add(orderDAO.fetchOrderById(rs1.getInt("orderId")));
			}
			return orders;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				ps1.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}

		}
	}

	/**
	 * Fetches the username of all orders from the database that have the same month
	 * and year as the given date, and the same location as the given location.
	 * 
	 * @param date     the reference date to filter the orders by month and year
	 * @param location the location to filter the orders by
	 * @return a list of all usernames of orders that meet the criteria, or null if
	 *         an exception is thrown
	 */
	public ArrayList<String> getAllCustomersOrdersByName(LocalDateTime date, String location) {

		PreparedStatement ps1 = con.getPreparedStatement(
				"SELECT username FROM orders WHERE" + " EXTRACT(MONTH FROM date) = EXTRACT(MONTH FROM ?) AND"
						+ " EXTRACT(YEAR FROM date) = EXTRACT(YEAR FROM ?) AND location = ?");
		try {
			ps1.setObject(1, date, MysqlType.DATETIME);
			ps1.setObject(2, date, MysqlType.DATETIME);
			ps1.setString(3, location);

			ResultSet rs1 = con.executeQuery(ps1);

			ArrayList<String> customersOrders = new ArrayList<>();

			while (rs1.next()) {
				customersOrders.add(rs1.getString("username"));
			}

			return customersOrders;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				ps1.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Retrieves a list of all customer names who made orders (excluding shipment
	 * orders) at a specific location in a specific month.
	 *
	 * @param date     the month to retrieve orders for
	 * @param location the location to retrieve orders for
	 * @return a list of customer names who made orders at the specified location in
	 *         the specified month, or null if an error occurred
	 */
	public ArrayList<String> getAllCustomersOrdersByNameWithOutShipment(LocalDateTime date, String location) {

		PreparedStatement ps1 = con.getPreparedStatement(
				"SELECT username FROM orders WHERE" + " EXTRACT(MONTH FROM date) = EXTRACT(MONTH FROM ?) AND"
						+ " EXTRACT(YEAR FROM date) = EXTRACT(YEAR FROM ?) AND location = ? AND type != 'SHIPMENT'");
		try {
			ps1.setObject(1, date, MysqlType.DATETIME);
			ps1.setObject(2, date, MysqlType.DATETIME);
			ps1.setString(3, location);

			ResultSet rs1 = con.executeQuery(ps1);

			ArrayList<String> customersOrders = new ArrayList<>();

			while (rs1.next()) {
				customersOrders.add(rs1.getString("username"));
			}

			return customersOrders;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				ps1.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Retrieves a list of all order dates (excluding shipment orders) made by
	 * customers at a specific location in a specific month.
	 *
	 * @param date     the month to retrieve orders for
	 * @param location the location to retrieve orders for
	 * @return a list of order dates made at the specified location in the specified
	 *         month, or null if an error occurred
	 */
	public ArrayList<LocalDateTime> getAllCustomersOrdersByDateWithOutShipment(LocalDateTime date, String location) {

		PreparedStatement ps1 = con.getPreparedStatement(
				"SELECT date FROM orders WHERE" + " EXTRACT(MONTH FROM date) = EXTRACT(MONTH FROM ?) AND"
						+ " EXTRACT(YEAR FROM date) = EXTRACT(YEAR FROM ?) AND location = ? AND type != 'SHIPMENT'");
		try {
			ps1.setObject(1, date, MysqlType.DATETIME);
			ps1.setObject(2, date, MysqlType.DATETIME);
			ps1.setString(3, location);

			ResultSet rs1 = con.executeQuery(ps1);

			ArrayList<LocalDateTime> customersOrdersByDate = new ArrayList<>();

			while (rs1.next()) {
				customersOrdersByDate.add(rs1.getObject(("date"), LocalDateTime.class));
			}

			return customersOrdersByDate;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				ps1.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Fetches the item names of all threshold breaches from the database that have
	 * the same month and year as the given date, and the same location as the given
	 * location.
	 * 
	 * @param date          the reference date to filter the threshold breaches by
	 *                      month and year
	 * @param ekrutLocation the location to filter the threshold breaches by
	 * @return a list of all item names of threshold breaches that meet the
	 *         criteria, or null if an exception is thrown
	 */
	public ArrayList<String> getThresholdBreaches(LocalDateTime date, String ekrutLocation) {

		PreparedStatement ps1 = con.getPreparedStatement(
				"SELECT itemName FROM threshold_breaches WHERE" + " EXTRACT(MONTH FROM date) = EXTRACT(MONTH FROM ?)"
						+ " AND EXTRACT(YEAR FROM date) = EXTRACT(YEAR FROM ?) AND ekrutLocation = ?");
		try {
			ps1.setObject(1, date, MysqlType.DATETIME);
			ps1.setObject(2, date, MysqlType.DATETIME);
			ps1.setString(3, ekrutLocation);

			ResultSet rs1 = con.executeQuery(ps1);

			ArrayList<String> thresholdAlerts = new ArrayList<>();

			while (rs1.next()) {
				thresholdAlerts.add(rs1.getString("itemName"));
			}

			return thresholdAlerts;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				ps1.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Creates a new report in the database with the given report object's type,
	 * date, and location. Sets the report object's report ID to the ID of the new
	 * report in the database.
	 * 
	 * @param report the report object containing the type, date, and location to
	 *               use for the new report
	 * @return true if the report is successfully created, false otherwise
	 */
	public boolean createReport(Report report) {

		// Pass true so that we can get the new report ID
		PreparedStatement p1 = con.getPreparedStatement(
				"INSERT INTO reports " + "(type,date,ekrutLocation,area) " + "VALUES(?,?,?,?)", true);

		try {
			p1.setString(1, report.getReportType().toString());
			p1.setObject(2, report.getDate(), MysqlType.DATETIME);
			p1.setString(3, report.getEkrutLocation());
			p1.setString(4, report.getArea());

			int res = p1.executeUpdate();
			if (res != 1) {
				return false;
			}
			// Get the new report ID
			ResultSet rs = p1.getGeneratedKeys();
			if (!rs.next()) {
				return false;
			}
			// TBD check, probably this is'nt required
			int reportID = rs.getInt(1);
			rs.close();

			// Save the ID in the report object for later use
			report.setReportID(reportID);

			return true;

		} catch (SQLException e) {
			return false;
		} finally {
			try {
				p1.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * This method creates a new order report in the database based on the provided
	 * report object.
	 * 
	 * @param report the report to be created in the database
	 * @return true if the report was successfully created, false otherwise
	 */
	public boolean createOrderReport(Report report) {

		con.beginTransaction();

		PreparedStatement ps1 = con
				.getPreparedStatement("INSERT INTO top_sellers" + " (reportID,itemName,totalSales) " + "VALUES(?,?,?)");

		PreparedStatement ps2 = con.getPreparedStatement("INSERT INTO orders_report_data"
				+ " (reportID,ekrutLocation,totalOrders,pickup,remote,totalOrdersInILS,pickupInILS,remoteInILS) "
				+ "VALUES(?,?,?,?,?,?,?,?)");

		PreparedStatement ps3 = con.getPreparedStatement("INSERT INTO monthly_orders"
				+ " (reportID,totalOrders,totalOrdersInILS,totalShipmentOrders,totalShipmentOrdersInILS) "
				+ "VALUES(?,?,?,?,?)");

		if (!createReport(report)) {
			con.abortTransaction();
			return false;
		}
		Integer reportID = report.getReportID();

		try {
			// Insert topSellers
			for (Map.Entry<String, Integer> entry : report.getTopSellersData().entrySet()) {
				ps1.setInt(1, reportID);
				ps1.setString(2, entry.getKey());
				ps1.setInt(3, entry.getValue());
				ps1.addBatch();
			}

			int[] results1 = ps1.executeBatch();
			for (int i : results1) {
				if (i != 1) {
					con.abortTransaction();
					return false;
				}
			}

			for (Map.Entry<String, ArrayList<Integer>> entry : report.getOrderReportData().entrySet()) {
				ps2.setInt(1, reportID);
				ps2.setString(2, entry.getKey());
				ps2.setInt(3, entry.getValue().get(0));
				ps2.setInt(4, entry.getValue().get(1));
				ps2.setInt(5, entry.getValue().get(2));
				ps2.setInt(6, entry.getValue().get(3));
				ps2.setInt(7, entry.getValue().get(4));
				ps2.setInt(8, entry.getValue().get(5));
				ps2.addBatch();
			}

			int[] results2 = ps2.executeBatch();
			for (int i : results2) {
				if (i != 1) {
					con.abortTransaction();
					return false;
				}
			}

			ps3.setInt(1, reportID);
			ps3.setInt(2, report.getTotalOrders());
			ps3.setInt(3, report.getTotalOrdersInILS());
			ps3.setInt(4, report.getTotalShipmentOrders());
			ps3.setInt(5, report.getTotalShipmentOrdersInILS());

			int res = ps3.executeUpdate();
			if (res != 1) {
				con.abortTransaction();
				return false;
			}
			con.commitTransaction();
			return true;

		} catch (SQLException e) {
			con.abortTransaction();
			return false;
		} finally {
			try {
				ps1.close();
				ps2.close();
				ps3.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Creates a customer report and stores it in the database.
	 *
	 * @param report the report to be created and stored
	 * @return true if the report was successfully created and stored, false
	 *         otherwise
	 */
	public boolean createCustomerReport(Report report) {

		con.beginTransaction();

		PreparedStatement ps1 = con.getPreparedStatement("INSERT INTO customers_report_data"
				+ " (reportID,`1`,`2`,`3`,`4`,`5`,`6+`)" + " VALUES(?,?,?,?,?,?,?)");

		PreparedStatement ps2 = con.getPreparedStatement("INSERT INTO customer_reports"
				+ " (reportID,`1`,`2`,`3`,`4`,`5`,`6`,`7`,`8`,`9`,`10`,`11`,`12`,`13`,`14`,`15`,`16`,`17`,`18`,`19`,`20`,`21`,`22`,`23`,`24`,`25`,`26`,`27`,`28`,`29`,`30`,`31`)"
				+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		if (!createReport(report)) {
			con.abortTransaction();
			return false;
		}
		Integer reportID = report.getReportID();

		try {

			ps1.setInt(1, reportID);
			ps1.setInt(2, report.getCustomerReportData().get("1"));
			ps1.setInt(3, report.getCustomerReportData().get("2"));
			ps1.setInt(4, report.getCustomerReportData().get("3"));
			ps1.setInt(5, report.getCustomerReportData().get("4"));
			ps1.setInt(6, report.getCustomerReportData().get("5"));
			ps1.setInt(7, report.getCustomerReportData().get("6+"));

			int res1 = ps1.executeUpdate();
			if (res1 != 1) {
				con.abortTransaction();
				return false;
			}

			ps2.setInt(1, reportID);
			for (int i = 1; i <= 31; i++) {
				ps2.setInt(i + 1, report.getCustomersOrdersByDate().get(i));
			}

			int res2 = ps2.executeUpdate();
			if (res2 != 1) {
				con.abortTransaction();
				return false;
			}

			con.commitTransaction();
			return true;

		} catch (SQLException e) {
			con.abortTransaction();
			return false;
		} finally {
			try {
				ps1.close();
				ps2.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Creates an inventory report in the database using the given report object.
	 * 
	 * @param report the report object containing the report data
	 * @return true if the report was created successfully, false otherwise
	 */
	public boolean createInventoryReport(Report report) {
		con.beginTransaction();

		PreparedStatement ps1 = con.getPreparedStatement("INSERT INTO inventory_report_data"
				+ " (reportID,itemName,threshold,thresholdBreaches) " + "VALUES(?,?,?,?)");
		if (!createReport(report)) {
			con.abortTransaction();
			return false;
		}
		Integer reportID = report.getReportID();

		int threshold = report.getThreshold();

		try {
			// Iterate over the entries in the Map contained in the report object
			for (Map.Entry<String, ArrayList<Integer>> entry : report.getInventoryReportData().entrySet()) {
				// Get the item name and threshold data from the entry
				String itemName = entry.getKey();
				ArrayList<Integer> thresholdData = entry.getValue();

				// tresholdData(0) is how many breaches that item "cause"
				int thresholdBreaches = thresholdData.get(0);

				ps1.setInt(1, reportID);
				ps1.setString(2, itemName);
				ps1.setInt(3, threshold);
				ps1.setInt(4, thresholdBreaches);
				ps1.addBatch();

			}

			int[] results = ps1.executeBatch();
			for (int i : results) {
				if (i != 1) {
					con.abortTransaction();
					return false;
				}
			}
			con.commitTransaction();
			return true;

		} catch (SQLException e) {
			con.abortTransaction();
			return false;
		} finally {
			try {
				ps1.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Retrieves a list of facilities in a given area.
	 *
	 * @param area a string representing the area to search for facilities
	 * @return an ArrayList of strings, each representing a facility in the given
	 *         area. If no facilities are found or there is an error, the method
	 *         returns null.
	 */
	public ArrayList<String> fetchFacilitiesByArea(String area) {

		PreparedStatement ps1 = con.getPreparedStatement("SELECT ekrutLocation FROM ekrut_machines WHERE area = ?");

		try {
			ps1.setString(1, area);

			ResultSet rs1 = con.executeQuery(ps1);

			ArrayList<String> facilities = new ArrayList<>();

			while (rs1.next()) {
				facilities.add(rs1.getString("ekrutLocation"));
			}

			return facilities;

		} catch (SQLException e) {
			return null;
		} finally {
			try {
				ps1.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Check if the latest report is updated.
	 * 
	 * @param date the date to check
	 * @return true if the latest report is updated, false otherwise
	 */
	public boolean isLatestReportUpdated(LocalDateTime date) {
		boolean exists = true;

		PreparedStatement ps1 = con
				.getPreparedStatement("SELECT 1 FROM reports WHERE EXTRACT(MONTH FROM date) = EXTRACT(MONTH FROM ?)"
						+ " AND EXTRACT(YEAR FROM date) = EXTRACT(YEAR FROM ?) LIMIT 1");

		try {
			ps1.setObject(1, date, MysqlType.DATETIME);
			ps1.setObject(2, date, MysqlType.DATETIME);
			ResultSet rs1 = con.executeQuery(ps1);

			if (!rs1.next()) {
				exists = false;
			}

			return exists;

		} catch (SQLException e) {
			return true;
		} finally {
			try {
				ps1.close();

			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
