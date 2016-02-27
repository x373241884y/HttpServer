package com.pack;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by toor on 16-2-27.
 */
public class TestRequest {
    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(9090);
            while (true) {
                Socket client = server.accept();
                Request request = new Request(client);
                System.out.println(request.getRequestURL());
                System.out.println(request.getMethod());
                client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
//Result
//localhost:9090/abc.html
//GET
//localhost:9090/test/aaa
//GET
