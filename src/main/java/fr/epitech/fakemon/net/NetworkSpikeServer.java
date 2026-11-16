package fr.epitech.fakemon.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkSpikeServer {

    public static void main(String[] hello) throws IOException {
        ServerSocket host = new ServerSocket(5000);
        Socket client = host.accept();
        System.out.println("Connection du client reçue");

        InputStream flow = client.getInputStream();
        InputStreamReader texte = new InputStreamReader(flow);
        BufferedReader reader = new BufferedReader(texte);
        String message = reader.readLine();
        System.out.println(message);
        client.close();
        host.close();
    }
}
