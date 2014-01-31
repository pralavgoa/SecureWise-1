package edu.ucla.wise.commons;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.ucla.wise.commons.InviteeMetadata.Values;

/**
 * This class is a survey object and contains information about a specific
 * survey.
 * 
 * @author Douglas Bell
 * @version 1.0  
 */
public class Survey {

	/** Instance Variables */
	// public String file_loc;
	private final String id;
	private final String title;
	private final String projectName;
	private final String fromString;
	private final String fromEmail;
	private final String interruptMessage;
	private final String doneMessage;
	private final String reviewMessage;
	private String version = "0";
	private boolean allowGoback;
	private final int minCompleters;
	private final String forwardUrl;
	private final String eduModule;
	private final String logoName;
	private final String inviteeFields[];

	private final Hashtable<String, ResponseSet> responseSets;
	private final Hashtable<String, SubjectSet> subjectSets;
	private final Hashtable<String, TranslationItem> translationItems;

	private final InviteeMetadata inviteeMetadata;
	private final Page[] pages;
	private final StudySpace studySpace;
	private int totalItemCount;

	/**
	 * Constructor - setup a survey by parsing the file
	 * 
	 * @param xmlDoc	Survey Xml that contains all the questions.
	 * @param ss		Study space to which this survey belongs to.
	 */
	public Survey(Document xmlDoc, StudySpace ss) throws DOMException{
		int numbPages = 0;
		totalItemCount = 0;
		studySpace = ss;

		/* create a parser and an XML document */
		xmlDoc.getDocumentElement().normalize();

		/* parse out the data from survey xml file */
		NodeList nodelist = xmlDoc.getElementsByTagName("Survey");
		if(nodelist.getLength() <1 ){
			throw new IllegalStateException("The survey node is not present in the xml document provided");
		}
		Node node = nodelist.item(0);

		/* parse out the survey attributes survey ID & title */
		id = node.getAttributes().getNamedItem("ID").getNodeValue();
		title = node.getAttributes().getNamedItem("Title")
				.getNodeValue();

		// names who send the email
		Node node2 = node.getAttributes().getNamedItem("From_String");
		if (node2 != null) {
			fromString = node2.getNodeValue();
		} else {
			fromString = "";
		}

		/* FROM email address - fake one
		 * the actual FROM email will use the one in SMTP email account
		 */
		node2 = node.getAttributes().getNamedItem("From_Email");
		if (node2 != null) {
			fromEmail = node2.getNodeValue();
		} else {
			fromEmail = "";
		}

		/* survey version */
		node2 = node.getAttributes().getNamedItem("Version");
		if (node2 != null) {
			version = node2.getNodeValue();
		} else {
			version = "";
		}

		// get the name of user-defined data table
		// the default is the invitee table
		// node2 = node.getAttributes().getNamedItem("user_data_page");
		// if (node2 != null)
		// user_data_page = node2.getNodeValue();
		// else
		// user_data_page = "Invitee";

		/* messsage ID for survey-interruption message */
		node2 = node.getAttributes().getNamedItem("Interrupt_Msg");
		if (node2 != null) {
			interruptMessage = node2.getNodeValue();
		} else {
			interruptMessage = "";
		}

		/* messsage ID for survey-completion message */
		node2 = node.getAttributes().getNamedItem("Done_Msg");
		if (node2 != null) {
			doneMessage = node2.getNodeValue();
		} else {
			doneMessage = "";
		}

		/* messsage ID for survey-review message */
		node2 = node.getAttributes().getNamedItem("Review_Msg");
		if (node2 != null) {
			reviewMessage = node2.getNodeValue();
		} else {
			reviewMessage = "";
		}

		/* allow user to go back to review those survey pages have been past over */
		node2 = node.getAttributes().getNamedItem("Allow_Goback");
		if (node2 != null) {
			allowGoback = new Boolean(node2.getNodeValue())
			.booleanValue();
		} else {
			allowGoback = false;
		}

		/* user won't allow to review the survey results,
		 * until the number of completers reach up to this number.
		 */
		node2 = node.getAttributes()
				.getNamedItem("View_Result_After_N");
		if (node2 != null) {
			minCompleters = Integer.parseInt(node2.getNodeValue());
		} else {
			minCompleters = -1;
		}

		/* forwarding URL after the survey process
		 * normally it is the URL link to the quiz
		 */
		node2 = node.getAttributes().getNamedItem("Forward_On");
		if (node2 != null) {
			forwardUrl = node2.getNodeValue();
		} else {
			forwardUrl = "";
		}

		/* the module ID for the quiz process */
		node2 = node.getAttributes().getNamedItem("Edu_Module");
		if (node2 != null) {
			eduModule = node2.getNodeValue();
		} else {
			eduModule = "";
		}

		/* the logo image's name for the survey */
		node2 = node.getAttributes().getNamedItem("Logo_Name");
		if (node2 != null) {
			logoName = node2.getNodeValue();
		} else {
			logoName = "proj_logo.gif";
		}
		node2 = node.getAttributes().getNamedItem("Project_Name");
		if (node2 != null) {
			projectName = node2.getNodeValue();
		} else {
			projectName = "UNKOWN";
		}

		/* count the number of pages */
		NodeList nodelistChildren = node.getChildNodes();
		for (int j = 0; j < nodelistChildren.getLength(); j++) {
			if (nodelistChildren.item(j).getNodeName()
					.equalsIgnoreCase("Survey_Page"))
				numbPages++;
		}

		/* parse out the response sets */
		responseSets = new Hashtable<String, ResponseSet>();
		nodelist = xmlDoc.getElementsByTagName("Response_Set");
		for (int i = 0; i < nodelist.getLength(); i++) {
			Node childNode = nodelist.item(i);
			ResponseSet r = new ResponseSet(childNode, this);
			responseSets.put(r.id, r);
		}

		/* parse out the invitee fields */
		nodelist = xmlDoc.getElementsByTagName("Invitee_Fields");
		if(nodelist.getLength() < 1){
			throw new IllegalStateException("The invitee fields are not present in the xml provided");
		}
		Node inviteeNode = nodelist.item(0);
		inviteeMetadata = new InviteeMetadata(inviteeNode, this);
		if(getInviteeMetadata() == null){
			throw new IllegalStateException("Please provide the invitee metadata in the survey xml");
		}
		inviteeFields = new String[getInviteeMetadata().fieldMap.size()];
		int cnt = 0;
		for (Map.Entry<String, Values> map : getInviteeMetadata().fieldMap
				.entrySet()) {
			getInviteeFields()[cnt++] = map.getKey();
		}
		getStudySpace().db.syncInviteeTable(getInviteeMetadata());

		/* parse out the translation sets */
		translationItems = new Hashtable<String, TranslationItem>();
		// nodelist = doc.getElementsByTagName("Translation");
		nodelist = xmlDoc.getElementsByTagName("TranslationType");
		for (int i = 0; i < nodelist.getLength(); i++) {
			Node childNode = nodelist.item(i);
			TranslationItem t = new TranslationItem(childNode, this);
			translationItems.put(t.id, t);
		}

		/* parse out the subject sets */
		subjectSets = new Hashtable<String, SubjectSet>();
		nodelist = xmlDoc.getElementsByTagName("Subject_Set");
		for (int i = 0; i < nodelist.getLength(); i++) {
			Node n = nodelist.item(i);
			SubjectSet subject_set = new SubjectSet(n, this);
			subjectSets.put(subject_set.id, subject_set);
		}

		/* create the pages */
		pages = new Page[numbPages];
		nodelist = xmlDoc.getElementsByTagName("Survey");

		for (int i = 0; i < nodelist.getLength(); i++) {
			if (nodelist.item(i).getNodeName().equalsIgnoreCase("Survey")) {
				NodeList nodelist1 = nodelist.item(i).getChildNodes();
				for (int j = 0, k = 0; j < nodelist1.getLength(); j++) {
					if (nodelist1.item(j).getNodeName()
							.equalsIgnoreCase("Survey_Page")) {
						getPages()[k] = new Page(nodelist1.item(j), this);
						totalItemCount += getPages()[k].getItemCount();
						k++;
					}
				}
			}
		}
	}

	/*
	 * 		} catch (DOMException e) {
			WISEApplication.logError(
					"WISE - SURVEY parse error: " + e.toString() + "\n" + id
					+ "\n" + this.toString(), null);

			return;
		}
	 */

	/**
	 * Returns a database Connection object.
	 * 
	 * @return	Connection	Database connection.
	 * @throws SQLException
	 */
	@Deprecated 
	public Connection getDBConnection() throws SQLException {
		return getStudySpace().getDBConnection();
	}

	/**
	 * Returns a the DataBank object for the Survey's StudySpace.
	 * 
	 * @return	DataBank.
	 */
	public DataBank getDB() {
		return getStudySpace().getDB();
	}

	/**
	 * Displays the sub-menu on the left side of frame to show the survey progress
	 * - the sub-menu won't let the user go back to review completed pages.
	 * 
	 * @param 	currentPage		Current page the user is on in the survey.
	 * @param 	completedPages	Hashtable of the completed pages in the survey.
	 * @return	String			HTML format of the survey progress on left side of the survey page.
	 */	
	public String printProgress(Page currentPage,
			Hashtable<String, String> completedPages) {
		String s = "";
		try {
			s += "<html><head>";
			s += "<STYLE>";
			s += "a:hover {color: #aa0000; text-decoration: underline}";
			s += "a:link {color: #003366; text-decoration: none}";
			s += "a:visited {color: #003366; text-decoration: none}";
			s += "</STYLE>";
			s += "</head>";
			s += "<LINK href='" + "styleRender?app=" + getStudySpace().studyName
					+ "&css=style.css" + "' type=text/css rel=stylesheet>";
			s += "<body>";
			// s += "<body text='#000000' bgcolor='#FFFFCC' >";
			s += "<font face='Verdana, Arial, Helvetica, sans-serif'>";
			s += "<table cellpadding=4 cellspacing=0>";
			s += "<tr><td align=center valign=top>";
			s += "<img src='" + "imageRender?app=" + getStudySpace().studyName
					+ "&img=" + logoName + "' border=0></td></tr>";
			s += "<tr><td>&nbsp;</td></tr>";

			// display the interrupt link
			/*
			 * Pralav: get rid of save feature s += "<tr><td>"; s +=
			 * "<a href=\"javascript:top.mainFrame.form.document.mainform.action.value='interrupt';"
			 * ; s +=
			 * "top.mainFrame.form.document.mainform.submit();\" target=\"_top\">"
			 * ; s +=
			 * "<font size=\"-1\"><center>Click here to <B>SAVE</B> your answers "
			 * ; s += "if you need to pause</center></font></a>"; s +=
			 * "</td></tr>"; s +=
			 * "<tr><td><font size=\"-2\">&nbsp;</font></td></tr>"; s +=
			 * "<tr><td>";
			 */

			/* display the page name list */
			s += "<table width=100 height=200 border=0 cellpadding=5 cellspacing=5 bgcolor=#F0F0FF>";
			s += "<tr><td align=center>";
			s += "<font size=\"-2\"><u>Page Progress</u></font></td></tr>";

			for (int i = 0; i < getPages().length; i++) {
				s += "<tr>";
				String pageStatus = completedPages.get(getPages()[i].id);
				if (pageStatus != null
						&& pageStatus.equalsIgnoreCase("Completed")) {

					/* completed pages */
					s += "<td bgcolor='#99CCFF' align=left colspan=1>";
				} else if (pageStatus != null
						&& pageStatus.equalsIgnoreCase("Current")) {
					/* current page - will be highlighted in yellow */
					s += "<td bgcolor='#FFFF00' align=left colspan=1>";
				} else {
					s += "<td>";
				}
				s += "<font size=\"-2\">";
				s += "<b>" + getPages()[i].title + "</b></font>";
				s += "</td></tr>";
			}
			s += "</table>";
			s += "</td></tr></table>";
		} catch (NullPointerException e) {
			WISEApplication.logError(
					"WISE - SURVEY - PRINT PROGRESS WITHOUT LINK: "
							+ e.toString() + " --> " + currentPage.toString()
							+ ", " + completedPages.toString(), null);
		}
		return s;
	}

	/**
	 *  Displays the sub-menu on the left side of frame to show the survey progress
	 * - the sub-menu will have the links to let the user review the completed pages
	 * 
	 * @param 	currentPage		Current page the user is on in the survey.
	 * @return	String			HTML format of the survey progress on left side of the survey page.
	 */
	public String printProgress(Page currentPage) {
		String s = "";
		try {
			s += "<table width=100% cellpadding=0 cellspacing=0>";
			s += "<tr><td align=center valign=top>";

			/* changing the path to WISE_Application.images_path */
			s += "<img src='" + "imageRender?app=" + getStudySpace().studyName
					+ "&img=" + logoName + "' border=0></td></tr>";
			s += "<tr><td>&nbsp;</td></tr>";

			/*
			 * Pralav: remove save button s += "<tr><td>"; s +=
			 * "<a href=\"javascript:top.mainFrame.form.document.mainform.action.value='interrupt';"
			 * ; s +=
			 * "top.mainFrame.form.document.mainform.submit();\" target=\"_top\">"
			 * ; s +=
			 * "<font size=\"-1\"><center>Click here to <B>SAVE</B> your answers "
			 * ; s += "if you need to pause</center></font></a>"; s +=
			 * "</td></tr>";
			 */

			s += "<tr><td><font size=\"-2\">&nbsp;</font></td></tr>";
			s += "<tr><td>";
			s += "<table width=100 height=200 border=0 cellpadding=5 cellspacing=5 bgcolor=#F0F0FF>";
			s += "<tr><td align=center>";
			s += "<font size=\"-2\"><u>Survey Pages</u></font></td></tr>";

			int idx = getPageIndex(currentPage.id);
			for (int i = 0; i < getPages().length; i++) {
				s += "<tr>";
				if (i != idx) {
					s += "<td bgcolor='#99CCFF' align=left colspan=1>";
					s += "<a href=\"javascript:document.mainform.action.value='linkpage';";
					s += "document.mainform.nextPage.value='"
							+ getPages()[i].id + "';";
					s += "document.mainform.submit();\" target=\"_top\">";
					s += "<font size=-2><b>" + getPages()[i].title
							+ "</b></font></a>";
				} else {
					s += "<td bgcolor='#FFFF00' align=left colspan=1>";
					s += "<font size=-2><b>" + getPages()[i].title + "</b></font>";
				}
				s += "</td></tr>";
			}
			s += "</table>";
			s += "</td></tr></table>";
		} catch (Exception e) {
			WISEApplication.logError(
					"WISE - SURVEY - PRINT PROGRESS WITH LINKS: "
							+ e.toString() + " --> " + currentPage.toString(),
							null);
		}
		return s;
	}

	/**
	 * Search by ID, return a specific ResponseSet
	 * 
	 * @param 	id			Id of the specific response set needed.
	 * @return	ResponseSet	The required response set.
	 */
	public ResponseSet getResponseSet(String id) {
		ResponseSet a = responseSets.get(id);
		return a;
	}

	/**
	 * Search by ID, return a specific SubjectSet
	 * 
	 * @param 	id			Id of the specific subject set needed.
	 * @return	SubjectSet	The required subject set.
	 */
	public SubjectSet getSubjectSet(String id) {
		SubjectSet ss = subjectSets.get(id);
		return ss;
	}

	/**
	 * Search by ID, return a specific Translation
	 * 
	 * @param 	id				Id of the specific translation item needed.
	 * @return	TranslationItem	The required translation item.
	 */
	public TranslationItem getTranslationItem(String id) {
		TranslationItem t = translationItems.get(id);
		return t;
	}

	/**
	 * search by page ID, return the page index in the page array
	 * 
	 * @param 	id		Id of the specific page needed.
	 * @return	int		The index of given page in the array of pages contained by this survey.
	 */
	public int getPageIndex(String id) {
		for (int i = 0; i < getPages().length; i++) {
			if (getPages()[i].id.equalsIgnoreCase(id))
				return i;
		}
		return -1;
	}

	/**
	 * Search by page ID, return the page the page array.
	 * 
	 * @param 	id		Id of the specific page needed.
	 * @return	Page	The page from the array of pages contained by this survey.
	 */
	public Page getPage(String id) {
		int i = getPageIndex(id);
		if (i != -1) {
			return getPages()[i];
		} else {
			return null;
		}
	}

	/**
	 * Checks if the index given is same as the index of the last page 
	 * in the array of page of this survey.
	 * 
	 * @param 	idx		index of the page to be checked.
	 * @return	boolean	True of the page is last else false.
	 */
	public boolean isLastPage(int idx) {
		int i = getPages().length - 1;
		if (idx == i) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Searches by page ID and checks if the page is the last page in the page array
	 * 
	 * @param 	id		Id of the page to be checked.
	 * @return	boolean	True of the page is last else false.
	 */
	public boolean isLastPage(String id) {
		return isLastPage(getPageIndex(id));
	}

	/**
	 * Searches by page ID, get the next page from the page array
	 * 
	 * @param 	id		Id of the page whose next page is needed.
	 * @return	Page	Next page in the survey, null in case of last page or error.
	 */
	public Page nextPage(String id) {
		int idx = getPageIndex(id);
		if (idx < 0) {
			return null;
		}
		idx++;
		if (idx >= getPages().length) {
			return null;
		} else {
			return getPages()[idx];
		}
	}

	/** search by page index, return the page ID from the page array object */
	/*
	 * public String get_page_id(int idx) { if (idx > pages.length) return null;
	 * else return pages[idx].id; }
	 */

	/** returns the previous page after the page index */
	/*
	 * public Page previous_page(int indx) { if(indx <= 0 || indx > pages.length
	 * ) return null; else return pages[indx-1]; }
	 */
	/** search by page ID, get the previous page from the page array */
	/*
	 * public Page previous_page(String id) { return
	 * previous_page(get_page_index(id)); }
	 */

	/** returns the page for that page index */
	/*
	 * public Page get_page(int idx) { if (idx <= pages.length) return
	 * pages[idx]; else return null; }
	 */

	/**
	 * get table creation syntax for the OLD version of data table -- RETIRABLE
	 * public String get_table_syntax(Statement stmt) { String create_str="";
	 * try { //get the 2nd max internal id in the survey table with the same
	 * survey ID //it is the index for locating the old survey data table String
	 * sql = "select max(internal_id) from "+
	 * "(select * from surveys where id='"+id+"' and internal_id <> "+
	 * "(select max(internal_id) from surveys where id='"+id+"')) as a;";
	 * boolean dbtype = stmt.execute(sql); ResultSet rs = stmt.getResultSet();
	 * //get the value from the table column of create_syntax if(rs.next()) {
	 * String sqlc =
	 * "select create_syntax from surveys where internal_id="+rs.getString(1);
	 * boolean dbtypec = stmt.execute(sqlc); ResultSet rsc =
	 * stmt.getResultSet(); if(rsc.next())
	 * create_str=rsc.getString("create_syntax"); }
	 * 
	 * if(create_str==null) create_str=""; } catch (Exception e) {
	 * Study_Util.email_alert("SURVEY - GET DATA TABLE SYNTAX: "+e.toString());
	 * }
	 * 
	 * return create_str; }
	 */

	/**
	 * This function returns all Field Names in survey
	 * -- used by DataBank to setup database.
	 * 
	 * @return Array	Array of all the field names in the survey.
	 */
	public String[] getFieldList() {
		String[] mainFields = new String[totalItemCount];
		int mainI = 0;
		for (int pageI = 0; pageI < getPages().length; pageI++) {
			String[] pageFields = getPages()[pageI].getFieldList();
			for (int fieldI = 0; fieldI < pageFields.length; fieldI++)
				mainFields[mainI++] = pageFields[fieldI];
		}
		return mainFields;
	}

	/**
	 * This function returns all value types of fields in survey
	 * -- used by DataBank to setup database.
	 * 
	 * @return Array	Array of all the value types of the fields in the survey.
	 */
	public char[] getValueTypeList() {
		char[] mainFieldTypes = new char[totalItemCount];
		int mainI = 0;
		for (int pageI = 0; pageI < getPages().length; pageI++) {
			char[] pageTypes = getPages()[pageI].getValueTypeList();
			for (int fieldI = 0; fieldI < pageTypes.length; fieldI++)
				mainFieldTypes[mainI++] = pageTypes[fieldI];
		}
		return mainFieldTypes;
	}

	/**
	 * This following function copy the above two functions for the
	 * repeating sets.
	 * 
	 * @return	ArrayList	of repeating item set in the survey.
	 */
	public ArrayList<RepeatingItemSet> getRepeatingItemSets() {
		ArrayList<RepeatingItemSet> surveyRepeatingItemSets = new ArrayList<RepeatingItemSet>();
		for (int pageI = 0; pageI < getPages().length; pageI++) {
			ArrayList<RepeatingItemSet> pageRepeatingItemSets = getPages()[pageI]
					.getRepeatingItemSets();
			for (RepeatingItemSet pageRepeatSetInstance : pageRepeatingItemSets) {
				surveyRepeatingItemSets.add(pageRepeatSetInstance);
			}
		}
		return surveyRepeatingItemSets;
	}

	/**
	 * get table creation syntax for the new data table public String
	 * create_table_syntax() throws SQLException { String create_str=""; for
	 * (int i = 0; i < pages.length; i++) create_str += pages[i].create_table();
	 * return create_str; }
	 */

	/**
	 * Prints a brief dump of survey structure.
	 * 
	 */
	@Override
	public String toString() {
		String s = "<p><b>SURVEY</b><br>";
		s += "ID: " + id + "<br>";
		s += "Title: " + getTitle() + "<br>";
		// s += "user_data_page: "+ user_data_page +"<br>";

		for (int i = 0; i < getPages().length; i++)
			s += getPages()[i].toString();
		if (getInviteeFields() != null) {
			s += "Invitee Fields ref'd: ";
			for (int i = 0; i < getInviteeFields().length; i++)
				s += getInviteeFields()[i] + "; ";
		}
		s += "</p>";
		return s;
	}

	public String getId() {
		return id;
	}

	public boolean isAllowGoback() {
		return allowGoback;
	}
	
	public void setAllowGoback(boolean allowGoBack) {
		this.allowGoback = allowGoBack;
	}

	public String getForwardUrl() {
		return forwardUrl;
	}

	public String getEduModule() {
		return eduModule;
	}

	public StudySpace getStudySpace() {
		return studySpace;
	}

	public int getMinCompleters() {
		return minCompleters;
	}

	public Page[] getPages() {
		return pages;
	}

	public String getTitle() {
		return title;
	}

	public InviteeMetadata getInviteeMetadata() {
		return inviteeMetadata;
	}

	public String[] getInviteeFields() {
		return inviteeFields;
	}

	/** prints the overview listing for a survey */
	/*
	 * public String print_overview() { String s =
	 * "<body text='#000000' bgcolor='#FFFFCC' >"; s +=
	 * "<font face='Verdana, Arial, Helvetica, sans-serif'>";
	 * 
	 * s += "<table cellpadding=5>"; s += "<tr><td>"; //s +=
	 * "<a href=\""+Study_Space.servlet_root+"logout\" target=\"_top\">"; s +=
	 * "<a href=\""+"logout\" target=\"_top\">"; s +=
	 * "<font size=\"-1\"><center>Please <B>LOGOUT</B> when finished</center></font></a>"
	 * ; s += "</td></tr>";
	 * 
	 * s += "<tr><td>"; s +=
	 * "<p><font size=\"-2\"><i>Jump to results for any page:</i></font>"; s +=
	 * "</td></tr>";
	 * 
	 * for (int i = 0; i < pages.length; i++) { s += "<tr><td>"; s +=
	 * "<font size=\"-2\">"; //s +=
	 * "<a href='"+Study_Space.servlet_root+"view_results?page="
	 * +pages[i].id+"' target='form'>"; s +=
	 * "<a href='view_results?page="+pages[i].id+"' target='form'>"; s +=
	 * "<b>"+pages[i].title+"</b></a></font>"; s += "</td></tr>"; } s +=
	 * "</table>"; return s; }
	 */
}
