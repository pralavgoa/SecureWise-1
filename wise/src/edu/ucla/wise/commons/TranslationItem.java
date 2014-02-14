package edu.ucla.wise.commons;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class contains a translation item and all its possible properties.
 * 
 * @author Douglas Bell
 * @version 1.0
 */
public class TranslationItem {
    private static final Logger LOGGER = Logger
	    .getLogger(TranslationItem.class);
    /** Instance Variables */
    public String id;
    public String name;
    public String charset;
    public String selector;

    public String text;
    public String stem;

    public ArrayList<String> responses;
    public ArrayList<String> values;

    public String[] stems;
    public String[] stemNames;
    public ArrayList<String[]> subjects;

    public Survey survey;

    /**
     * constructor: parse a translation item node from the XML
     * 
     * @param n
     *            Node from XML where the TranslationItem is populated.
     * @param s
     *            Survey whose xml is parsed.
     * 
     */
    public TranslationItem(Node n, Survey s) {
	try {
	    this.survey = s;

	    /* parse the translation node and assign its properties */
	    this.id = n.getAttributes().getNamedItem("ID").getNodeValue();
	    this.name = n.getAttributes().getNamedItem("name").getNodeValue();

	    /* charset is the encoding type */
	    this.charset = n.getAttributes().getNamedItem("charset")
		    .getNodeValue();

	    Node node1 = n.getAttributes().getNamedItem("selector");
	    if (node1 != null) {
		this.selector = node1.getNodeValue();
	    } else {
		this.selector = "0";
	    }

	    NodeList nodelist = n.getChildNodes();
	    this.responses = new ArrayList<String>();
	    this.values = new ArrayList<String>();

	    /* get the size of the subject stem array */
	    int numStems = 0;
	    for (int i = 0; i < nodelist.getLength(); i++) {
		if (nodelist.item(i).getNodeName().equalsIgnoreCase("Sub_Stem")) {
		    numStems++;
		}
	    }

	    /* declare the stem array */
	    this.stems = new String[numStems];
	    this.stemNames = new String[numStems];
	    int idNum = 1;

	    for (int i = 0, j = 0; i < nodelist.getLength(); i++) {
		Node nc = nodelist.item(i);

		/* parse the translated directive */
		if (nc.getNodeName().equalsIgnoreCase("Directive")) {
		    this.stem = nc.getFirstChild().getNodeValue();
		}

		/*
		 * parse the stem - translated version (open question/closed
		 * question/question block)
		 */
		if (nc.getNodeName().equalsIgnoreCase("Stem")) {
		    this.stem = nc.getFirstChild().getNodeValue();
		    // Study_Util.email_alert("get the translated stem: " +
		    // stem);
		    /*
		     * Transformer transformer =
		     * TransformerFactory.newInstance().newTransformer();
		     * StringWriter sw = new StringWriter();
		     * transformer.transform(new DOMSource(nc), new
		     * StreamResult(sw)); stem = sw.toString();
		     * Study_Util.email_alert("get the translated stem: " +
		     * stem);
		     */
		}

		/*
		 * parse the translated response option (closed
		 * question/question block)
		 */
		if (nc.getNodeName().equalsIgnoreCase("Response_Option")) {
		    String str = nc.getFirstChild().getNodeValue();
		    this.responses.add(str);
		    Node node2 = nodelist.item(i).getAttributes()
			    .getNamedItem("value");
		    if (node2 != null) {
			this.values.add(node2.getNodeValue());
		    } else {
			this.values.add("-1");
		    }
		}

		/* parse the translated sub stem (question block) */
		if (nc.getNodeName().equalsIgnoreCase("Sub_Stem")) {
		    this.stems[j] = nc.getFirstChild().getNodeValue();
		    this.stemNames[j] = this.name + "_" + (j + 1);
		    j++;
		}

		/* parse the translated sub set reference (question block) */
		if (nc.getNodeName().equalsIgnoreCase("Subject")) {
		    String[] str = new String[2];
		    if (nc.getAttributes().getNamedItem("IDnum") != null) {
			str[0] = nc.getAttributes().getNamedItem("IDnum")
				.getNodeValue();
		    } else {
			str[0] = Integer.toString(idNum++);
		    }
		    str[1] = nc.getFirstChild().getNodeValue();
		    this.subjects.add(str);
		}
	    }
	} catch (DOMException e) {
	    LOGGER.error(
		    "WISE - Translation Item: ID = " + this.id + "; Survey = "
			    + s.getId() + "; Study = " + s.getStudySpace().id
			    + " --> " + e.toString(), null);
	    return;
	}
    }
}
