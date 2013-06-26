package com.marklogic.utilities;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.marklogic.utilities.GnipLoader;
import com.zaubersoftware.gnip4j.api.model.Activity;

public class CreateDocument {
	static int cnt;
	public void createXML(Activity activity){
		try
		{
			
		  /*
		   * <alert>
  				<id>Alerts-8Dump</id>
  				<message>Dump  MICROSOFT CORP  Dump</message>
  				<platform>twitter</platform>
  				<postedDate>2012-03-24</postedDate>
  				<statuses>5823</statuses>
  				<followers>308</followers>
  				<security>MICROSOFT CORP</security>
  				<ticker>MSFT</ticker>
  				<sector>Technology</sector>
  				<signal>Dump</signal>
  				<trader>Andy Downings</trader>
			</alert>
		   * */
		  cnt++;	
		  DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		  DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		  
		  //root elements
		  Document doc = docBuilder.newDocument();

		  Element rootElement = doc.createElement("alert");
		  doc.appendChild(rootElement);

		  //ID 
		  Element id = doc.createElement("id");
		  id.appendChild(doc.createTextNode(activity.getId()));
		  rootElement.appendChild(id);
		  
		  //Followers
		  Element followers = doc.createElement("followers");
		  followers.appendChild(doc.createTextNode(activity.getActor().getFollowersCount().toString()));
		  rootElement.appendChild(followers);
		
		  //user statuses 
		  Element userStatuses = doc.createElement("userStatusCount");
		  userStatuses.appendChild(doc.createTextNode(Integer.toString(activity.getActor().getStatusesCount())));
		  rootElement.appendChild(userStatuses);

		  //Message
		  Element tweet = doc.createElement("message");
		  tweet.appendChild(doc.createTextNode(activity.getBody().toString()));
		  rootElement.appendChild(tweet);
		  
		  Element body = doc.createElement("body");
		  body.appendChild(doc.createTextNode(activity.getBody().toString()));
		  rootElement.appendChild(body);
		  
		  //tweet-url link
		  Element tweet_url = doc.createElement("link");
		  tweet_url.appendChild(doc.createTextNode(activity.getLink().toString()));
		  rootElement.appendChild(tweet_url);

		  //Trader
		  Element trader = doc.createElement("trader");
		  System.out.println("Trader Display Name: " + activity.getActor().getDisplayName());
		  trader.appendChild(doc.createTextNode(activity.getActor().getDisplayName()));
		  rootElement.appendChild(trader);
		  
		  //Platform
		  Element platform = doc.createElement("platform");
		  platform.appendChild(doc.createTextNode("twitter"));
		  rootElement.appendChild(platform);

		  //tweet timestamp 
		  Element tweeted_on = doc.createElement("date");
		  tweeted_on.appendChild(doc.createTextNode(activity.getActor().getPostedTime().toString()));
		  rootElement.appendChild(tweeted_on);
		  
		  TransformerFactory tf = TransformerFactory.newInstance();
		  Transformer transformer1 = tf.newTransformer();
		  transformer1.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		  StringWriter writer = new StringWriter();
		  transformer1.transform(new DOMSource(doc), new StreamResult(writer));
		  String output = writer.getBuffer().toString();
		  
		  //write the content into xml file
		  TransformerFactory transformerFactory = TransformerFactory.newInstance();
		  Transformer transformer = transformerFactory.newTransformer();
		  DOMSource source = new DOMSource(doc);
		  // Create a read-write memory-mapped file
		  String filename = "/Users/adhavle/desktop/marklogic-twitter-" 
				  			+ activity.getActor().getPostedTime().getMonth()
				  			+"-"+activity.getActor().getPostedTime().getDay()
				  			+"-"+activity.getActor().getPostedTime().getYear()
				  			+"-"+cnt+".xml";
		  File tweetFile = new File(filename);
		  try{
		  StreamResult result =  new StreamResult(tweetFile);
		  transformer.transform(source, result);
		  URI serverUri = new URI ("xcc://admin:Arjun2010@localhost:8802");
		  GnipLoader gLoader = new GnipLoader(serverUri);
		  gLoader.loadTweets(tweetFile,output);
		  }catch(Exception e){
			  e.printStackTrace();
		  }
		   System.out.println("Done");
		}catch(ParserConfigurationException pce){
		  pce.printStackTrace();
		}catch(TransformerException tfe){
		  tfe.printStackTrace();
		}

	
	}

}
