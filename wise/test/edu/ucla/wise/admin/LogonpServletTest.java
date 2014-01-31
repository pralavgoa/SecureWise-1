package edu.ucla.wise.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeast;

import org.junit.Test;

public class LogonpServletTest {

	@Test
	public void testLogonpServlet() throws ServletException, IOException{
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);  
		
		when(request.getParameter("username")).thenReturn("me");
		//skip the password to assert that it fails
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		when(response.getWriter()).thenReturn(printWriter);
		
		new LogonpServlet().service(request, response);
		
		verify(request, atLeast(1)).getParameter("username");
		printWriter.flush();
		assertTrue(stringWriter.toString().contains("Please provide a valid password"));
	}

}
