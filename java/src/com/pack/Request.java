package com.pack;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;

public class Request {
    private String method;// 请求方法
    private String protocol;// 协议版本
    private String requestURL;
    private String requestURI;//请求的URI地址  在HTTP请求的第一行的请求方法后面
    private String host;//请求的主机信息
    private String connection;//Http请求连接状态信息 对应HTTP请求中的Connection
    private String agent;// 代理，用来标识代理的浏览器信息 ,对应HTTP请求中的User-Agent:
    private String language;//对应Accept-Language
    private String encoding;//请求的编码方式  对应HTTP请求中的Accept-Encoding
    private String charset;//请求的字符编码  对应HTTP请求中的Accept-Charset
    private String accept;// 对应HTTP请求中的Accept;
    private String referer;
    private String vxreferer;

    private InputStream inputStream;
    private Socket socket;


    public Request(Socket socket) throws IOException {
        this.socket = socket;
//      BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream())); // 不能使用字符流
        InputStream inputStream = socket.getInputStream();
        this.inputStream = inputStream;
        StringBuffer sb = new StringBuffer();
        byte[] temp;
        while (true) {
            temp = new byte[1024];
            int flag = inputStream.read(temp);
            if(flag!=-1){
                String line = new String(temp, 0, flag);
                line = URLDecoder.decode(line, "UTF-8");
                sb.append(line);
                if (flag < 1024) {
                    break;
                }
            }else{
                break;
            }
        }
        parser(sb.toString()); //parse headers
        this.requestURL = host + requestURI;
    }

    private void parser(String string) {
        String[] lines = string.split(System.getProperty("line.separator"));
        String s;
        for (int i = 0; i < lines.length; i++) {
            s = lines[i].trim();
            if (s.startsWith("GET")) {
                this.method = "GET";
                int index = s.indexOf("HTTP");
                String uri = s.substring(3 + 1, index - 1);// 用index-1可以去掉连接中的空格
                uri = uri.replaceAll("\\?.*", "");
                this.requestURI = uri;
                this.protocol = s.substring(index);
            } else if (s.startsWith("POST")) {
                this.method = "POST";

                int index = s.indexOf("HTTP");
                String uri = s.substring(3 + 1, index - 1);// 用index-1可以去掉连接中的空格
                uri = uri.replaceAll("\\?.*", "");
                this.requestURI = uri;
                this.protocol = s.substring(index);

            } else if (s.startsWith("Accept:")) {
                this.accept = s.substring("Accept:".length() + 1);
            } else if (s.startsWith("User-Agent:")) {
                this.agent = s.substring("User-Agent:".length() + 1);

            } else if (s.startsWith("Host:")) {
                this.host = s.substring("Host:".length() + 1);

            } else if (s.startsWith("Accept-Language:")) {
                this.language = s.substring("Accept-Language:".length() + 1);
            } else if (s.startsWith("Accept-Charset:")) {
                this.charset = s.substring("Accept-Charset:".length() + 1);
            } else if (s.startsWith("Accept-Encoding:")) {
                this.encoding = s.substring("Accept-Encoding:".length() + 1);
            } else if (s.startsWith("Connection:")) {
                this.connection = s.substring("Connection:".length() + 1);
            } else if (s.startsWith("Referer:")) {
                this.referer = s.substring("Referer:".length() + 1);
            } else if (s.startsWith("$Referer:")) {
                this.vxreferer = s.substring("$Referer:".length() + 1);
            }
        }
    }

    public String getMethod() {
        return method;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getRequestURL() {
        return requestURL;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public String getHost() {
        return host;
    }

    public String getConnection() {
        return connection;
    }

    public String getAgent() {
        return agent;
    }

    public String getLanguage() {
        return language;
    }

    public String getEncoding() {
        return encoding;
    }

    public String getCharset() {
        return charset;
    }

    public String getAccept() {
        return accept;
    }

    public String getReferer() {
        return referer;
    }

    public String getVxreferer() {
        return vxreferer;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public Socket getSocket() {
        return socket;
    }
}