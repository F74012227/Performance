package com.mmlab.performance.tcp;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class ServerService {

    private static final String TAG = ServerService.class.getName();

    private ServerThread serverThread;
    private ConcurrentHashMap<String, ServerHandler> serverHandlers = new ConcurrentHashMap<>();

    private int SERVER_PORT;

    public ServerService(int port) {
        SERVER_PORT = port;
    }

    public void start() {

    }

    public void stop() {

    }

    public void send() {

    }

    class ServerThread extends Thread {

        private final String TAG = ServerThread.class.getName();

        private ServerSocket serverSocket;

        public ServerThread() {

        }

        public void run() {
            try {
                serverSocket = new ServerSocket(SERVER_PORT);

                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    ServerHandler serverHandler = new ServerHandler(socket);
                    serverHandler.start();
                    serverHandlers.put(socket.getRemoteSocketAddress().toString(), serverHandler);
                    Log.d(TAG, "socket remote address : " + socket.getRemoteSocketAddress().toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

            }
        }

//        public void release(){
//            Iterator<String> iterator = serverHandlers.keySet().iterator();
//            while (iterator.hasNext()) {
//                String  string = iterator.next();
//                Log.d(TAG, "remote socket address : " + member.getRemoteSocketAddress().toString());
//                if (member.getRemoteSocketAddress().toString().equals(host))
//                    try {
//                        ObjectOutputStream writer = members.get(member);
//                        writer.write(1);
//                        writer.writeObject(new Package(Package.TAG_COMMAND, Package.TYPE_NONE, "", Utils.stringToByteArray("request video" + " " + String.valueOf(mediaLength) + " " + remoteUri)));
//                    } catch (IOException e) {
//                        members.remove(member);
//                        e.printStackTrace();
//                    }
//            }
//        }

        public void interrupt() {
            super.interrupt();

            try {
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class ServerHandler extends Thread {

        private final String TAG = ServerHandler.class.getName();

        private Socket socket;

        private DataInputStream dataInputStream;
        private DataOutputStream dataOutputStream;

        public ServerHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {

            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                while (!Thread.currentThread().isInterrupted()) {
                    int byteNumber = dataInputStream.readInt();
                    byte[] bytes = new byte[byteNumber];
                    dataInputStream.readFully(bytes);

                    Log.d(TAG, "receive : " + new String(bytes));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    dataInputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    dataOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        public void interrupt() {
            super.interrupt();

            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
