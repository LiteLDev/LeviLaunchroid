package com.microsoft.xbox.idp.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class HttpHeaders {
    private final List<Header> headers = new ArrayList<>();

    public void add(String str, String str2) {
        this.headers.add(new Header(str, str2));
    }

    public Collection<Header> getAllHeaders() {
        return this.headers;
    }

    public Header getFirstHeader(String str) {
        if (str == null) {
            return null;
        }
        for (Header next : this.headers) {
            if (str.equals(next.key)) {
                return next;
            }
        }
        return null;
    }

    public Header getLastHeader(String str) {
        if (str == null) {
            return null;
        }
        for (int size = this.headers.size() - 1; size >= 0; size--) {
            Header header = this.headers.get(size);
            if (str.equals(header.key)) {
                return header;
            }
        }
        return null;
    }

    @NotNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (Header append : this.headers) {
            sb.append(append);
        }
        sb.append(" ]");
        return sb.toString();
    }

    public static class Header {
        public final String key;
        private final String value;

        public Header(String str, String str2) {
            this.key = str;
            this.value = str2;
        }

        public String getKey() {
            return this.key;
        }

        public String getValue() {
            return this.value;
        }

        @NotNull
        public String toString() {
            return "{ " + "\"" + this.key + "\": " + "\"" + this.value + "\"" + " }";
        }
    }
}
