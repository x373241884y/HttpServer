package com.pack;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HttpServer {

    public static String WEB_ROOT = "webRoot";
    public static int port = 8080;

    public HttpServer() {
    }

    public HttpServer(int port,String root) {
        super();
        this.WEB_ROOT = root;
        this.port = port;
    }

    public void start() throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket(this.port);
        System.out.println("http server running in " + this.port + "...");
        while (true) {
            Socket socket = serverSocket.accept();
            SocketThread st = new SocketThread(socket);
            st.start();
        }
    }
    //test
    public static void main(String[] args) {
        try {
            new HttpServer().start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
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
            String root = new File(HttpServer.WEB_ROOT).getAbsolutePath();
            String filename = root + response.getResourcepath();
            outFile(filename);
            response.flush();
            response.getSocket().shutdownOutput();
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
        }finally {
            System.out.println("thread:" + this.getId() + " destory....");
        }
    }

    private void outFile(String filename) throws IOException {
        System.out.println("reading file start=> "+filename);
        FileInputStream fs = new FileInputStream(filename);
        response.sendHeaders();
        byte[] buffer = new byte[1024];
        int flag;
        while (true) {
            flag = fs.read(buffer);
            String line = new String(buffer,0,flag);
            response.write(line);
            if (flag <1024) {
                break;
            }
        }
        fs.close();
        System.out.println("reading file  end=> "+filename);
    }
}






