package com.example.a1117p.bboomvpn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipInputStream;

public class HttpResponse {

    private static final boolean DEBUG = false;

    private static final int MAX_LINE_LENGTH = 65536;
    private static final int MAX_HEADERS = 256;
    private static final int MAX_CHUNKS = 10;
    private static final int BUFFER_SIZE = 1024;
    private static final int DISK_CACHE_CUTOFF = 1048576;
    private static final Pattern STATUS_PATTERN = Pattern.compile("^HTTP/1\\.[0|1]\\s(\\d+)\\s(.*)$");

    private Map<String, String> headers = new HashMap<String, String>();
    private int status;
    private int contentLength;
    private String statusLine;
    private String statusMessage;
    private byte[] responseBody;

    private HttpResponse() {
    }

    public static byte[] reverse(byte[] packet, byte[] bytes, boolean isGzip) throws IOException {

//		System.out.println(Thread.currentThread().getName() + ": com.example.a1117p.bboomvpn.HttpResponse.parse()");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        count = 0;
        String statusLine = readLine2(packet);
        outputStream.write(statusLine.getBytes());


        String header = null;
        String name, value;
        int i = 0, len = 0;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (isGzip) {
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
            gzipOutputStream.write(bytes);
            gzipOutputStream.finish();
        } else {
            byteArrayOutputStream.write(bytes);
        }
        byte[] tmp = byteArrayOutputStream.toByteArray();
        len = tmp.length;
        do {
            header = readLine2(packet);
            if (header != null) {

                if (header.indexOf(':') == -1) {
                    throw new IOException("Invalid HTTP response header found: " + header);
                }
                if (header.substring(0, header.indexOf(": ")).equalsIgnoreCase("Content-Length")) {

                    outputStream.write("Transfer-Encoding: chunked\r\n".getBytes());
                }

                outputStream.write(header.getBytes());


            }
        } while (header != null && i++ < MAX_HEADERS);

        outputStream.write("\r\n".getBytes());
        outputStream.write(Integer.toHexString(len).getBytes());
        outputStream.write("\r\n".getBytes());
        outputStream.write(tmp);
        outputStream.write("\r\n0\r\n\r\n".getBytes());

// this is sort of ugly, but meh

        return outputStream.toByteArray();
    }

    public static HttpResponse parse(byte[] bytes) throws IOException, NumberFormatException, NotEndException {
//		System.out.println(Thread.currentThread().getName() + ": com.example.a1117p.bboomvpn.HttpResponse.parse()");
        count = 0;
        String str = new String(bytes);
        HttpResponse response = new HttpResponse();
        response.statusLine = readLine(bytes);

        Matcher matcher = STATUS_PATTERN.matcher(response.statusLine);
        if (matcher.find()) {
            response.status = Integer.parseInt(matcher.group(1));
            response.statusMessage = matcher.group(2);
        }

        String header = null;
        String name, value;
        int i = 0, len = 0;
        do {
            header = readLine(bytes);
            if (header != null) {

                if (header.indexOf(':') == -1) {
                    throw new IOException("Invalid HTTP response header found: " + header);
                }

                debug(header);

                name = header.substring(0, header.indexOf(":")).trim();
                value = header.substring(header.indexOf(":") + 2).trim();

                response.headers.put(name, value);


            }
        } while (header != null && i++ < MAX_HEADERS);

        len = response.contentLength = response.getIntHeader("Content-Length");
        if (len == 0) { // we're done here
            return response;
        }

        OutputStream out;


        out = new ByteArrayOutputStream();


        if ("chunked".equalsIgnoreCase(response.getHeader("Transfer-Encoding"))) {

            i = len = 0;
            int hex = 0;
            String strHex;

            do {

                strHex = readLine(bytes).trim();
                hex = Integer.parseInt(strHex, 16);

//				debug("Reading " + hex + " bytes in this chunk");
                len += readBody(bytes, out, hex);

                if (hex > 0) {
                    readLine(bytes); // there's a blank line before the content
                }

            } while (hex > 0 && i++ < MAX_CHUNKS);

            response.contentLength = len;
            response.responseBody = ((ByteArrayOutputStream) out).toByteArray();

        } else {

            readBody(bytes, out, len);

// this is sort of ugly, but meh
            response.responseBody = ((ByteArrayOutputStream) out).toByteArray();

        }

        return response;
    }

    static int count;

    private static String readLine(byte[] bytes) throws IOException {
        StringBuffer buf = new StringBuffer();

        int i = 0;
        char c;
        int len = bytes.length;
        while ( count < len &&(c = (char) bytes[count++]) != '\n' && i++ < MAX_LINE_LENGTH) {
            if (c == '\r') {
                continue;
            }
            buf.append(c);
        }

        if (i > 1) {
            return buf.toString();
        } else {
            return null;
        }
    }

    private static String readLine2(byte[] bytes) throws IOException {
        StringBuffer buf = new StringBuffer();

        int i = 0;
        char c;
        int len = bytes.length;
        while ((c = (char) bytes[count++]) != '\n' && count <= len && i++ < MAX_LINE_LENGTH) {
            buf.append(c);
        }
        buf.append(c);

        if (i > 1) {
            return buf.toString();
        } else {
            return null;
        }
    }

    private static int readBody(byte[] bytes, OutputStream out, int len) throws IOException, NotEndException {

        try {
            out.write(bytes, count, len);
            count +=len;
        } catch (IndexOutOfBoundsException e) {
            throw new NotEndException();
        }
        return len;
    }

    public int getStatus() {
        return status;
    }

    public String getStatusLine() {
        return statusLine;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Set<String> getHeaderNames() {
        return headers.keySet();
    }

    public int getIntHeader(String name) {
        int result = -1;
        try {
            result = Integer.parseInt(headers.get(name));
        } catch (Exception e) {
        }
        return result;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getContentLength() {
        return contentLength;
    }

    /**
     * Return the response body as a String
     * WARNING:  If you call this method on
     * a very large response, it will load
     * the whole thing into memory, which
     * is almost certainly not what you want.
     * Use getResponseBodyAsStream for large
     * responses, where it will be streamed
     * from disk.
     *
     * @return the http response body
     */
    public String getResponseBodyAsString() throws IOException, FileNotFoundException {
        return new String(getResponseBody());
    }

    /**
     * Return the response body as a byte array
     * WARNING:  If you call this method on
     * a very large response, it will load
     * the whole thing into memory, which
     * is almost certainly not what you want.
     * Use getResponseBodyAsStream for large
     * responses, where it will be streamed
     * from disk.
     *
     * @return the http response body
     */
    public byte[] getResponseBody() throws FileNotFoundException, IOException {

        String encoding = headers.get("Content-Encoding");
        if (encoding == null) {
            return responseBody;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in;


        in = new ByteArrayInputStream(responseBody);


        if ("gzip".equals(encoding)) {
            in = new GZIPInputStream(in);
        } else if ("deflate".equals(encoding)) {
            in = new ZipInputStream(in);
        }

        try {
            int n;
            byte[] buf = new byte[1024];
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    public InputStream getResponseBodyAsStream() throws FileNotFoundException, IOException {

        String encoding = headers.get("Content-Encoding");
        InputStream in;


        in = new ByteArrayInputStream(responseBody);


        if ("gzip".equals(encoding)) {
            in = new GZIPInputStream(in);
        } else if ("deflate".equals(encoding)) {
            in = new ZipInputStream(in);
        }

        return in;

    }

    private static void debug(String message) {
        if (DEBUG) {
            System.out.println(message);
        }
    }
}