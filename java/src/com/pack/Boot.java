package com.pack;

/**
 * Created by toor on 16-2-27.
 */
public class Boot {
    public static void main(String[] args) throws Exception {
        System.out.println("For Example:");
        System.out.println("            java -jar fs.jar -port 8080");
        System.out.println("            java -jar fs.jar -port8080");
        System.out.println("            Options:");
        System.out.println("                    -port    local port");

        String port = null;
        String root = null;
        String argsStr = " ";
        argsStr += String.join(" ", args);
        argsStr += " ";  //" -port 8080 -root /home/toor/ ";
        if (argsStr.matches("\\s.*\\-port\\s?\\d+\\s.*")) {
            port = argsStr.replaceAll("\\s.*\\-port\\s?(\\d+)\\s.*", "$1");
        }
        if (port != null) {
            new HttpServer(Integer.parseInt(port), "").start();
        }else {
            new HttpServer().start();
        }
    }
}

