package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.example.libs.ThreadPool;

public class App {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(
                Integer.parseInt(args[0]));
        System.out.println("Server is listening to the port " 
                + args[0] + "...");
        int threadCount = 4;
        if(args.length > 2) {
            threadCount = Integer.parseInt(args[2]);
        }
        ThreadPool pool = new ThreadPool(threadCount);
        System.out.println("Thread Count = " + threadCount);

        // Register a shutdown hook to stop the server and spawned thread gracefully
        // if forcefully closed
        boolean isUnlimitedConnections = false;
        int connectionLimit;
        if(args.length < 2) {
            isUnlimitedConnections = true;
            connectionLimit = 0;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                try {
                    pool.close();
                    serverSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("Server stopped.");
            }));
        }else {
            connectionLimit = Integer.parseInt(args[1]);
        }

        int numOfConnections = 0;
        while(numOfConnections < connectionLimit || isUnlimitedConnections) {
            Socket clientSocket = serverSocket.accept();
            Runnable lambda = () -> {
                try {
                    handleConnection(clientSocket);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            };
            pool.execute(lambda);
            numOfConnections++;
        }
        System.out.println("Shutting down the server...");
        pool.close();
        serverSocket.close();
    }

    public static void handleConnection(Socket clientSocket) throws Exception {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        String request_line = in.readLine();
        String[] statusAndFilePath = switch(request_line) {
            case "GET / HTTP/1.1" -> 
                new String[]{"HTTP/1.1 200 OK","hello.html"};
            case "GET /sleep HTTP/1.1" -> {
                Thread.sleep(10000);
                yield new String[]{"HTTP/1.1 200 OK","hello.html"};
            }
            default -> 
                new String[]{"HTTP/1.1 404 NOT FOUND","404.html"};
        };
        String contents = new String(Files.readAllBytes(
                    Paths
                    .get("src/main/resources/" + statusAndFilePath[1])));
        String response = 
            statusAndFilePath[0] 
            + "\r\nContent-Length: " 
            + contents.length() 
            + "\r\n\r\n" 
            + contents;
        clientSocket.getOutputStream().write(response.getBytes());
        clientSocket.close();
    }
}
