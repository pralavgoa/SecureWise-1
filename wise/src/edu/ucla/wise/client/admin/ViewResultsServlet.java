package edu.ucla.wise.client.admin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import edu.ucla.wise.commons.Page;
import edu.ucla.wise.commons.SanityCheck;
import edu.ucla.wise.commons.StudySpace;
import edu.ucla.wise.commons.Survey;
import edu.ucla.wise.commons.WiseConstants;

/**
 * ViewResultsServlet class used to view the survey results 
 * (with the summary of data) by page (viewed by admin on behalf of Admin Application).
 * URL: /survey/admin_view_results.
 * 
 * @author Douglas Bell
 * @version 1.0  
 */
@WebServlet("/survey/admin_view_results")
public class ViewResultsServlet extends HttpServlet {
    static final long serialVersionUID = 1000;

    /**
     * Sets the session parameters for the next page to be get 
     * the results and also prints the results for current page.
     *  
     * @param 	req	 HTTP Request.
     * @param 	res	 HTTP Response.
     * @throws 	ServletException and IOException. 
     */
    @Override
    public void service(HttpServletRequest req, HttpServletResponse res)
    		throws ServletException, IOException {
		
    	/* prepare for writing */
		res.setContentType("text/html");
		PrintWriter out = res.getWriter();
		String path = req.getContextPath();
		
		HttpSession session = req.getSession(true);
		String studyId, surveyId;
	
		/* check if it is the first link */
		String a = req.getParameter("a");
	    if (SanityCheck.sanityCheck(a)) {
	    	path = req.getContextPath() + "/" + WiseConstants.ADMIN_APP;
			res.sendRedirect(path + "/sanity_error.html");
		    return;
	    }	    
	    a=SanityCheck.onlyAlphaNumeric(a);
				
		
		/* create session info from the first URL link */
		if (a != null && a.equalsIgnoreCase("FIRSTPAGE")) {
		    
			/* get the study id */
		    studyId = (String) req.getParameter("SID");
		    
		    /* get the survey id */
		    surveyId = (String) req.getParameter("s");
		    
		    if (SanityCheck.sanityCheck(studyId)
		    		|| SanityCheck.sanityCheck(surveyId)) {
		    	res.sendRedirect(path + "/admin/sanity_error.html");
				return;
			}
		    studyId=SanityCheck.onlyAlphaNumeric(studyId);
		    surveyId=SanityCheck.onlyAlphaNumeric(surveyId);
	
		    /* get the current study space */
		    StudySpace ss = StudySpace.getSpace(studyId);
		    
		    /* save the study space in the session */
		    session.setAttribute("STUDYSPACE", ss);
		    
		    /* get the current survey */
		    Survey sy = ss.getSurvey(surveyId);
		    
		    /* save the survey in the session */
		    session.setAttribute("SURVEY", sy);
	
		    /* set the first page id */
		    String pageId = sy.getPages()[0].id;
		    
		    /* set the page id in the session as the current page id */
		    session.setAttribute("PAGEID", pageId);
	
		    /* get the user or the user group whose results will be presented */
		    String whereStr = req.getParameter("whereclause");
		    if(SanityCheck.sanityCheck(whereStr)){
		    	res.sendRedirect(path + "/admin/sanity_error.html");
				return;
			}
		    if (whereStr == null) {
		    	whereStr = "";
		    }			
		    if (whereStr.equals("")) {
		    	
				/* check if specific users selected */
				String allUser = req.getParameter("alluser");				
				if(SanityCheck.sanityCheck(allUser)){
			    	res.sendRedirect(path + "/admin/sanity_error.html");
					return;
				}
				if (allUser == null || allUser.equals("")
						|| allUser.equalsIgnoreCase("null")) {
				    String user = req.getParameter("user");
				    if (SanityCheck.sanityCheck(user)) {
				    	res.sendRedirect(path + "/admin/sanity_error.html");
						return;
					}
				    
				    /* get the specified user list */
				    if (user == null) {
						out.println("Please select at least one invitee");
						return;
				    }
				    whereStr = "invitee in (" + user + ")";
				}
		    }
	
		    /* update whereclause in a session */
		    session.removeAttribute("whereStr");
		    session.setAttribute("WHERECLAUSE", whereStr);
	
		    /* call itself to display the page */
		    res.sendRedirect("admin_view_results");
		} else {
			
		    /* get the survey from the session */
		    Survey survey = (Survey) session.getAttribute("SURVEY");
		    
		    /* get the page id from the session */
		    String pageId = (String) session.getAttribute("PAGEID");
		    
		    /* get the where string from the session */
		    String whereClause = (String) session.getAttribute("WHERECLAUSE");
	
		    if (survey == null || whereClause == null || pageId == null) {
				out.println("<p>ADMIN VIEW RESULTS Error: " +
						"can't get the study where string/survey/page info.</p>");
				return;
		    }
	
		    /* get the current page */
		    Page pg = survey.getPage(pageId);
		    
		    /* update the page id to be the next page */
		    session.removeAttribute("PAGEID");
		    if (!survey.isLastPage(pageId)) {
		    	session.setAttribute("PAGEID", survey.nextPage(pageId).id);
		    }
			
		    /* display the result on current page */
		    out.println(pg.renderAdminResults(whereClause));
		}
		out.close();
    }
}