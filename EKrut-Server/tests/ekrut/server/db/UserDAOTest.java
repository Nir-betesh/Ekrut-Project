package ekrut.server.db;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ekrut.entity.User;
import ekrut.entity.UserType;

class UserDAOTest {
	UserDAO userDAO;
	DBController dbController;
	User user1;
	User userResult;

	@BeforeEach
	void setUp() throws Exception {
		dbController = new DBController("jdbc:mysql://localhost/test_ekrut?serverTimezone=IST", "root", "1qazZ2wsxX!@");
		dbController.connect();
		userDAO = new UserDAO(dbController);
		user1 = new User(UserType.CEO, "ceo", "123", "israel", "lobisvili", "1", "israel@ek.com", "05050050001", null);
	}

	// Checking functionality fetchUserByUsername: fetch user by exists username
	// Input parameters: "ofek"
	// Expected result: User with user name: "ofek"
	@Test
	void test_FetchUserByUsername_UserNameExists_ResUser() {
		userResult = userDAO.fetchUserByUsername("ceo");
		assertEquals(userResult.getUserType(), UserType.CEO, "Incorrect userType");
		assertEquals(userResult.getPassword(), "123", "Incorrect password");
		assertEquals(userResult.getFirstName(), "israel", "Incorrect first-name");
		assertEquals(userResult.getLastName(), "lobisvili", "Incorrect last-name");
		assertEquals(userResult.getId(), "1", "Incorrect ID");
		assertEquals(userResult.getEmail(), "israel@ek.com", "Incorrect email");
		assertEquals(userResult.getPhoneNumber(), "05050050001", "Incorrect phone number");
		assertEquals(userResult.getArea(), null, "Incorrect area");
	}

	// Checking functionality fetchUserByUsername: fetch user by not exists username
	// Input parameters: "dana"
	// Expected result: null
	@Test
	void test_FetchUserByUsername_UserNameNotExist_ResNull() {
		assertNull(userDAO.fetchUserByUsername("dana"));
	}

	// Checking functionality fetchUserByUsername: fetch user by null username
	// Input parameters: null
	// Expected result: null
	@Test
	void test_FetchUserByUsername_NullUser_ResNull() {
		assertNull(userDAO.fetchUserByUsername(null));
	}

}
