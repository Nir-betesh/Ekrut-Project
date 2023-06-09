package ekrut.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import ekrut.entity.Customer;
import ekrut.entity.User;
import ekrut.entity.UserRegistration;
import ekrut.entity.UserType;

/**
 * The `UserDAO` class provides methods for fetching and creating users in a
 * database.
 * 
 * @author Yovel Gabay
 */
public class UserDAO {

	private DBController con;

	public UserDAO(DBController con) {
		this.con = con;
	}

	/**
	 * Fetches a user from the database by their username.
	 *
	 * @param username The username of the user to fetch.
	 * @return The `User` object if found, or `null` if not found or an error
	 *         occurred.
	 */
	public User fetchUserByUsername(String username) {
		User user = null;
		PreparedStatement ps = con.getPreparedStatement("SELECT * FROM users WHERE username = ?");
		try {
			ps.setString(1, username);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				// (userType, username, password, firstName, lastName, id, email, phoneNumber,
				// area)
				user = new User(UserType.valueOf(rs.getString(1)), username, rs.getString(3), rs.getString(4),
						rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9));
				Customer customerInfo = fetchCustomerInfo(user);
				user.setCustomerInfo(customerInfo);
			}

			return user;
		} catch (SQLException e1) {
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
	 * Fetches a user from the database by their phone number.
	 *
	 * @param phoneNumber The phone number of the user to fetch.
	 * @return The `User` object if found, or `null` if not found or an error
	 *         occurred.
	 */
	
	public User fetchUserByPhoneNumber(String phoneNumber) {
		User user = null;
		PreparedStatement ps = con.getPreparedStatement("SELECT * FROM users WHERE phoneNumber = ?");
		try {
			ps.setString(1, phoneNumber);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				user = new User(UserType.valueOf(rs.getString(1)), rs.getString(2), rs.getString(3), rs.getString(4),
						rs.getString(5), rs.getString(6), rs.getString(7), phoneNumber, rs.getString(9));
				Customer customerInfo = fetchCustomerInfo(user);
				user.setCustomerInfo(customerInfo);
			}

			return user;
		} catch (SQLException e1) {
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
	 * Fetches a user from the database by their email.
	 *
	 * @param email The email of the user to fetch.
	 * @return The `User` object if found, or `null` if not found or an error
	 *         occurred.
	 */
	public User fetchUserByEmail(String email) {
		User user = null;
		PreparedStatement ps = con.getPreparedStatement("SELECT * FROM users WHERE email = ?");
		try {
			ps.setString(1, email);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				// (userType, username, password, firstName, lastName, id, email, phoneNumber,
				// area)
				user = new User(UserType.valueOf(rs.getString(1)), rs.getString(2), rs.getString(3), rs.getString(4),
						rs.getString(5), rs.getString(6), email, rs.getString(8), rs.getString(9));
				Customer customerInfo = fetchCustomerInfo(user);
				user.setCustomerInfo(customerInfo);
			}

			return user;
		} catch (SQLException e1) {
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
	 * Retrieves customer specific info from the database.
	 * 
	 * @param user the user whose info should be retrieved
	 * @return the customer specific info of the user
	 */
	public Customer fetchCustomerInfo(User user) {
		if (user.getUserType() == UserType.REGISTERED)
			return null;

		PreparedStatement ps = con.getPreparedStatement("SELECT * FROM customers WHERE username = ?");

		try {
			ps.setString(1, user.getUsername());
			ResultSet rs = ps.executeQuery();

			if (rs.next())
				return new Customer(rs.getInt("subscriberNumber"), rs.getString("username"),
						rs.getBoolean("monthlyCharge"), rs.getString("creditCardNumber"),
						rs.getBoolean("orderedAsSub"));
			return null;

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
	 * This method retrieves a user with the role of AREA_MANAGER from the database
	 * based on the specified area.
	 * 
	 * @param area The area for which the AREA_MANAGER is being retrieved.
	 * @return The AREA_MANAGER user for the specified area, or null if no such user
	 *         exists in the database. (role, username, password, firstName,
	 *         lastName, id, email, phoneNumber, area)
	 */
	public User fetchManagerByArea(String area) {
		User user = null;
		PreparedStatement ps = con
				.getPreparedStatement("SELECT * FROM users WHERE userType = 'AREA_MANAGER' AND area = ?");
		try {
			ps.setString(1, area);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				user = new User(UserType.AREA_MANAGER, rs.getString(2), rs.getString(3), rs.getString(4),
						rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), area);
				Customer customerInfo = fetchCustomerInfo(user);
				user.setCustomerInfo(customerInfo);
			}
			return user;
		} catch (SQLException e1) {
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
	 * 
	 * Fetches all users with a certain role from the users table.
	 * 
	 * @param userType The role of the users to be fetched.
	 * @return A list of all users with the specified role. Returns null if no users
	 *         are found.
	 */
	public ArrayList<User> fetchAllUsersByRole(UserType userType) {
		if (userType==null)
			return null;
		ArrayList<User> usersList = new ArrayList<>();
		User user = null;
		PreparedStatement ps = con.getPreparedStatement("SELECT * FROM users WHERE userType = ?");
		try {
			ps.setString(1, userType.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				user = new User(userType, rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
						rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9));
				Customer customerInfo = fetchCustomerInfo(user);
				user.setCustomerInfo(customerInfo);
				usersList.add(user);
			}

			return usersList.size() != 0 ? usersList : null;
		} catch (SQLException e1) {
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
	 * Updates an existing user in the database.
	 * @param user the user whose should be update
	 * @return `true` if the user was successfully updated, `false` if an error
	 *         occurred. UserType userType, String username, String password, String
	 *         firstName, String lastName, String id, String email, String
	 *         phoneNumber, String area
	 */
	public boolean updateUser(User user) {
		if(user==null)
			return false;
		
		String query = "UPDATE users SET userType = ?, firstName = ?, lastName = ?, id = ? , email = ?, phoneNumber = ?, area= ? WHERE username = ?";
		PreparedStatement ps = con.getPreparedStatement(query);
		try {
			ps.setString(1, user.getUserType().toString());
			ps.setString(2, user.getFirstName());
			ps.setString(3, user.getLastName());
			ps.setString(4, user.getId());
			ps.setString(5, user.getEmail());
			ps.setString(6, user.getPhoneNumber());
			ps.setString(7, user.getArea());
			ps.setString(8, user.getUsername());
			return 1 == ps.executeUpdate();
		} catch (SQLException e1) {
			return false;
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 
	 * Returns a list of all UserRegistration objects for a given area.
	 * 
	 * @param area The area to search for UserRegistration objects in.
	 * @return An ArrayList of UserRegistration objects for the given area. Returns
	 *         null if no UserRegistration objects are found.
	 */
	public ArrayList<UserRegistration> getUserRegistrationList(String area) {
		ArrayList<UserRegistration> usersRegistrationList = new ArrayList<>();
		UserRegistration userRegistration = null;
		PreparedStatement ps = con.getPreparedStatement("SELECT * FROM user_registration WHERE area= ?");
		try {
			ps.setString(1, area);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				// String userName, String creditCardNumber,String phoneNumber,String email,
				// boolean monthlyCharge, String customerOrSub
				userRegistration = new UserRegistration(rs.getString(1), rs.getString(2), rs.getString(3),
						rs.getString(4), rs.getBoolean(5), rs.getBoolean(6) ? "customer" : "subscriber", area);
				usersRegistrationList.add(userRegistration);
			}
			return usersRegistrationList.size() != 0 ? usersRegistrationList : null;
		} catch (SQLException e1) {
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
	 * 
	 * Creates or updates a customer in the customers table.
	 * 
	 * @param customer The customer to create or update.
	 * @return True if the customer was successfully created or updated. False
	 *         otherwise.
	 */
	public boolean createOrUpdateCustomer(Customer customer) {
		String query = "INSERT INTO customers"
				+ " VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE subscriberNumber = ?, monthlyCharge = ?";
		PreparedStatement ps = con.getPreparedStatement(query);
		try {
			ps.setString(1, customer.getUsername());
			ps.setInt(2, customer.getSubscriberNumber());
			ps.setBoolean(3, customer.isMonthlyCharge());
			ps.setString(4, customer.getCreditCard());
			ps.setBoolean(5, false); // When we create a new customer he still not placed order as a subscriber
			ps.setInt(6, customer.getSubscriberNumber());
			ps.setBoolean(7, customer.isMonthlyCharge());
			return 1 >= ps.executeUpdate();
		} catch (SQLException e1) {
			return false;
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 
	 * Creates a new user registration in the user_registration table.
	 * 
	 * @param user The UserRegistration object to be created.
	 * @return True if the UserRegistration was successfully created. False
	 *         otherwise.
	 */
	public boolean createUserToRegister(UserRegistration user) {
		String query = "INSERT INTO user_registration VALUES(?,?,?,?,?,?,?)";
		PreparedStatement ps = con.getPreparedStatement(query);
		try {
			ps.setString(1, user.getUsername());
			ps.setString(2, user.getCreditCardNumber());
			ps.setString(3, user.getPhoneNumber());
			ps.setString(4, user.getEmail());
			ps.setBoolean(5, user.getMonthlyCharge());
			ps.setBoolean(6, user.getCustomerOrSub().equals("customer"));
			ps.setString(7, user.getArea());
			return 1 >= ps.executeUpdate();
		} catch (SQLException e1) {
			return false;
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 
	 * Deletes a user registration from the user_registration table.
	 * 
	 * @param username The username of the UserRegistration to be deleted.
	 * @return True if the UserRegistration was successfully deleted. False
	 *         otherwise.
	 */
	public boolean deleteUserFromRegistration(String username) {
		PreparedStatement ps = con.getPreparedStatement("DELETE FROM user_registration WHERE userName = ?;");
		try {
			ps.setString(1, username);
			return (ps.executeUpdate() == 1);
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
