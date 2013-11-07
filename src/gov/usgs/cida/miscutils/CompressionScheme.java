/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.usgs.cida.miscutils;

/**
 *
 * This enumeration of recognized compression treatments for data 
 * is offered as a convenience. The OuterFace needs to deal with compression
 * and archiving directives, so it goes into this package (for now.)
 * 
 * The distinguished value NONE means exactly what it sounds like: no 
 * compression is applied.
 * 
 * @author Bill Blondeau
 */
public enum CompressionScheme
{
    NONE,
    ZIP,
    GZIP,
    BZIP,
    TAR_GZ;
}
