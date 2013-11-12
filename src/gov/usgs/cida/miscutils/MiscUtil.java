package gov.usgs.cida.miscutils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A noninstantiable library class providing functions of general utility for
 * CIDA Resource Management.
 *
 * @author Bill Blondeau <wblondeau@usgs.gov>
 */
public class MiscUtil
{

    private MiscUtil ()
    {
        // private constructor enforces noninstantiability
    }

    public static byte[] byteArrayFromStream (InputStream stream) throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream ();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = stream.read (data, 0, data.length)) != -1)
        {
            buffer.write (data, 0, nRead);
        }
        buffer.flush ();

        return buffer.toByteArray ();
    }

    /**
     * Cleans, normalizes, and verifies that the argument makes an acceptable
     * URL.
     *
     * @param target
     * @return
     */
    public static URI validTarget (URI target)
    {
        if (target == null)
        {
            throw new IllegalArgumentException ("Null parameter target");
        }

        target = target.normalize ();
        // roundtrip through URL should squeeze out any gross errors of form
        try
        {
            target = target.toURL ().toURI ();
        }
        catch (MalformedURLException | URISyntaxException syntx)
        {
            throw new IllegalArgumentException ("unacceptable target URI", syntx);
        }

        return target;
    }

    /**
     * Creates a URI that is also a valid URL. If the parameter represents a
     * Syntactically valid URI that is not normalized, the return value will be
     * normalized.
     *
     * @param uriString
     * @return
     * @throws IllegalArgumentException if the parameter is null, empty or
     * blank; if the parameter is not a valid URI; or if the parameter is not a
     * valid URL.
     */
    public static URI makeValidURI (String uriString)
    {
        if (uriString == null || uriString.trim ().isEmpty ())
        {
            throw new IllegalArgumentException (
                    "Parameter 'uriString' not permitted to be "
                    + "null, empty, or blank.");
        }
        try
        {
            URI retval = new URI (uriString);
            return validTarget (retval);
        }
        catch (URISyntaxException usx)
        {
            throw new IllegalArgumentException (
                    "Parameter 'uriString' ("
                    + uriString
                    + ") not a valid URI expression.", usx);
        }
        catch (IllegalArgumentException iax)
        {
            throw new IllegalArgumentException (
                    "Parameter 'uriString' ("
                    + uriString
                    + ") is a valid URI but not a valid URL.");
        }
    }

    /**
     * Reads an InputStream into a String. This is a destructive read, in that
     * no attempt to reset the InputStream is made. Calling code should manage
     * any desired mark/reset behavior.
     *
     * Obviously not recommended for situations in which the size of the
     * InputStream's content might be large, and memory footprint is a concern.
     *
     * @param instream
     * @param encoding
     * @return
     * @throws IllegalArgumentException
     */
    public static String inputStream2String (InputStream instream, String encoding)
            throws IllegalArgumentException
    {

        // sanity
        if (instream == null)
        {
            throw new IllegalArgumentException ("Parameter 'instream' not permitted to be null.");
        }
        if (encoding == null)
        {
            throw new IllegalArgumentException ("Parameter 'encoding' not permitted to be null.");
        }

        char[] buffer = new char[8 * 1024];  // 8KB at a time
        StringBuilder builder = new StringBuilder ();
        try
        {
            try (Reader inreader = new InputStreamReader (instream, encoding))
            {
                int numChars = 0;
                while ((numChars = inreader.read (buffer, 0, buffer.length)) > 0)
                {
                    builder.append (buffer, 0, numChars);
                }
            }
        }
        catch (UnsupportedEncodingException ex)
        {
            throw new IllegalArgumentException ("Encoding not valid: ", ex);
        }
        catch (IOException iox)
        {
            throw new RuntimeException (iox);
        }

        return builder.toString ();
    }

    /**
     * Returns the DOM Document built from the passed parameter. Because Java's
     * XML parsing is a verbose go-around-your-elbow-to-get-to-your-thumb
     * annoyance.
     *
     * IMPLEMENTATION NOTE: This method performs a normalizeDocument() on the
     * Document immediately before returning it. This was put in place due to
     * extremely odd behavior when adopting the root node of one Document into
     * another Document and appending the newly imported Node. (Extremely
     * incoherent data: attributes displaying the value of other attributes,
     * missing data, etc.)
     *
     * @param xmlString must be a well-formed serialization of an XML Node,
     * whether intended as a Document or not.
     * @return
     */
    public static Document parseToXML (String xmlString)
    {
        if (xmlString == null || xmlString.trim ().isEmpty ())
        {
            throw new IllegalArgumentException ("Parameter 'xmlString' not permitted to be null, empty, or blank.");
        }

        Document retval = null;
        // parse the string, create the Document
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance ();
        try
        {
            DocumentBuilder db = fac.newDocumentBuilder ();
            retval = db.parse (new InputSource (new ByteArrayInputStream (xmlString.getBytes ())));
        }
        catch (ParserConfigurationException pce)
        {
            throw new RuntimeException ("Factory cannot create the Document.", pce);
        }
        catch (SAXException se)
        {
            throw new IllegalArgumentException ("Parameter is unparseable.", se);
        }
        catch (IOException ex)
        {
            // ...it's a String. In memory. ... what'd I do????
            throw new RuntimeException ("No idea what's wrong.", ex);
        }

        retval.normalizeDocument ();
        return retval;
    }

    /**
     * A convenience method that wraps all of the Java file-read garrulity, and
     * rethrows the FileNotFound and other checked IOExceptions as unchecked
     * RuntimeExceptions. Intended for line-oriented text content with system
     * default encoding; not useful for binary content.
     *
     * Exception handling (and preemptive sanity checking) is delegated to the
     * calling code.
     *
     *
     * @param filepathname the filepath to the desired file
     * @return the contents of the file as a String
     * @throws RuntimeException
     */
    public static String readTextFile (String filepathname)
            throws RuntimeException, IllegalArgumentException
    {
        // sanity
        if (filepathname == null || filepathname.trim ().isEmpty ())
        {
            throw new IllegalArgumentException (
                    "Parameter 'filepathname' not allowed to "
                    + "be null, empty, or blank.");
        }
        String retval = null;
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader (new FileReader (filepathname));
            String line;
            StringBuilder builder = new StringBuilder ();
            String ls = System.getProperty ("line.separator");

            while ((line = reader.readLine ()) != null)
            {
                builder.append (line);
                builder.append (ls);
            }
            retval = builder.toString ();
        }
        catch (IOException exc)
        {
            // this also catches FileNotFoundException, which is a subclass
            throw new RuntimeException (
                    "Problem reaading file '" + filepathname + "'.", exc);
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close ();
                }
                catch (IOException ioe)
                {
                    throw new RuntimeException (
                            "Problem attempting to close file reader.", ioe);
                }
            }
        }

        return retval;
    }

    /**
     * Writes the content of an InputStream into a text file. If the file does
     * not exist, it will be created. If the file does exist, it will be
     * replaced.
     *
     * Character translation from stream to text is done by default. Not
     * recommended for binary data.
     *
     * @param stream
     * @param filepathname
     * @throws RuntimeException
     */
    public static void writeStreamToTextFile (InputStream stream, String filepathname)
            throws RuntimeException
    {
        // sanity
        if (filepathname == null || filepathname.trim ().isEmpty ())
        {
            throw new IllegalArgumentException (
                    "Parameter 'filepathname' not permitted to "
                    + "be null, empty, or blank.");
        }
        if (stream == null)
        {
            throw new IllegalArgumentException (
                    "Parameter 'stream' not permitted to be null.");
        }

        String encoding = Defaults.DEFAULT_ENCODING;

        // declare outside so in scope for finally block
        BufferedWriter bw = null;

        try
        {
            // read from parameter
            InputStreamReader rddr = new InputStreamReader (stream);

            // make a read buffer
            char[] charz = new char[1024 * 8]; // 8k seems reasonable

            // set up read streams
            File file = new File (filepathname);
            FileWriter fw = new FileWriter (file);
            bw = new BufferedWriter (fw);

            int charzread;
            while ((charzread = rddr.read (charz, 0, charz.length)) > 0)
            {
                bw.write (charz);
            }
            bw.flush (); // is this necessary? Probably not harmful
        }
        catch (IOException ioe)
        {
            throw new RuntimeException (
                    "Problem occurred while attempting to copy "
                    + "InputStream data to file"
                    + filepathname, ioe);
        }
        finally
        {
            if (bw != null)
            {
                try
                {
                    bw.close ();
                }
                catch (IOException ioe)
                {
                    throw new RuntimeException (
                            "Problem closing BufferedWriter.", ioe);
                }
            }
        }

    }

    /**
     * Reads content from InputStream, transforms it according to the XSLT f
     *
     * @param source
     * @param destination
     */
    public static void transformStreamToStream (InputStream source, String xsltFilepath,
            OutputStream destination)
    {

    }

    public static String prettyPrintDocument (Document doc)
    {

        if (doc == null)
        {
            throw new IllegalArgumentException ("Parameter not permitted to be null.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream ();
        StreamResult result = new StreamResult (baos);
        DOMSource domSource = new DOMSource (doc);
        TransformerFactory tf = TransformerFactory.newInstance ();

        try
        {
            Transformer transformer = tf.newTransformer ();

            // configure the Transformer
            transformer.setOutputProperty (OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty (OutputKeys.METHOD, "xml");
            transformer.setOutputProperty (OutputKeys.INDENT, "yes");
            transformer.setOutputProperty (OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty ("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.transform (domSource, result);

        }
        catch (TransformerException exc)
        {
            throw new RuntimeException (exc);
        }

        return baos.toString ();
    }

    /**
     * Creates a properly parameterized URL. Accepts a URI and a Map&lt;String,
     * List&lt;String&gt;&gt; (which is a general data structure capable of
     * containing parsed HTTP query parameters.)
     *
     * @param endpointURL
     * @param queryParams
     * @return
     */
    public static URL parameterizeURL (
            URI endpointURL,
            Map<String, List<String>> queryParams)
            throws IllegalArgumentException
    {

        if (endpointURL == null)
        {
            throw new IllegalArgumentException (
                    "Parameter 'endpointURL' not permitted to be null.");
        }
        // throws IllegalargumentException
        MiscUtil.validTarget (endpointURL);

        if (queryParams == null)
        {
            throw new IllegalArgumentException (
                    "Parameter 'queryParams' not permitted to be null.");
        }

        String urlstring = endpointURL.toString ();

        if ( ! queryParams.isEmpty ())
        {

            if (urlstring.indexOf ("?") > 0)
            {
                urlstring += "&";
            }
            else
            {
                urlstring += "?";
            }

            urlstring += MiscUtil.makeParameterString (queryParams);
        }

        try
        {
            return new URL (urlstring);
        }
        catch (MalformedURLException mue)
        {
            throw new IllegalArgumentException (""
                    + "Parameters resolve to a malformed URL. \n'urlString': "
                    + urlstring
                    + "; \n'queryParams': "
                    + writeParamsAsQuerystring (queryParams), mue);
        }
    }

    /**
     * Converts a conventional Map of String Lists into a URL querystring.
     * <ul>
     * <li>Multiple values for the same field name are written as recurring
     * assignments of the parameter to multiple values, e.g.
     * <code>param1=val1&param1=val2&param1=val3</code>. These values are
     * written into the returned string in the order in which they appear in the
     * List.</li>
     * <li>no leading "?" or "&" separator is provided, since only the calling
     * code will know which (if any) leading separator is required.</li>
     * <li>null values are omitted entirely.</li>
     * <li>No urlencoding is performed on anything: the returned String is in a
     * fully decoded format. Any desired urlencoding is the responsibility of
     * the calling code.</li>
     * </ul>
     *
     * @param queryParams
     * @return
     * @throws IllegalArgumentException
     */
    public static String writeParamsAsQuerystring (
            Map<String, List<String>> queryParams)
            throws IllegalArgumentException
    {
        if (queryParams == null)
        {
            throw new IllegalArgumentException (
                    "Parameter 'queryParams' not permitted to be null.");
        }
        if (queryParams.isEmpty ())
        {
            return "";
        }

        StringBuilder retval = new StringBuilder ();
        String separator = "";
        for (String key : queryParams.keySet ())
        {
            if (queryParams.get (key) != null)
            {
                for (String val : queryParams.get (key))
                {
                    if (val != null)
                    {
                        retval.append (separator);
                        retval.append (key);
                        retval.append ("=");
                        retval.append (val);
                        separator = "&";
                    }
                }
            }
        }
        return retval.toString ();
    }

    /**
     * Constructs a parameter String from a parameter map. Leaves the initial
     * demarcator off, since it does not have sufficient information to know
     * whether to write "?" or "&". Trims leading and trailing space from
     * parameter names and values and then URLencodes the values.
     *
     * When a parameter has multiple values (i.e. its
     * <code>List<String>.size()</code> > 1), the values are separately
     * given with a repeated parameter name.
     *
     * @param params
     * @return
     */
    public static String makeParameterString (Map<String, List<String>> params)
    {
        String retval = "";
        for (String currentkey : params.keySet ())
        {
            for (String item : params.get (currentkey))
            {
                retval += currentkey + "=";
                String trimmed = urlencode (item.trim ());
                retval += trimmed;
                retval += "&";
            }
        }
        // remove trailing & from parameter string
        retval = retval.substring (0, retval.length () - 1);

        return retval;
    }

    /**
     * All the benefits of urlencoding to UTF-8 without the checked exception
     *
     * @param param
     * @return the urlencoded form of param, trimmed and with nulls converted to
     * zero-length Strings
     */
    public static String urlencode (String param)
    {
        if (param == null)
        {
            param = "";
        }

        try
        {
            return URLEncoder.encode (param, Defaults.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException uee)
        {
            // if UTF-8 is unsupported, we are so screwed.
            // converting this to unchecked exception.
            throw new RuntimeException (Defaults.DEFAULT_ENCODING + " unsupported?", uee);
        }
    }

    public static String repeat (String str, int times)
    {
        if (times < 0)
        {
            throw new IllegalArgumentException (
                    "Parameter 'times' must be nonnegative. Passed: " + times);
        }

        // common cases
        if (str == null || times == 0)
        {
            return "";
        }
        if (times == 1)
        {
            return str;
        }

        // general case
        StringBuilder retval = new StringBuilder (str.length () * times);
        for (int indx = 0; indx < times; indx ++)
        {
            retval.append (str);
        }

        return retval.toString ();
    }
}
