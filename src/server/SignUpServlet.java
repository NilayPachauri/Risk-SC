package server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import classes.Authentication;

@WebServlet("/SignUpServlet")
public class SignUpServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public SignUpServlet() {
        super();
    }

    // Service block to sign up the user based off the form constructed in the LogIn/SignUp JSP
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String first = request.getParameter("first");
    	String last = request.getParameter("last");
    	String username = request.getParameter("username");
    	String password = request.getParameter("password");
    	String confirm = request.getParameter("confirmPassword");
    	String image = request.getParameter("image");
    	
    	/*
    	 * Message used to communicate with client about log-in
    	 * 
    	 * Possibilities:
    	 * 	Message = 0: User signed up
    	 * 	Message = 1: Password is not at least eight characters long
    	 * 	Message = 2: Password does not have any characters
    	 * 	Message = 3: Password does not have any numbers
    	 * 	Message = 4: User already exists with username
    	 * 	Message = 5: Invalid Confirmation of password
    	 */
    	
    	String message = "";
    	
    	System.out.println(first + " " + last + " " + username + " " + password + " " + confirm + " " + image);
    	
    	synchronized(this) {
    		try {
        		Class.forName("com.mysql.jdbc.Driver");
        		Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/final?user=root&password=root&allowPublicKeyRetrieval=true&useSSL=false");
        		
        		if (first.isEmpty())
        			message += "Your first name cannot be empty\n";
        		
        		if (last.isEmpty())
        			message += "Your last name cannot be empty\n";
        		
        		if (username.isEmpty())
        			message += "Your username cannot be empty\n";
        		
        		if (!has8Characters(password))	{
        			message += "Your password must have at least 8 characters\n";
        		}
        		
        		if (!containsAlpha(password))	{
        			message += "Your password must have at least one alphabetical character\n";
        		}
        		
        		if (!containsDigit(password))	{
        			message += "Your password must have at least one digit\n";
        		}
        		
        		if (!password.equals(confirm))	{
        			message += "Your password doesn't match your confirm password\n";
        		}
        		
        		if (message.length() == 0)	{
        			message += "Signed Up Successfully!";
        			this.createUser(first, last, username, password, image, conn);
        		}
        		
        		conn.close();
        	}
        	catch (SQLException sqle) {
        		System.out.println("SQL Exception: " + sqle.getMessage());
        	} 
        	catch (ClassNotFoundException cnfe) {
    			System.out.println("Class Not Found Exception: " + cnfe.getMessage());
    		}
    		
    		System.out.println(message);
        	
        	// Sends message back to jQuery call
        	response.setContentType("text/html;charset=UTF-8");
        	response.getWriter().write(message);
    	}
    }
    
    // Checks whether or not a user exists already
    private boolean userExists(String username, Connection conn) {
    	try {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = '" + username + "'");
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				return true;
			}
		} 
		catch (SQLException sqle) {
			System.out.println(sqle.getMessage());
		}
    	
    	return false;
    }
    
    // Creates a user within the database
    private void createUser(String first, String last, String username, String password, String image, Connection conn) {
    	try {
    		PreparedStatement ps = conn.prepareStatement("INSERT INTO users (username, password, fname, lname, image) values (?, ?, ?, ?, ?)");
    		
    		String hash = Authentication.hashString(password);
    		
    		ps.setString(1, username);
    		ps.setString(2, hash);
    		ps.setString(3, first);
    		ps.setString(4, last);
    		ps.setString(5, image);
    		
    		ps.execute();
    	}
    	catch (SQLException sqle) {
    		System.out.println(sqle.getMessage());
    	}
    }
    
    private boolean containsAlpha(String s)	{
    	return !s.matches("[^a-zA-Z]+");
    }
    
    private boolean containsDigit(String s)	{
    	return !s.matches("[^0-9]+");
    }
    
    private boolean has8Characters(String s)	{
    	return (s.length() >= 8);
    }
}
