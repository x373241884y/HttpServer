package com.pack;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by toor on 16-2-25.
 */
public class Response {

    private HashMap<String,String> headers;

    private Socket socket;
    private OutputStream outputStream;
    private Request request;
    private PrintWriter pw;
    private String resourcepath;

    public Response(Socket socket, Request request) throws IOException {
        this.socket = socket;
        this.request = request;
        this.outputStream = socket.getOutputStream();
        this.pw = new PrintWriter(this.outputStream);
        headers = new HashMap<>();
        init();
        analyseMime();
    }

    private void init() {
        headers.put("contentType","text/plain");
        headers.put("status","200 OK");
        headers.put("charSet","utf-8");
        headers.put("date",new Date().toString());
        headers.put("contentLocation",request.getRequestURI());
        headers.put("server","javaway server");
    }

    private void analyseMime() {
        String uri = request.getRequestURI();
        uri = uri.replaceAll("\\?.*", "");
        if (uri.endsWith(".html")) {
            setContentType("text/html");
        }else if (uri.endsWith(".css")) {
            setContentType("text/css");
        }else if (uri.endsWith(".js")) {
            setContentType("application/javascript");
        }else if (uri.endsWith(".png")) {
            setContentType("image/png");
        }else if (uri.endsWith(".gif")) {
            setContentType("image/gif");
        }else if (uri.endsWith(".jpeg") || uri.endsWith(".jpg")) {
            setContentType("image/jpeg");
        }else if (uri.endsWith(".json")) {
            setContentType("application/json");
        }else if (uri.endsWith(".do")) {
            setContentType("application/json");
            String referer = request.getVxreferer(); //htmls/Menu/Menu.html
            if (referer!=null) {
                String prefix = referer.replaceAll("(htmls)(.*\\/)\\w+\\.html", "/data/$2");
                uri = prefix + uri.replaceAll(".*\\/(.*)\\.do", "$1.json");
            }
        }
        this.resourcepath=uri;
    }

    public void error(String status, String message) {
        pw.println("HTTP/1.1 "+status);
        pw.println("Server:"+headers.get("server"));
        pw.println("Content-Type:text/html"+";charset:"+headers.get("charSet"));
        pw.println("Last-Modified:"+headers.get("date"));
        pw.println("Date:"+headers.get("date"));
        pw.println("Accept-ranges: bytes");
        pw.println();
        pw.println("<html><head><title>test server</title></head><body><p> Http Status => "+status+"<br/><br/><br/> Http Url => " + request.getRequestURI() + "</p></body></html>");
        pw.flush();
        pw.close();
    }

    public void write(String string) {
        pw.print(string);
    }

    public void sendHeaders() {
        pw.println("HTTP/1.1 "+headers.get("status"));
        pw.println("Server:"+headers.get("server"));
        pw.println("Content-Type:"+headers.get("contentType")+";charset:"+headers.get("charSet"));
        pw.println("Last-Modified:"+headers.get("date"));
        pw.println("Date:"+headers.get("date"));
        pw.println("Accept-ranges: bytes");
        pw.println();
    }

    public void flush() {
        pw.flush();
    }

    public void end() throws IOException {
        socket.close();
    }


    public void setContentType(String string) {
        headers.put("contentType", string);
    }

    //get
    public Socket getSocket() {
        return socket;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public Request getRequest() {
        return request;
    }

    public String getResourcepath() {
        return resourcepath;
    }
}
