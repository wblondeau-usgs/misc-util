/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.usgs.cida.miscutils;

/**
 *
 * This enumeration of recognized media types (aka "mime types") 
 * for data serialization is offered as a convenience. The 
 * OuterFace needs to deal with serialization directives, so it goes 
 * into this package (for now.)
 * 
 * @author Bill Blondeau
 */
public enum MediaType
{
    XML ("xml", "text/xml",
            "Normal text serialization of XML Infoset content"),
    FI ("fi", "application/fastinfoset",
            "Fast Infoset optimized serialization of XML Infoset content"),
    TSV ("tsv", "text/tab-separated-values",
            "Tabular represntation, rows delimited by newline, data "
            + "items delimited by tab"),
    CSV ("csv", "text/csv",
            "Tabular represntation, rows delimited by newline, data "
            + "items delimited by comma"),
    TEXT ("text", "text/plain",
            "Whitespace-preserved character serialization of raw content");
    
    
    private final String shortname;
    private final String ianaName;
    private final String description;
    
    private MediaType (String shortname, String ianaName, String description)
    {
        this.shortname = shortname;
        this.ianaName = ianaName;
        this.description = description;
    }
    
    public String getShortName ()
    {
        return this.shortname;
    }
    
    public String getIANAName ()
    {
        return this.ianaName;
    }
    
    public String getDescription ()
    {
        return this.description;
    }
}
