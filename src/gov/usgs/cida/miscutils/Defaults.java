/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.usgs.cida.miscutils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A noninstantiable library class of default values for various purposes
 * 
 * @author whb
 */
public class Defaults
{
    private Defaults ()
    {
        // private constructor enforces noninstantiability
    }
    
    /**
     * Whenever a null or undeclared URI is passed as an argument, this reserved
     * URI may be substituted in method or constructor calls if the null
     * argument is not a sufficient error condition to justify an Exception.
     *
     * This is also an appropriate value when the actual semantics of an
     * expression negate the use of an otherwise required URI value: for
     * example, constructing a Request when there is actually no
     * resourceDefinitionURI that applies to the call.
     */
    public static final URI UNDECLARED_URI;

    static
    {
        try
        {
            UNDECLARED_URI = new URI ("http://cida.usgs.gov/reserved/undeclared/");
        }
        catch (URISyntaxException mue)
        {
            throw new RuntimeException ("UNDECLARED_URI syntax error.", mue);
        }
    }

    public static final String DEFAULT_HTTP_DECLARATION = "HTTP/1.1";

    public static final String DEFAULT_ENCODING = "UTF-8";

    
}
