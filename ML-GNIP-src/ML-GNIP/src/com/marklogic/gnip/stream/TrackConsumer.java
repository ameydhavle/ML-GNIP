package com.marklogic.gnip.stream;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import sun.misc.BASE64Encoder;


import com.marklogic.utilities.GnipLoader;
import com.marklogic.utilities.JsonXmlReader;
import com.zaubersoftware.gnip4j.api.RemoteResourceProvider;
import com.zaubersoftware.gnip4j.api.exception.AuthenticationGnipException;
import com.zaubersoftware.gnip4j.api.exception.TransportGnipException;


/**
 * @author adhavle
 *
 */
public class TrackConsumer implements RemoteResourceProvider{
	private static int counter;
	static Properties properties = new Properties();
	static Properties masterProperties = new Properties();
	static RemoteResourceProvider facade;
	 
    public static void main(String... args) throws IOException {
    	TrackConsumer track = new TrackConsumer();
		track.getData();
    }
    
public void getData(){
	String path = new File(".").getAbsolutePath();
    	if(path.substring(path.length()-1).equals(".")){
    		path = path.substring(0,path.length()-1)+"conf.properties";
    	}else{
    		path = path+"conf.properties";
    	}
    	System.out.println("path" + path);
    	try {
    		//System.out.println(path);
			masterProperties.load(new FileInputStream(path));
			System.out.println("load prop" + masterProperties.getProperty("stream"));
		    properties.load(new FileInputStream(masterProperties.getProperty("stream")));
		    System.out.println("load prop 2");
		} catch (IOException e) {
		}
		final String username = masterProperties.getProperty("stream-username").toString();
		final String password = masterProperties.getProperty("stream-password").toString();
		String dataCollectorURL =(String) properties.get("stream-url");
		String rulesURL = (String)properties.get("rules");
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
        	System.out.println("1" + dataCollectorURL);
            connection = getConnection(dataCollectorURL, username, password);
            inputStream = connection.getInputStream();
            int responseCode = connection.getResponseCode();
            System.out.println("2");
            if (responseCode >= 200 && responseCode <= 299) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(inputStream)));
                String tmp;
      		    while(reader.ready() ){
      		    	System.out.println("3");
      		    	try{
                    	String json = reader.readLine();
                    	if(json.length()>0){
                    		tmp ="{\"marklogic\":" +json+ "}";
                    		XMLUnit.setIgnoreWhitespace(true);
                    		String tmpXml = convertToXml(tmp);
                    	}
                     }catch(Exception e){
                    	System.out.println("Transformation step" );
                    	e.printStackTrace();
                     }
                   }
            } else {
            	System.out.println("reconnecting");
            	connection = getConnection(dataCollectorURL, username, password);
                handleNonSuccessResponse(connection);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (connection != null) {
                try {
					handleNonSuccessResponse(connection);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
            }
        } finally {
            if (inputStream != null) {
                try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        }	
	}
    
    public String convertToXml(final String json) throws Exception {
        return convertToXml(json, "");
    }
    
    public String convertToXml(final String json, final String namespace) throws Exception {
    	return convertToXml(json, namespace, false);
    }
  
    public String convertToXml(final String json, final String namespace, final boolean addTypeAttributes) throws Exception {
    	counter++;
    	long start = System.currentTimeMillis();
    	String nodeUri=properties.getProperty("doc-uri").toString();
    	File tweetFile = new File(nodeUri+"_"+counter+".xml");
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
		InputSource source = new InputSource(new StringReader(json));
		Result result = new StreamResult(tweetFile);
		transformer.transform(new SAXSource(new JsonXmlReader(namespace, addTypeAttributes),source), result);
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		String outFile;
		Writer outW = new StringWriter();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(tweetFile);
			Node root = dom.getFirstChild();
			
			Node store_name = dom.createElement("platform");
			store_name.setTextContent(properties.getProperty("doc-uri"));
			root.appendChild(store_name);
			
			Node userName = dom.createElement("userName");
			String tmpUserName = dom.getElementsByTagName("displayName").item(0).getTextContent().toString();
			userName.setTextContent(tmpUserName);
			root.appendChild(userName);
			
			OutputFormat format = new OutputFormat(dom);
			format.setLineWidth(65);
            format.setIndenting(true);
            format.setIndent(2);
            
            
            XMLSerializer serializer = new XMLSerializer(outW, format);
            serializer.serialize(dom);
            
           // System.out.println(dom.getElementById("twitterTimeZone"));
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		URI serverUri = new URI (masterProperties.getProperty("xcc-url").toString());
		GnipLoader gLoader = new GnipLoader(serverUri);
		outFile = new String(outW.toString().substring(38));
		if(gLoader.loadTweets(tweetFile,outFile)){
			long elapsed = System.currentTimeMillis() - start;
			System.out.println( "file processed in " + elapsed); 
		}
		return ("Done loading file");
    }
  
    
    private static void handleNonSuccessResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage();
        System.out.println("Non-success response: " + responseCode + " -- " + responseMessage);
    }

    private static HttpURLConnection getConnection(String urlString, String username, String password) throws IOException {
        URL url = new URL(urlString);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(1000 * 60 * 60);
        connection.setConnectTimeout(1000 * 10);

        connection.setRequestProperty("Authorization", createAuthHeader(username, password));
        connection.setRequestProperty("Accept-Encoding", "gzip");

   return connection;
    }

    private static String createAuthHeader(String username, String password) throws UnsupportedEncodingException {
        BASE64Encoder encoder = new BASE64Encoder();
        String authToken = username + ":" + password;
        return "Basic " + encoder.encode(authToken.getBytes());
    }

	@Override
	public InputStream getResource(URI arg0)
			throws AuthenticationGnipException, TransportGnipException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void postResource(URI arg0, Object arg1)
			throws AuthenticationGnipException, TransportGnipException {
		// TODO Auto-generated method stub
		
	}
    
    
}
