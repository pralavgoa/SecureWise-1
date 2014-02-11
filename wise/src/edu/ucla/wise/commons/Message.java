package edu.ucla.wise.commons;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class represents our email prompts
 * 
 * @author Douglas Bell
 * @version 1.0  
 */
public class Message {
	
    /** Email Image File Names */
    private static final String headerImgFilename = "email_header_img.jpg";
    private static final String footerImgFilename1 = "email_bottom_img1.jpg";
    private static final String footerImgFilename2 = "email_bottom_img2.jpg";
    private static final String WISE_SHARED = "WiseShared";
    private static final String htmlOpen = "<html><head><meta http-equiv='Content-Type' content='text/html; charset=iso-8859-1'></head>"
	    + "<body bgcolor=#FFFFFF text=#000000><center>";
    private static final String htmlClose = "</center></body></html>";

    /** Instance Variables */
    public String id, subject;
    public String mainBody, htmlBody, htmlHeader, htmlTail, htmlSignature;
    private boolean htmlFormat = false;
    public boolean hasLink = false, hasDLink = false;
    public String msgRef = null;
    private String servletPath = null;

    // TODO: should ideally be customizable for survey/irb; should use xlst

    /**
     * Constructor for Message
     * 
     * @param n	XML DOM for the message from the preface file.
     */
    public Message(Node n) {
    	try {
    		
    		/* get the message sequence */
    		// message_sequence = msg_seq;
    		
    		/* get the message type */
    		// type = message_type;
    		
    		/* parse out the reminder attributes: ID, subject, format, trigger days and max count */
    		id = "";
    		subject = "";
    		
    		/* ID, subject, trigger days and maximum count are required */
    		id = n.getAttributes().getNamedItem("ID").getNodeValue();
    		subject = n.getAttributes().getNamedItem("Subject").getNodeValue();

    		/* email format is optional */
    		Node node = n.getAttributes().getNamedItem("Format");
    		if (node != null) {
    			htmlFormat = node.getNodeValue().equalsIgnoreCase("html");
    		}
    		// System.out.println("ID - "+id);
    		
    		/* read out the contents of the email message */
    		NodeList nodeP = n.getChildNodes();
    		boolean hasRef = false;
    		mainBody = "";
    		htmlHeader = "";
    		htmlBody = "";
    		htmlTail = "";
    		htmlSignature = "";

    		for (int j = 0; j < nodeP.getLength(); j++) {
    			if (nodeP.item(j).getNodeName()
    					.equalsIgnoreCase("Message_Ref")
    					&& !hasRef) {
    				
    				/* check prevents a 2nd ref (that's invalid) */
    				msgRef = nodeP.item(j).getAttributes().getNamedItem("ID")
    						.getNodeValue();
    				hasRef = true;
    				break;
    			} else {
    				if (nodeP.item(j).getNodeName().equalsIgnoreCase("p")) {
    					mainBody += nodeP.item(j).getFirstChild()
    							.getNodeValue()
    							+ "\n\n";
    					htmlBody += "<p>"
    							+ nodeP.item(j).getFirstChild().getNodeValue()
    							+ "</p>\n";
    				}
    				if (nodeP.item(j).getNodeName().equalsIgnoreCase("s")) {
    					if (htmlFormat)
    						htmlSignature += "<p>"
    								+ nodeP.item(j).getFirstChild()
    								.getNodeValue() + "</p>\n";
    					else {
    						mainBody += nodeP.item(j).getFirstChild()
    								.getNodeValue()
    								+ "\n\n";
    						htmlBody += "<p>"
    								+ nodeP.item(j).getFirstChild()
    								.getNodeValue() + "</p>\n";
    					}
    				}
    				
    				/* mark the URL link */
    				if (nodeP.item(j).getNodeName().equalsIgnoreCase("link")) {
    					hasLink = true;
    					mainBody = mainBody + "URL LINK\n\n";
    					htmlBody += "<p align=center><font color='blue'>[<u>URL Link to Start the Survey</u>]</font></p>\n";
    				}
    				
    				/* mark the decline URL link */
    				if (nodeP.item(j).getNodeName()
    						.equalsIgnoreCase("declineLink")) {
    					hasDLink = true;
    					mainBody = mainBody + "DECLINE LINK\n\n";
    					AdminApplication.logInfo("########The Body of the email is: "+ mainBody);
    					htmlBody += "<p align=center><font color='blue'>[<u>URL Link to Decline the Survey</u>]</font></p>\n";
    				}
    			}
    		}
    	} catch (DOMException e) {
    		AdminApplication.logError("WISE - TYPE MESSAGE: ID = " + id
    				+ "; Subject = " + subject + " --> " + e.toString(), e);
    		return;
    	}
    }

    /**
     * Resolve message references. Do this after construct-time so that order of
     * messages in file won't matter.
     * 
     * @param myPreface		preface object on which the resolve is performed.
     */
    public void resolveRef(Preface myPreface) {
    	if (msgRef != null) {
    		Message refdMsg = myPreface.getMessage(msgRef);
    		if (refdMsg.msgRef == null) {
    			mainBody = refdMsg.mainBody;
    			hasLink = refdMsg.hasLink;
    			hasDLink = refdMsg.hasDLink;
    			htmlTail = refdMsg.htmlTail;
    			htmlHeader = refdMsg.htmlHeader;
    		} else
    			WISEApplication
    			.logError(
    					"MESSAGE: ID = "
    							+ id
    							+ "; Subject = "
    							+ subject
    							+ " refernces a message that itself has a message ref. Double-indirection not supported. ",
    							null);
    	}
    }

    /**
     * Sets the HTML header and footer of the email.
     * 
     * @param servletPath	Servlet path to be set.
     * @param imgRootPath	image root path for Email
     */
    public void setHrefs(String servletPath, String imgRootPath) {
    	this.servletPath = servletPath;
    	if (htmlFormat) {
    		/* compose the html header and tail */
    		htmlHeader = "<table width=510 border=0 cellpadding=0 cellspacing=0>"
    				+ "<tr><td rowspan=5 width=1 bgcolor='#996600'></td>"
    				+ "<td width=500 height=1 bgcolor='#996600'></td>"
    				+ "<td rowspan=5 width=1 bgcolor='#996600'></td></tr>"
    				+ "<tr><td height=120 align=center><img src='"
    				+ WISEApplication.rootURL
    				+ "/"
    				+ WISE_SHARED
    				+ "/image?img="
    				+ headerImgFilename
    				+ "'></td></tr>"
    				+ "<tr><td>"
    				+ "<table width=100% border=0 cellpadding=0 cellspacing=0>"
    				+ "<tr><td width=20>&nbsp;</td>"
    				+ "<td width=460><font size=1 face='Verdana'>\n\n";

    		/* NOTE: signature included in the tail */
    		htmlTail = "</font></td><td width=20>&nbsp;</td>"
    				+ "</tr></table></td></tr>" + "<tr><td>"
    				+ "<table width=100% border=0 cellpadding=0 cellspacing=0>"
    				+ "<tr><td rowspan=2 width=25>&nbsp;</td>"
    				+ "<td height=80 width=370><font size=1 face='Verdana'>"
    				+ htmlSignature
    				+ "</font></td>"
    				+ "<td rowspan=2 height=110 width=105 align=left valign=bottom><img src=\""
    				+ WISEApplication.rootURL
    				+ "/"
    				+ WISE_SHARED
    				+ "/image?img="
    				+ footerImgFilename2
    				+ "\"></td></tr>"
    				+ "<tr><td height=30 width=370 align=center valign=bottom><img src='"
    				+ WISEApplication.rootURL
    				+ "/"
    				+ WISE_SHARED
    				+ "/image?img="
    				+ footerImgFilename1
    				+ "'></td></tr>"
    				+ "</table></td></tr>"
    				+ "<tr><td width=500 height=1 bgcolor='#996600'></td></tr></table>\n\n";
    	}
    }

    /**
     * Composes the body of email in text format.
     * 
     * @param 	salutation	Salutation to address the invitee to whom email has to be sent.
     * @param 	lastname	Last name of the invitee.
     * @param 	ssid		Studyspace id for generation of the URL.
     * @param 	msgIndex	Id to identify the invitee.
     * @return	String		Body of the email in text format.		
     */
    public String composeTextBody(String salutation, String lastname,
    		String ssid, String msgIndex) {
    	String textBody = null;
    	
    	/* compose the text body */
    	textBody = "Dear " + salutation + " " + lastname + ":\n\n" + mainBody;

    	if (hasLink) {
    		String reminderLink = servletPath + "survey?msg="
    				+ msgIndex
    				+ "&t="
    				+ WISEApplication.encode(ssid);

    		String declineLink = servletPath + "survey/declineNOW?m="
    				+ msgIndex + "&t="
    				+ WISEApplication.encode(ssid);

    		textBody = textBody.replaceAll("URL LINK", reminderLink + "\n");
    		if (hasDLink) {
    			textBody = textBody.replaceAll("DECLINE LINK", declineLink
    					+ "\n");
    		}
    	}
    	return textBody;
    }

    /**
     * The URL for anonymous user to use.
     * 
     * @param 	servletPath		Root path of the URL being generated.
     * @param 	msgIndex		Index to identify a particular user.
     * @param 	studySpaceId	Study space id for which link has to be generated.
     * @param 	surveyId		Survey in the study space for which URL is generated.
     * @return	String			URL for accessing the survey by anonymous users.
     */
    public static String buildInviteUrl(String servletPath, String msgIndex,
    		String studySpaceId, String surveyId) {

    	/* t = xxxx -> study space
    	 * m = yyyy -> survey_user_message_space
    	 * s = zzzz -> survey id
    	 * for anonymous user we do not have survey message user and it is not
    	 * possible to get survey from study space while knowing the survey for
    	 * annonymous users because study space can have multiple surveys.
    	 */
    	if (msgIndex == null) {
    		return servletPath + "survey?t="
    		+ WISEApplication.encode(studySpaceId) + "&s="
    		+ CommonUtils.base64Encode(surveyId);
    	}
    	return servletPath + "survey?msg=" + msgIndex
    			+ "&t=" + WISEApplication.encode(surveyId);
    }

    /**
     * Composes the body of email in text format.
     * 
     * @param 	salutation	Salutation to address the invitee to whom email has to be sent.
     * @param 	lastname	Last name of the invitee.
     * @param 	ssid		Studyspace id for generation of the URL.
     * @param 	msgIndex	Id to identify the invitee.
     * @return	String		Body of the email in text format.		
     */
    public String composeHtmlBody(String salutation, String lastname,
    		String ssid, String msgIndex) {
    	
    	if (!htmlFormat) {
    		
    		/* null return is the external signal the message
        	 * doesn't have an HTML version
        	 */
    		return null; 
    	}
    	
    	/* this overrides the iVar TODO: FIX so that we can actually use it here
    	 * and for overview display
    	 */
    	
    	String htmlBody = null;
    	
    	/* add the html header & the top of the body to the html body */
    	htmlBody = htmlOpen + htmlHeader;
    	htmlBody += "<p><b>Dear " + salutation + " " + lastname + ":</b></p>"
    			+ mainBody;
    	htmlBody = htmlBody.replaceAll("\n", "<br>");
    	if (hasLink) {
    		String reminderLink = servletPath + "survey?msg="
    				+ msgIndex
    				+ "&t="
    				+ WISEApplication.encode(ssid);
    		String declineLink = servletPath + "declineNOW?m="
    				+ msgIndex
    				+ "&t="
    				+ WISEApplication.encode(ssid);
    		htmlBody = htmlBody.replaceAll("URL LINK",
    				"<p align=center><a href='" + reminderLink + "'>"
    						+ reminderLink + "</a></p>");
    		if (hasDLink)
    			htmlBody = htmlBody.replaceAll("DECLINE LINK",
    					"<p align=center><a href='" + declineLink + "'>"
    							+ declineLink + "</a></p>");
    	}
    	
    	/* append the bottom part of body for the html email */
    	htmlBody += htmlTail + htmlClose;
    	return htmlBody;
    }

    /**
     * Renders html table rows to complete sample display page (used by Admin)
     * 
     * @return	String	HTML format of the mail.
     */
    public String renderSampleAsHtmlRows() {
    	String outputString = "<tr><td class=sfon>Subject: </td>";
    	outputString += "<td>" + subject + "</td></tr>";
    	outputString += "<tr><td colspan=2>";

    	/* add the the bottom imag & signature to the html body */
    	if (htmlFormat) {
    		outputString += htmlHeader;
    		outputString += "<p>Dear [Salutation] [Name]:</p>";
    		outputString += htmlBody;
    		outputString += htmlTail; // note: includes signature
    	} else {
    		outputString += "<table width=100% border=0 cellpadding=0 cellspacing=0>";
    		outputString += "<tr><td>&nbsp;</td></tr>";
    		outputString += "<tr><td colspan=2><font size=1 face='Verdana'><p>Dear [Salutation] [Name],</p>\n";
    		outputString += mainBody
    				+ "<p>&nbsp;</p></font></td></tr></table>\n\n";
    	}
    	outputString += "</td></tr>";
    	return outputString;
    }

    @Override
    public String toString() {
	return "<P><B>Message</b> ID: " + id + "<br>\n" + "References: "
		+ msgRef + "<br>\n" + "Subject: " + subject + "<br>\n"
		+ "Body: " + mainBody + "</p>\n";
    }
    /*
     * DEPRECATED public String irb() { return message_sequence.irb_id; } public
     * String survey() { return message_sequence.survey_id; }
     */
}