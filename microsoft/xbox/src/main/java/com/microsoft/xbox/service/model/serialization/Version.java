package com.microsoft.xbox.service.model.serialization;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

@Root(name = "version")
public class Version {
    @Element
    public int latest;
    @Element
    public int min;
    @Element
    public String url;
}
