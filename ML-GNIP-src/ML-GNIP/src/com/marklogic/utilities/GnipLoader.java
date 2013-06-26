package com.marklogic.utilities;


import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentCreateOptions;
import com.marklogic.xcc.ContentFactory;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.examples.ModuleRunner;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.exceptions.XQueryException;
import com.marklogic.xcc.exceptions.XccConfigException;

public class GnipLoader
{
	static Properties properties = new Properties();
	static Properties masterProperties = new Properties();
    private static int cnt;
	private final Session session;
	private ContentCreateOptions options = null;
	ModuleRunner mRunner=null;
	
	public GnipLoader (URI serverUri) throws XccConfigException
	{
		mRunner = new ModuleRunner(serverUri);
		ContentSource cs = ContentSourceFactory.newContentSource (serverUri);
		session = cs.newSession();
	}
	
	public boolean loadTweets (File gnipTweet,String outFile) throws RequestException, URISyntaxException, XccConfigException
	{
		String returnVal = "";
		String path = new File(".").getAbsolutePath();
    	if(path.substring(path.length()-1).equals(".")){
    		path = path.substring(0,path.length()-1)+"conf.properties";
    	}else{
    		path = path+"conf.properties";
    	}
		try {
			masterProperties.load(new FileInputStream(path));
		    properties.load(new FileInputStream(masterProperties.getProperty("stream").toString()));
		} catch (IOException e) {
		}
		cnt++;
		
		Date dateTime = new java.util.Date();
		String dtTime = Long.toString(dateTime.getTime());
		String tmpURI = "/"+properties.getProperty("doc-uri")+"_"+dtTime+".xml";
		String highlight = "import module namespace functx = \"http://www.functx.com\" at \"/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy\";\n" +
				"import module namespace mem = \"http://xqdev.com/in-mem-update\" at \"/MarkLogic/appservices/utils/in-mem-update.xqy\";"+
				"declare namespace html = \"http://www.w3.org/1999/xhtml\";\n" + 
				"declare namespace xl = \"http://schemas.openxmlformats.org/spreadsheetml/2006/main\";let $prod-map:= map:map()\n" + 
				"let $prod-doc := xdmp:document-get(\""+masterProperties.getProperty("product-snomed").toString()+"\")\n" +
				"let $products := $prod-doc/xl:sst/xl:si/fn:string(xl:t)\n" + 
				" \n" + 
				"let $symp-map := map:map()\n" + 
				"let $symp-doc := xdmp:document-get(\""+masterProperties.getProperty("symptom-snomed").toString()+"\")\n" +
				"let $symptoms := $symp-doc/xl:sst/xl:si/fn:string(xl:t)\n" + 
				" \n" + 
				"let $push-symptoms :=\n" + 
				"             for $i at $d in $symptoms\n" + 
				"             return map:put($symp-map, fn:string($d), fn:lower-case($i))\n" + 
				"            \n" + 
				"let $push-products :=\n" + 
				"             for $i at $d in $products\n" + 
				"             return map:put($prod-map, fn:string($d), functx:capitalize-first($i))\n" + 
				"            \n" + 
				"let $prod-query := cts:word-query(map:get($prod-map,map:keys($prod-map)),\"case-insensitive\")\n" + 
				"let $symp-query := cts:word-query(map:get($symp-map,map:keys($symp-map)), \"case-insensitive\")\n" + 
				"let $doc:= " + outFile + 
				"\n" + 
				"let $desc :=\n" + 
				"             $doc//"+properties.getProperty("stream-body").toString()+"\n" + 
				"             for $description at $n in $desc\n" + 
				"             let $sym:= fn:string-length(cts:highlight($description, $symp-query, <adverse_event>{functx:capitalize-first($cts:text)}</adverse_event>)//adverse_event[1]/text())\n" + 
				"             let $prod:= fn:string-length(cts:highlight($description, $prod-query, <product_name>{functx:capitalize-first($cts:text)}</product_name>)//product_name[1]/text())\n" + 
				"          	  return \n" + 
				"			  if($sym > 0 and $prod > 0)"+
		        "    		  then"+
				"             let $res:= xdmp:document-insert(\""+tmpURI+"\", $doc,xdmp:default-permissions(), \n" + 
				"         	 'twitter')\n" +
				"return fn:true() \n"+			
				"			  else fn:false() "		; 
		
		String highlightReplace = "import module namespace functx = \"http://www.functx.com\" at \"/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy\";\n" + 
				"import module namespace mem = \"http://xqdev.com/in-mem-update\" at \"/MarkLogic/appservices/utils/in-mem-update.xqy\";\n" + 
				"declare namespace html = \"http://www.w3.org/1999/xhtml\";\n" + 
				"declare namespace xl = \"http://schemas.openxmlformats.org/spreadsheetml/2006/main\";let $prod-map:= map:map()\n" + 
				"let $doc:= fn:doc(\""+tmpURI+"\")\n" + 
				"return \n" + 
				"if($doc)\n" + 
				"then \n" + 
				" \n" + 
				"let $prod-doc := xdmp:document-get(\""+masterProperties.getProperty("product-snomed").toString()+"\")\n" + 
				"let $products := $prod-doc/xl:sst/xl:si/fn:string(xl:t)\n" + 
				" \n" + 
				"let $symp-map := map:map()\n" + 
				"let $symp-doc := xdmp:document-get(\""+masterProperties.getProperty("symptom-snomed").toString()+"\")\n" + 
				"let $symptoms := $symp-doc/xl:sst/xl:si/fn:string(xl:t)\n" + 
				" \n" + 
				"let $push-symptoms :=\n" + 
				"             for $i at $d in $symptoms\n" + 
				"             return map:put($symp-map, fn:string($d), fn:lower-case($i))\n" + 
				"            \n" + 
				"let $push-products :=\n" + 
				"             for $i at $d in $products\n" + 
				"             return map:put($prod-map, fn:string($d), functx:capitalize-first($i))\n" + 
				"            \n" + 
				"let $prod-query := cts:word-query(map:get($prod-map,map:keys($prod-map)),\"case-insensitive\")\n" + 
				"let $symp-query := cts:word-query(map:get($symp-map,map:keys($symp-map)), \"case-insensitive\")\n" + 
				"let $product_name := cts:highlight(\n" + 
				"                       cts:highlight($doc//body, $prod-query, <product_name>{functx:capitalize-first($cts:text)}</product_name>),\n" + 
				"                       $symp-query,\n" + 
				"                       <adverse_event>{functx:capitalize-first($cts:text)}</adverse_event>)  \n" + 
				"let $desc :=\n" + 
				"             $doc//"+properties.getProperty("stream-body").toString()+"\n" + 
				"             for $description at $n in $desc\n" + 
				"             return\n" + 
				"			  xdmp:node-replace\n" + 
				"               (\n" + 
				"                     $description,\n" + 
				"                     <body product_name=\"{$product_name/product_name[1]}\" adverse_event=\"{$product_name/adverse_event[1]}\">{functx:remove-elements-not-contents(cts:highlight(\n" + 
				"                  cts:highlight($description, $prod-query, <product_name>{functx:capitalize-first(cts:stem(fn:lower-case($cts:text),\"en\"))}</product_name>),\n" + 
				"                   $symp-query,\n" + 
				"                   <adverse_event>{functx:capitalize-first(cts:stem(fn:lower-case($cts:text),\"en\"))}</adverse_event>), 'body')}</body>\n" + 
				"                )\n" + 
				"               else()  ";
		
		String cleanUp =
				"let $doc:= fn:doc(\""+tmpURI +"\")\n" + 
				"return\n" + 
				"if(fn:not($doc//product_name)) then \n" + 
				"xdmp:document-delete(fn:document-uri($doc)) else()";
		
		/* String enrichUserName =
				"let $doc:= fn:doc(\""+tmpURI +"\")\n" + 
				"return\n" + 
				"if(fn:string-length($doc/userName/text()) gt 1) then () else "+
				"xdmp:node-insert-child($doc/marklogic,<userName>{$doc/marklogic/actor/displayName/text()}</userName>)"; */
		
		String enrichPostedDate ="import module namespace functx = \"http://www.functx.com\" at \"/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy\";\n" + 
				"let $doc:= fn:doc(\""+tmpURI +"\")\n" + 
				"  let $timeZone := xs:string($doc//*:twitterTimeZone[1]/text())\n" + 
				"  let $dtF := $doc//*:postedTime[1]/text()\n" + 
				"  return \n" + 
				"   xdmp:node-insert-child($doc/marklogic,\n" + 
				"    <posted_date>{xs:date(fn:substring($dtF[1],0,functx:index-of-string($dtF[1],'T')))}</posted_date>);";
		
		String enrichRegion = "import module namespace functx = \"http://www.functx.com\" at \"/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy\";\n" + 
				"let $doc:= fn:doc(\""+tmpURI +"\")\n" + 
				"  let $timeZone := xs:string($doc//*:twitterTimeZone[1]/text())\n" + 
				"  return \n" + 
				"  xdmp:node-insert-child($doc/marklogic,\n" + 
				"    <region>{if ($timeZone) then fn:tokenize($timeZone,'\\s')[1] else \"N.A.\"}</region>);";
		
		String enrichFollowerCount = "import module namespace functx = \"http://www.functx.com\" at \"/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy\";\n" + 
				"let $doc:= fn:doc(\""+tmpURI +"\")\n" + 
				"  let $timeZone := xs:double($doc//*:followersCount)\n" + 
				"  return \n" + 
				"   xdmp:node-insert-child($doc/marklogic,\n" + 
				"    <followers>{if ($timeZone castable as xs:double) then $timeZone else 0}</followers>)";
		
		String enrichStatusesCount ="import module namespace functx = \"http://www.functx.com\" at \"/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy\";\n" + 
				"let $doc:= fn:doc(\""+tmpURI +"\")\n" + 
				"  let $timeZone := xs:double($doc//*:statusesCount)\n" + 
				"  return \n" + 
				"   xdmp:node-insert-child($doc/marklogic,\n" + 
				"    <statuses>{if ($timeZone castable as xs:double) then $timeZone else 0}</statuses>)";
		
		String enrichCompanyName="declare function local:getCompany($prod as xs:string){\n" + 
				"let $prod-doc := xdmp:document-get(\""+masterProperties.getProperty("company").toString()+"\")\n" + 
				"for $x in $prod-doc//*:t\n" + 
				"where $x = $prod\n" + 
				"return \n" + 
				"$x/@company\n" + 
				"};\n" + 
				"let $doc:= fn:doc(\""+tmpURI +"\")\n" + 
				"let $desc :=\n" + 
				"             $doc//"+properties.getProperty("stream-body").toString()+"\n" + 
				"for $description at $n in $desc\n" + 
				"  let $enrichedElem := <product_name company=\"{if(local:getCompany($description/product_name[last()]/text())[1]) then local:getCompany($description/product_name[last()]/text())[1] else \"N.A.\"}\">{data($description/product_name[last()])}</product_name>\n" + 
				"  return \n" + 
				"   xdmp:node-replace($description/product_name[last()],$enrichedElem)\n" + 
				"";
		
		try{
		Request request = session.newAdhocQuery(highlight);
		ResultSequence hightlightRS = session.submitRequest (request);
		//System.out.println("Query: " + highlight);
		String res = hightlightRS.asString();
		System.out.println("Parsing tweet body,contains product and adverse event(s) : " +res);
		returnVal = res;
		}catch(Exception e){
			System.out.println("*******************");
			System.out.println("Error highlight query" + e.getMessage());
			System.out.println("*******************");
			e.printStackTrace();
		}
		
		if(returnVal.equalsIgnoreCase("true"))
		{
			/*
			Request requestSeverity = session.newAdhocQuery(severity);
			ResultSequence requestSeverityRS = session.submitRequest(requestSeverity);

			Request requestSector = session.newAdhocQuery(sectorSecurity);
			ResultSequence requestSectorRS = session.submitRequest(requestSector);
			*/
			Request requestReplace = session.newAdhocQuery(highlightReplace);
			ResultSequence requestReplaceRS = session.submitRequest(requestReplace);
			System.out.println("Query: " + highlightReplace);
			
			Request requestCleanUp = session.newAdhocQuery(cleanUp);
			ResultSequence requestCleanUpRS = session.submitRequest(requestCleanUp);
			
			Request enrichPostedDateR,enrichFollowerCountR,enrichStatusesCountR,enrichRegionR,enrichCompanyR;
			
			enrichPostedDateR = session.newAdhocQuery(enrichPostedDate);
			ResultSequence enrichPostedDateRS = session.submitRequest (enrichPostedDateR);
			
			enrichFollowerCountR = session.newAdhocQuery(enrichFollowerCount);
			ResultSequence enrichFollowerCountRS = session.submitRequest (enrichFollowerCountR);
			
			enrichStatusesCountR = session.newAdhocQuery(enrichStatusesCount);
			session.submitRequest(enrichStatusesCountR);
			
			enrichRegionR = session.newAdhocQuery(enrichRegion);
			session.submitRequest(enrichRegionR);
			try{
			enrichCompanyR = session.newAdhocQuery(enrichCompanyName);
			session.submitRequest(enrichCompanyR);
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
		
		//Clean up
		GenericExtFilter gFilter = new GenericExtFilter(".xml");
		String currentDir = new File(".").getAbsolutePath();
		File dir = new File(currentDir);
		String[] list = dir.list(gFilter);
		File filedelete;
		for (String s : list){
			filedelete = new File(s);
			filedelete.delete();
		}
		session.close();
		return true;
	}
	
	

	public void setOptions (ContentCreateOptions options)
	{
		this.options = options;
	}

}

