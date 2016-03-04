package com.pack;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HttpServer {

    public static String WEB_ROOT;
    public static int port = 8080;
    public HttpServer() {
        this.WEB_ROOT=new File("webRoot").getAbsolutePath();
    }

    public HttpServer(int port, String root) {
        super();
        this.WEB_ROOT=new File(root).getAbsolutePath();
        this.port = port;
    }

    public void start() throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket(this.port);
        System.out.println("http server running in " + this.port + "...");
        System.out.println("server work in path:"+this.WEB_ROOT);
        while (true) {
            Socket socket = serverSocket.accept();
            SocketThread st = new SocketThread(socket);
            st.start();
        }
    }

    //test
/*    public static void main(String[] args) {
        try {
            new HttpServer().start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }*/
}

class SocketThread extends Thread {
    Request request;
    Response response;

    public SocketThread(Socket socket) throws IOException {
        this.request = new Request(socket);
        this.response = new Response(socket, request);
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "  " + request.getMethod() + " " + request.getRequestURI());
    }

    @Override
    public void run() {
        try {
            String filename = HttpServer.WEB_ROOT + response.getResourcepath();
            File file = new File(filename);
            if (file.isFile()) {
                outFile(file);
            } else if(file.isDirectory()) {
                respFilelist(file);
            }else{
                response.error("500 Bad Request","糟糕的请求");
            }
            response.flush();
            response.end();

        } catch (FileNotFoundException e) {
            try {
                response.error("404 Not Found", "页面没有找到!");
                response.end();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("thread:" + this.getId() + " destory....");
        }
    }

    private void respFilelist(File file) throws IOException {
        String[] filenames = file.list();
        String directory =file.getAbsolutePath();
        String current = directory.substring(HttpServer.WEB_ROOT.length())+"/";
        response.sendHeaders();
        response.flush();
        response.write("<html><head><title>Index of:/</title></head><body><h1>Directory:" + current + "</h1><table border='0'><tbody>");
        response.write("<tr><td><a href='../'>Parent Directory</a></td><td></td><td></td></tr>");
        if(filenames!=null){
            for (int i = 0; i < filenames.length; i++) {
                File temp = new File(file.getAbsolutePath() + File.separator + filenames[i]);
                String href = filenames[i];
                if(temp.isFile()){
                    href=current+href;
                }else if(temp.isDirectory()&&href!="/"){
                    href=current+href+"/";
                }
                long size = temp.length();    //   大小   bytes
                String modify = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(temp.lastModified());    //   修改时间
                response.write("<tr><td><a href='" + href + "'>" + filenames[i] + "</a></td><td style='text-align:right'>" + size + "  bytes</td><td> " + modify + "</td></tr>");
            }
        }
        response.write("</tbody></table></body></html>");
    }

    private void outFile(File file) throws IOException {
        FileInputStream fs = new FileInputStream(file);
        response.sendHeaders();
        response.flush();
        byte[] buffer = new byte[1024];
        int flag;
        while (true) {
            flag = fs.read(buffer);
//            String line = new String(buffer,0,flag);
//            response.write(line); //这样处理对二进制文件不支持
            if(flag!=-1){
                response.getOutputStream().write(buffer, 0, flag);
                if (flag < 1024) {
                    break;
                }
            }else{
                break;
            }

        }
        fs.close();
    }
}