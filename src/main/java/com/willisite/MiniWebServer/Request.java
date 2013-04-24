package com.willisite.MiniWebServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

class InvalidRequestException extends RuntimeException {
  public InvalidRequestException(String message) {
    super(message);
  }
}

class InterruptedRequestException extends RuntimeException {
  public InterruptedRequestException(String message) {
    super(message);
  }
}

public class Request {
  private static final Logger LOGGER = LoggerFactory.getLogger("Request");

  private String method = "GET";
  private URI uri = null;
  private String version = "HTTP/1.0";
  private HashMap<String, Header> headers = new HashMap<String, Header>();

  public Request()  {
    try {
      setUri("/");
    } catch (URISyntaxException e) {}
  }

  public Request(String uri) throws URISyntaxException {
    setUri(uri);
  }

  public Request(String method, String uri) throws URISyntaxException {
    this(uri);
    setMethod(method);
  }

  public Request(InputStream is) throws IOException, InterruptedRequestException, InvalidRequestException, URISyntaxException, InvalidHeaderException {
    read(is);
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    method = method.toUpperCase();
    // TODO: Validate method
    this.method = method;
  }

  public URI getUri() {
    return uri;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public void setUri(String uri) throws URISyntaxException {
    setUri(new URI(uri));
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    version = version.toUpperCase();
    if (version.equals("HTTP/1.0") || version.equals("HTTP/1.1")) this.version = version;
  }

  public Header getHeader(String key) {
    return headers.get(WordUtils.capitalizeFully(key, '-'));
  }

  public void setHeader(Header header) {
    if (!StringUtils.isBlank(header.getValue())) headers.put(header.getKey(), header);
  }

  public void setHeader(String key, String value) {
    Header h = new Header(key, value);
    setHeader(h);
  }

  public void setHeader(String header) {
    Header h = Header.parseHeader(header);
    setHeader(h);
  }

  public void removeHeader(String key) {
    headers.remove(key);
  }

  @Override
  public String toString() {
    return getMethod() + " " + getUri() + " " + getVersion();
  }

  public void parseRequest(String request) throws InvalidRequestException, URISyntaxException {
    String[] parts = request.trim().split("\\s+");
    if (parts.length != 3) throw new InvalidRequestException("Invalid request line format: " + request);
    setMethod(parts[0]);
    setUri(parts[1]);
    setVersion(parts[2]);
  }

  public void read(InputStream is) throws IOException, InterruptedRequestException, InvalidRequestException, URISyntaxException, InvalidHeaderException {
    String line = Utils.readLine(is);
    if (line == null) throw new InterruptedRequestException("No request line was received");
    parseRequest(line);
    while (!StringUtils.isBlank(line = Utils.readLine(is))) setHeader(line);
    if (getHeader("Content-Length") != null) {
      int len = 0;
      try {
        len = Integer.parseInt(getHeader("Content-Length").getValue());
      } catch (NumberFormatException e) {
        throw new InvalidHeaderException("Invalid value for Content-Length");
      }
      // TODO: Handle post
      is.skip(len);
    }
  }
}
