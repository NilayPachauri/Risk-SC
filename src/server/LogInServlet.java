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
import javax.servlet.http.HttpSession;

@WebServlet("/LogInServlet")
public class LogInServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public LogInServlet() {
        super();
    }
    
    // Log-in the user into the game and server
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String username = request.getParameter("username");
    	String password = request.getParameter("password");
    	
    	/*
    	 * Message used to communicate with client about log-in
    	 * 
    	 * Possibilities:
    	 * 	Message = 0: User now logged in
    	 * 	Message = 1: Incorrect password
    	 * 	Message = 2: User does not exist
    	 */
    	int message = 0;
    	
    	synchronized(this) {
    		try {
        		Class.forName("com.mysql.jdbc.Driver");
        		Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/final?user=root&password=root&allowPublicKeyRetrieval=true&useSSL=false");
        		
        		if(this.userExists(username, conn)) {
        			PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = '" + username + "'");
        			ResultSet rs = ps.executeQuery();
        			
        			while(rs.next()) {
        				// Message = 0 means user is now logged in
        				if(rs.getString("password").equals(password)) {
        					message = 0;
        					
        					HttpSession session = request.getSession(true);
        					session.setAttribute("username", rs.getString("username"));
        					
        				}
        				// Message = 1 means incorrect password
        				else {
        					message = 1;
        				}
        			}
        		}
        		// Message = 2 means user does not exist in database
        		else {
        			message = 2;
        		}
        		
        		conn.close();
        	}
        	catch (SQLException sqle) {
        		System.out.println(sqle.getMessage());
        	} 
        	catch (ClassNotFoundException cnfe) {
        		System.out.println(cnfe.getMessage());
    		}
    		
    		request.setAttribute("message", message);
    		request.getRequestDispatcher("LogIn.jsp").forward(request, response);
    	}
    }
    
    // Checks whether or not the user exists in the database
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
}
