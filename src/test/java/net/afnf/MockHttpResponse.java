package net.afnf;

import java.io.IOException;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;

public class MockHttpResponse implements CloseableHttpResponse {

    private HttpEntity entity = null;
    private int statusCode;

    public MockHttpResponse(int statusCode, String content) throws Exception {
        this.statusCode = statusCode;
        this.entity = new StringEntity(content);
    }

    public HttpEntity getEntity() {
        return entity;
    }

    public Locale getLocale() {

        return null;
    }

    public StatusLine getStatusLine() {
        return new BasicStatusLine(new HttpVersion(1, 1), statusCode, "");
    }

    public void setEntity(HttpEntity arg0) {

    }

    public void setLocale(Locale arg0) {

    }

    public void setReasonPhrase(String arg0) throws IllegalStateException {

    }

    public void setStatusCode(int arg0) throws IllegalStateException {

    }

    public void setStatusLine(StatusLine arg0) {

    }

    public void setStatusLine(ProtocolVersion arg0, int arg1) {

    }

    public void setStatusLine(ProtocolVersion arg0, int arg1, String arg2) {

    }

    public void addHeader(Header arg0) {

    }

    public void addHeader(String arg0, String arg1) {

    }

    public boolean containsHeader(String arg0) {

        return false;
    }

    public Header[] getAllHeaders() {

        return null;
    }

    public Header getFirstHeader(String arg0) {

        return null;
    }

    public Header[] getHeaders(String arg0) {

        return null;
    }

    public Header getLastHeader(String arg0) {

        return null;
    }

    public HttpParams getParams() {

        return null;
    }

    public ProtocolVersion getProtocolVersion() {

        return null;
    }

    public HeaderIterator headerIterator() {

        return null;
    }

    public HeaderIterator headerIterator(String arg0) {

        return null;
    }

    public void removeHeader(Header arg0) {

    }

    public void removeHeaders(String arg0) {

    }

    public void setHeader(Header arg0) {

    }

    public void setHeader(String arg0, String arg1) {

    }

    public void setHeaders(Header[] arg0) {

    }

    public void setParams(HttpParams arg0) {

    }

    public void close() throws IOException {

    }

}
