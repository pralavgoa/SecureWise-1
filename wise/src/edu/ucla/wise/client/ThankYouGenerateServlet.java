package edu.ucla.wise.client;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import edu.ucla.wise.commons.Preface;
import edu.ucla.wise.commons.StudySpace;
import edu.ucla.wise.commons.SurveyorApplication;
import edu.ucla.wise.commons.ThankyouPage;
import edu.ucla.wise.commons.WISEApplication;

/**
 * ThankYouGenerateServlet is used to handle the "Thank You" page.
 * 
 * @author Douglas Bell
 * @version 1.0
 */
@WebServlet("/survey/thank_you")
public class ThankYouGenerateServlet extends HttpServlet {
    public static final Logger LOGGER = Logger
	    .getLogger(ThankYouGenerateServlet.class);

    private static final long serialVersionUID = 1000;

    /**
     * Generates the thank you using the content from the xml file that is
     * uploaded for creation of the survey.
     * 
     * @param req
     *            HTTP Request.
     * @param res
     *            HTTP Response.
     * @throws ServletException
     *             and IOException.
     */
    @Override
    public void service(HttpServletRequest req, HttpServletResponse res)
	    throws ServletException, IOException {

	/* Prepare for writing */
	PrintWriter out;
	res.setContentType("text/html");
	out = res.getWriter();

	HttpSession session = req.getSession(true);

	/* if the session is new, then show the session expired info */
	if (session.isNew()) {
	    res.sendRedirect(SurveyorApplication.getInstance()
		    .getSharedFileUrl() + "error" + SurveyorApplication.htmlExt);
	    return;
	}

	/* get the study space from the session */
	StudySpace studySpace = (StudySpace) session.getAttribute("STUDYSPACE");
	if (studySpace == null) {
	    out.println("<p>Error: Can't find the study space.</p>");
	    return;
	}

	/* get the preface file which contains thank you element */
	String error = null;
	Preface pf = studySpace.get_preface();

	if (pf != null) {
	    ThankyouPage thankyouPage = pf.getThankyouPage();
	    if (thankyouPage == null) {
		error = "Error: Can't find a default Thank You Page in the Preface for current survey.";
		LOGGER.error("WISE - THANKYOU_GENERATE " + error, null);
		out.println("<p>" + error + "</p>");
		return;
	    }

	    String title, banner, logo, pageContents;
	    title = thankyouPage.title;
	    banner = thankyouPage.banner;
	    logo = thankyouPage.logo;
	    pageContents = thankyouPage.pageContents;

	    /* clear session */
	    session.invalidate();

	    /* print the Thank You page */
	    StringBuffer thankyouHtml = new StringBuffer("");

	    /* compose the common header */
	    thankyouHtml.append("<HTML><HEAD><TITLE>" + title
		    + " - Thanks</TITLE>");
	    thankyouHtml
		    .append("<META http-equiv=Content-Type content='text/html; charset=iso-8859-1'>");
	    thankyouHtml.append("<LINK href='" + "styleRender?app="
		    + studySpace.studyName + "&css=style.css"
		    + "' type=text/css rel=stylesheet>");
	    thankyouHtml
		    .append("<META content='MSHTML 6.00.2800.1170' name=GENERATOR></HEAD>");

	    /* compose the top part of the body */
	    thankyouHtml.append("<body><center>");
	    thankyouHtml
		    .append("<table width=100% cellspacing=1 cellpadding=9 border=0>");
	    thankyouHtml
		    .append("<tr><td width=98 align=center valign=top><img src='"
			    + WISEApplication.rootURL
			    + "/WiseShared"
			    + "/image?study="
			    + studySpace.studyName
			    + "&img="
			    + logo + "' border=0 align=middle></td>");
	    thankyouHtml
		    .append("<td width=695 align=center valign=middle><img src='"
			    + WISEApplication.rootURL
			    + "/WiseShared"
			    + "/image?study="
			    + studySpace.studyName
			    + "&img="
			    + banner + "' border=0 align=middle></td>");
	    thankyouHtml
		    .append("<td rowspan=6 align=center width=280>&nbsp;</td></tr>");
	    thankyouHtml.append("<tr><td width=98 rowspan=3>&nbsp;</td>");
	    thankyouHtml.append("<td class=head>THANK YOU</td></tr>");
	    thankyouHtml.append("<tr><td width=695 align=left colspan=1>");

	    /* get the welcome contents */
	    thankyouHtml.append(pageContents);
	    thankyouHtml.append("</td></tr>");
	    thankyouHtml.append("</table></center></body></html>");

	    /* print out the html form */
	    out.println(thankyouHtml);
	} else {
	    error = "Error: Can't get the preface";
	}
	if (error != null) {
	    LOGGER.error("WISE - THANKYOU GENERATE: " + error, null);
	    out.println("<p>" + error + "</p>");
	}
	out.close();
	return;
    }
}