package edu.ucla.wise.commons;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class contains a thank you page object set in the preface.
 * 
 * @author Douglas Bell
 * @version 1.0
 */
public class ThankyouPage {
    private static final Logger LOGGER = Logger.getLogger(ThankyouPage.class);

    /** Instance Variables */
    public String id;
    public String title;
    public String banner;
    public String logo;
    public String surveyId;
    // public String irb_id;
    public String pageContents;

    public Preface preface;

    /**
     * Constructor: parse a thank you node from XML
     * 
     * @param n
     *            Node from the XML that has to be parsed to get the information
     *            about Thank you page.
     * @param p
     *            Preface object to which this ThankYou is linked with.
     */
    public ThankyouPage(Node n, Preface p) {
	try {
	    this.preface = p;
	    // assign id & survey id (required) no ID
	    // id = n.getAttributes().getNamedItem("ID").getNodeValue();
	    // survey_id =
	    // n.getAttributes().getNamedItem("Survey_ID").getNodeValue();

	    // assign various attributes
	    // Node node_child = n.getAttributes().getNamedItem("Title");
	    // if(node_child !=null)
	    // title = node_child.getNodeValue();
	    // else
	    // title = "";
	    this.title = "Thank You";
	    Node nodeChild = n.getAttributes().getNamedItem("BannerFileName");
	    if (nodeChild != null) {
		this.banner = nodeChild.getNodeValue();
	    } else {
		this.banner = "title.gif";
	    }
	    nodeChild = n.getAttributes().getNamedItem("LogoFileName");
	    if (nodeChild != null) {
		this.logo = nodeChild.getNodeValue();
	    } else {
		this.logo = "proj_logo.gif";
	    }
	    // node_child = n.getAttributes().getNamedItem("IRB_ID");
	    // if(node_child !=null)
	    // irb_id = node_child.getNodeValue();
	    // else
	    // irb_id = "";

	    NodeList nodeP = n.getChildNodes();
	    this.pageContents = "";
	    for (int j = 0; j < nodeP.getLength(); j++) {
		if (nodeP.item(j).getNodeName().equalsIgnoreCase("p")) {
		    this.pageContents += "<p>"
			    + nodeP.item(j).getFirstChild().getNodeValue()
			    + "</p>";
		}
		if (nodeP.item(j).getNodeName()
			.equalsIgnoreCase("html_content")) {
		    NodeList nodeN = nodeP.item(j).getChildNodes();
		    for (int k = 0; k < nodeN.getLength(); k++) {
			if (nodeN.item(k).getNodeName()
				.equalsIgnoreCase("#cdata-section")) {
			    this.pageContents += nodeN.item(k).getNodeValue();
			}
		    }
		}
	    }
	} catch (DOMException e) {
	    // WISE_Application.email_alert("WISE - THANKYOU PAGE : Preface = "+p.project_name+"; Study = "+p.study_space.id+" --> "+e.toString());
	    LOGGER.error("WISE - THANKYOU PAGE : Preface = " + p.projectName
		    + "--> " + e.toString(), e);
	    return;
	}
    }
}