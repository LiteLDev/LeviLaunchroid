package com.microsoft.xbox.service.model.serialization;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;


@Root(name = "version")
public class Version {
    @Element
    public int latest;
    @Element
    public int min;
    @Element
    public String url;
}
