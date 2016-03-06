package com.mmlab.performance;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.onionnetworks.fec.FECCode;
import com.onionnetworks.fec.FECCodeFactory;
import com.onionnetworks.util.Buffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Created by mmlab on 2016/2/26.
 */
public class BroadcastService {

    private static final String TAG = BroadcastService.class.getName();
    private static final int RECEIVE_ACTION = 0;
    private static final int FINISH_ACTION = 1;

    private Context context;

    private int BROADCAST_PORT = 65303;
    private static final int DATAGRAM_MAX_SIZE = 546;
    private int CHUNCK_SIZE = 10240;

    private static final int MAX_SESSION_NUMBER = 255;
    private static final int MAX_GROUP_NUMBER = 255;
    private static final int MAX_ID_NUMBER = 255;

    private static final int MAX_PACKETS = 255 * 255;
    private static final int SESSION_START = 128;
    private static final int SESSION_END = 64;

    private static final int HEADER_SIZE = 34;
    private int DATA_SIZE = DATAGRAM_MAX_SIZE - HEADER_SIZE;

    private ArrayList<Sender> senders = new ArrayList<>();
    private Receiver receiver;

    private HandlerThread pHandlerThread;
    private Handler pHandler;
    private Handler mHandler;
    private WifiManager.MulticastLock multicastLock;

    private OnFinishedListener onFinishedListener;

    public void setOnFinishedListener(OnFinishedListener onFinishedListener) {
        this.onFinishedListener = onFinishedListener;
    }

    interface OnFinishedListener {
        void onReceived();

        void onTransmitted();

        void onFinished();
    }

    public BroadcastService(Context context) {
        this.context = context;
    }

    private class SenderThread extends AsyncTask<Void, Void, Void> {

        private final String TAG = SenderThread.class.getName();

        protected Void doInBackground(Void... params) {

            return null;
        }
    }

    public void setFECEnabled(boolean enabled) {
        this.isFEC = enabled;
    }

    public boolean isFEC = false;
    int currentSession = -1;
    int currentGroup = -1;
    int currentId = -1;
    int slicesStored = 0;
    boolean endFirst = true;
    int[] slicesCol = null;
    int[] sliceConsist = null;
    byte[] imageData = null;
    boolean sessionAvailable = false;
    int recvK;
    int recvN;
    boolean finished = false;

    public void start() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock("multicast.test");
        multicastLock.acquire();

        if (pHandlerThread == null) {
            pHandlerThread = new HandlerThread(TAG);
            pHandlerThread.start();
        }

        if (pHandler == null) {
            pHandler = new Handler(pHandlerThread.getLooper()) {
                public void handleMessage(Message msg) {
                    int what = msg.what;
                    Object object = msg.obj;
                    switch (what) {
                        case RECEIVE_ACTION:
                            DatagramPacket datagramPacket = (DatagramPacket) object;
                            byte[] data = datagramPacket.getData();

                            short session = (short) (data[1] & 0xff);
                            int slices = (int) ((data[2] & 0xff) << 8 | (data[3] & 0xff));
                            int maxPacketSize = (int) ((data[4] & 0xff) << 8 | (data[5] & 0xff)); // mask the sign bit
                            int slice = (int) ((data[6] & 0xff) << 8 | (data[7] & 0xff)); // mask the sign bit
                            int size = (int) ((data[8] & 0xff) << 8 | (data[9] & 0xff)); // mask the sign bit
                            short id = (short) (data[10] & 0xff);
                            short sets = (short) (data[11] & 0xff);
                            short set = (short) (data[12] & 0xff);
                            int nameLength = (int) (data[13] & 0xff);

                            Log.d(TAG, "id : " + id);
                            Log.d(TAG, "sets : " + sets);
                            Log.d(TAG, "set : " + set);
                            Log.d(TAG, "session : " + session);
                            Log.d(TAG, "slices : " + slices);
                            Log.d(TAG, "maxPacketSize : " + maxPacketSize);
                            Log.d(TAG, "slice : " + slice);
                            Log.d(TAG, "size : " + size);
                            Log.d(TAG, "nameLength : " + nameLength);

                            String name = new String(Arrays.copyOfRange(data, 14, 14 + nameLength));

                            Log.d(TAG, "name : " + name);

                            currentId = id;

                            if (id != idNumber && idNumber != -1 && !finished) {
                                finished = true;
                                mHandler.obtainMessage(FINISH_ACTION).sendToTarget();
                            }

                            if (session != currentSession) {
                                finished = false;
                                currentSession = session;
                                recvK = slices;
                                if (isFEC)
                                    recvN = recvK * 2;
                                else
                                    recvN = recvK;
                                slicesStored = 0;
                                imageData = new byte[slices * maxPacketSize];
                                slicesCol = new int[slices];
                                sliceConsist = new int[recvN];
                                sessionAvailable = true;
                            }

                            if (sessionAvailable && session == currentSession && slicesStored < recvK) {
                                if (slicesCol != null && sliceConsist[slice] == 0) {
                                    sliceConsist[slice] = 1;
                                    slicesCol[slicesStored] = slice;
                                    if (isFEC) {
                                        System.arraycopy(data, HEADER_SIZE, imageData, slicesStored * maxPacketSize, size);
                                    } else {
                                        System.arraycopy(data, HEADER_SIZE, imageData, slice * maxPacketSize, size);
                                    }
                                    slicesStored++;

                                    if (slicesStored == recvK) {
                                        Log.d(TAG, "We did it!!!");
                                        if (set == (sets - 1)) {
                                            finished = true;
                                            mHandler.obtainMessage(FINISH_ACTION).sendToTarget();
                                        }

                                        if (!name.contains("txt")) {
                                            if (!isFEC)
                                                norRecv(name, imageData, slicesCol);
                                            else {
                                                fecRecv(name, imageData, slicesCol);
                                            }
                                        }
                                        mHandler.obtainMessage(RECEIVE_ACTION).sendToTarget();
                                    }
                                }
                            }

                            break;
                        default:
                    }
                    super.handleMessage(msg);
                }
            };
        }

        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper()) {
                public void handleMessage(Message msg) {
                    int what = msg.what;
                    Object object = msg.obj;
                    switch (what) {
                        case RECEIVE_ACTION:
                            if (onFinishedListener != null)
                                onFinishedListener.onReceived();
                            break;
                        case FINISH_ACTION:
                            if (onFinishedListener != null)
                                onFinishedListener.onFinished();
                            break;
                        default:
                    }
                    super.handleMessage(msg);
                }
            };
        }
    }

    public void stop() {
        if (mHandler != null)
            mHandler.removeCallbacksAndMessages(null);

        if (pHandler != null)
            pHandler.removeCallbacksAndMessages(null);

        if (pHandlerThread != null)
            pHandlerThread.quit();

        mHandler = null;
        pHandler = null;
        pHandlerThread = null;

        multicastLock.release();
    }

    public void send(String fileName, File file) {
        Sender sender = new Sender(file);
        sender.setFileName(fileName);
        senders.add(sender);
        sender.execute();
    }

    public void send(String fileName, String string) {
        Sender sender = new Sender(string);
        sender.setFileName(fileName);
        senders.add(sender);
        sender.execute();
    }

    public void send() {
        Sender sender = new Sender("123");
        senders.add(sender);
        sender.execute();
    }

    public void receive() {
        if (receiver == null) {
            receiver = new Receiver();
            receiver.start();
        } else if (!receiver.isAlive()) {
            receiver.interrupt();
            receiver = new Receiver();
            receiver.start();
        }
    }

    private int idNumber;
    private int sessionNumber;
    private int groupNumber;
    private int groups;

    class Sender extends AsyncTask<Void, Void, Void> {

        private final String TAG = Sender.class.getName();

        private DatagramSocket datagramSocket = null;

        private InetAddress inetAddress = null;

        private Object object;

        private String fileName;

        public Sender(Object object) {
            this.object = object;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        protected Void doInBackground(Void... voids) {

            try {
                datagramSocket = new DatagramSocket(null);
                datagramSocket.setReuseAddress(true);
                datagramSocket.bind(new InetSocketAddress(BROADCAST_PORT));
                datagramSocket.setBroadcast(true);

                String broadcastAddress = getBroadcast();
                Log.d(TAG, broadcastAddress);

                inetAddress = InetAddress.getByName(broadcastAddress);

                send();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (datagramSocket != null) {
                    datagramSocket.close();
                }
            }
            return null;
        }

        protected void onPostExecute(Void aVoid) {
//            super.onPostExecute(aVoid);
            if (onFinishedListener != null) {
                onFinishedListener.onTransmitted();
            }
        }

        public void interrupt() {

            if (datagramSocket != null) {
                datagramSocket.close();
            }

            cancel(true);
        }

        private void send() {
            groupNumber = 0;
            try {
                String sUrl = "http://deh.csie.ncku.edu.tw/deh/functions/pic_add_watermark.php?src=player_pictures/20150305182420_455_.jpg";
                fileName = sUrl.substring(sUrl.lastIndexOf("/") + 1, sUrl.length());
                Log.d(TAG, "fileName : " + fileName);
                fileName = "file.jpg";
                URL url = new URL(sUrl);
                URLConnection conn = url.openConnection();
                conn.connect();

                InputStream is = conn.getInputStream();
                int len = CHUNCK_SIZE, rlen;
                byte[] buffer = new byte[CHUNCK_SIZE];
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                while ((rlen = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, rlen);
                }
                byte[] data = bos.toByteArray();
                int dadada = data.length;

                groups = (int) Math.ceil((float) dadada / (float) CHUNCK_SIZE);
                Log.d(TAG, "length : " + dadada + "  groups:" + groups);
                for (int i = 0; i < groups; i++) {
                    Log.d(TAG, "current group :" + i);
                    int index = i * CHUNCK_SIZE;
                    if (i == (groups - 1)) {
                        if (isFEC)
                            fecSendBytes(Arrays.copyOfRange(data, index, index + dadada - i * CHUNCK_SIZE));
                        else
                            norSendBytes(Arrays.copyOfRange(data, index, index + dadada - i * CHUNCK_SIZE));
                    } else {
                        if (isFEC) {
                             fecSendBytes(Arrays.copyOfRange(data, index, index + CHUNCK_SIZE));
                        } else
                            norSendBytes(Arrays.copyOfRange(data, index, index + CHUNCK_SIZE));
                    }
                    groupNumber++;
                }
            } catch (Exception e) {
                Log.d(TAG, e.toString(), e);
            }


            /*if (object instanceof File) {
                File file = (File) object;
                FileInputStream fileInputStream = null;

                try {
                    fileInputStream = new FileInputStream(file);
                    groups = (int) Math.ceil((float) file.length() / (float) CHUNCK_SIZE);

                    byte[] bytes = new byte[CHUNCK_SIZE];
                    int readBytes;

                    while ((readBytes = fileInputStream.read(bytes, 0, CHUNCK_SIZE)) != -1) {
                        if (isFEC)
                            fecSendBytes(bytes);
                        else
                            norSendBytes(bytes);
                        groupNumber++;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (object instanceof String) {
                String string = (String) object;
                byte[] data = string.getBytes();
                int dataLength = data.length;
                int k = (int) Math.ceil(dataLength / (float) CHUNCK_SIZE);

                groups = k;

                for (int i = 0; i < k; ++i) {
                    if (i == (k - 1)) {
                        if (isFEC)
                            fecSendBytes(Arrays.copyOfRange(data, i * CHUNCK_SIZE, dataLength - i * CHUNCK_SIZE));
                        else
                            norSendBytes(Arrays.copyOfRange(data, i * CHUNCK_SIZE, dataLength - i * CHUNCK_SIZE));
                    } else {
                        if (isFEC)
                            fecSendBytes(Arrays.copyOfRange(data, i * CHUNCK_SIZE, CHUNCK_SIZE));
                        else
                            norSendBytes(Arrays.copyOfRange(data, i * CHUNCK_SIZE, CHUNCK_SIZE));
                    }
                    groupNumber++;
                }
            }*/

            idNumber = idNumber < MAX_ID_NUMBER ? ++idNumber : 0;
        }

        private void norSendBytes(byte[] bytes) {
            int bytesLength = bytes.length;
            int k = (int) Math.ceil(bytesLength / (float) DATA_SIZE);

//            byte[] source = new byte[k * DATA_SIZE];
//            System.arraycopy(bytes, 0, source, 0, bytesLength);
//            int sourceLength = source.length;

            DatagramPacket sendPacket;
            for (int i = 0; i < k; i++) {

                int flags = 0;
                flags = i == 0 ? flags | SESSION_START : flags;
                flags = (i + 1) * DATA_SIZE > bytesLength ? flags | SESSION_END : flags;

                int size = (flags & SESSION_END) != SESSION_END ? DATA_SIZE : bytesLength - i * DATA_SIZE;
                byte[] sendData = new byte[HEADER_SIZE + DATA_SIZE];
                sendData[0] = (byte) flags;
                sendData[1] = (byte) sessionNumber;
                sendData[2] = (byte) (k >> 8);
                sendData[3] = (byte) k;
                sendData[4] = (byte) (DATA_SIZE >> 8);
                sendData[5] = (byte) DATA_SIZE;
                sendData[6] = (byte) (i >> 8);
                sendData[7] = (byte) i;
                sendData[8] = (byte) (size >> 8);
                sendData[9] = (byte) size;
                sendData[10] = (byte) idNumber;
                sendData[11] = (byte) groups;
                sendData[12] = (byte) groupNumber;

                byte[] txtBytes = fileName.getBytes();
                sendData[13] = (byte) txtBytes.length;
                System.arraycopy(txtBytes, 0, sendData, 14, txtBytes.length);


                System.arraycopy(bytes, i * DATA_SIZE, sendData, HEADER_SIZE, size);

                sendPacket = new DatagramPacket(sendData, DATAGRAM_MAX_SIZE, inetAddress, BROADCAST_PORT);

                try {
                    datagramSocket.send(sendPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            sessionNumber = sessionNumber < MAX_SESSION_NUMBER ? ++sessionNumber : 0;
        }

        private void fecSendBytes(byte[] data) {
            int dataLength = data.length;
            int k = (int) Math.ceil(dataLength / (float) DATA_SIZE);
            int n = k * 2;

            Log.d(TAG, "TTTTTTTTTTTTTTTTTTTTTTT  : " + k + "      " + n + " " + dataLength);

            byte[] source = new byte[k * DATA_SIZE]; //this is our source file
            Arrays.fill(source, (byte) 0);
            System.arraycopy(data, 0, source, 0, dataLength);

            //NOTE: The source needs to split into k*packetsize sections
            //So if your file is not of the right size you need to split
            //it into groups.  The final group may be less than
            //k*packetsize, in which case you must pad it until you read
            //k*packetsize.  And send the length of the file so that you
            //know where to cut it once decoded.

            //this will hold the encoded file
            byte[] repair = new byte[n * DATA_SIZE];

            //These buffers allow us to put our data in them they
            //reference a packet length of the file (or at least will once
            //we fill them)
            Buffer[] sourceBuffer = new Buffer[k];
            Buffer[] repairBuffer = new Buffer[n];

            for (int i = 0; i < sourceBuffer.length; i++)
                sourceBuffer[i] = new Buffer(source, i * DATA_SIZE, DATA_SIZE);

            for (int i = 0; i < repairBuffer.length; i++)
                repairBuffer[i] = new Buffer(repair, i * DATA_SIZE, DATA_SIZE);

            //When sending the data you must identify what it's index was.
            //Will be shown and explained later
            int[] repairIndex = new int[n];

            for (int i = 0; i < repairIndex.length; i++)
                repairIndex[i] = i;

            //create our fec code
            FECCode fec = FECCodeFactory.getDefault(context).createFECCode(k, n);

            //encode the data
            fec.encode(sourceBuffer, repairBuffer, repairIndex);
            //encoded data is now contained in the repairBuffer/repair byte array

            //From here you can send each 'packet' of the encoded data, along with
            //what repairIndex it has.  Also include the group number if you had to
            //split the file

            int MAX_PACKETS = 255 * 255;
            int SESSION_START = 128;
            int SESSION_END = 64;
            dataLength = n * DATA_SIZE;

            DatagramPacket segmentPacket;
            for (int i = 0; i < n; i++) {

                int flags = 0;
                flags = i == 0 ? flags | SESSION_START : flags;
                flags = (i + 1) * DATA_SIZE > dataLength ? flags | SESSION_END : flags;

                int size = (flags & SESSION_END) != SESSION_END ? DATA_SIZE : dataLength - i * DATA_SIZE;
                byte[] head = new byte[HEADER_SIZE + size];
                head[0] = (byte) flags;
                head[1] = (byte) sessionNumber;
                head[2] = (byte) (k >> 8);
                head[3] = (byte) k;
                head[4] = (byte) (DATA_SIZE >> 8);
                head[5] = (byte) DATA_SIZE;
                head[6] = (byte) (i >> 8);
                head[7] = (byte) i;
                head[8] = (byte) (size >> 8);
                head[9] = (byte) size;
                head[10] = (byte) idNumber;
                head[11] = (byte) groups;
                head[12] = (byte) groupNumber;

                byte[] txtBytes = fileName.getBytes();
                head[13] = (byte) txtBytes.length;
                System.arraycopy(txtBytes, 0, head, 14, txtBytes.length);

                System.arraycopy(repairBuffer[i].getBytes(), 0, head, HEADER_SIZE, repairBuffer[i].getBytes().length);

                // int index = i * DATAGRAM_MAX_SIZE;

                if (i == (n - 1)) {
                    segmentPacket = new DatagramPacket(head, DATAGRAM_MAX_SIZE, inetAddress, BROADCAST_PORT);
                } else {
                    segmentPacket = new DatagramPacket(head, DATAGRAM_MAX_SIZE, inetAddress, BROADCAST_PORT);
                }

                // suspend=generator.nextDouble()*10*1;
                try {
                    datagramSocket.send(segmentPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Thread.sleep((int)suspend);
            }
            Log.d(TAG, "groups : " + groups + "groupNumber : " + groupNumber + "chunk size : " + n + " Ori : " + k);

        /* Increase session number */
            sessionNumber = sessionNumber < MAX_SESSION_NUMBER ? ++sessionNumber : 0;
        }
    }

    class Receiver extends Thread {

        private final String TAG = Receiver.class.getName();

        private DatagramSocket datagramSocket = null;

        public Receiver() {

        }

        public void run() {
            try {
                datagramSocket = new DatagramSocket(null);
                datagramSocket.setReuseAddress(true);
                datagramSocket.bind(new InetSocketAddress(BROADCAST_PORT));
                datagramSocket.setBroadcast(true);

                String broadcastAddress = getBroadcast();
                Log.d(TAG, "broadcastAddress : " + broadcastAddress);

                InetAddress inetAddress = InetAddress.getByName(broadcastAddress);


                while (!Thread.currentThread().isInterrupted()) {
                    byte[] receiveData = new byte[DATAGRAM_MAX_SIZE];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, DATAGRAM_MAX_SIZE);
                    receivePacket.setLength(DATAGRAM_MAX_SIZE);
                    datagramSocket.receive(receivePacket);
                    Log.d(TAG, "receive the message");
                    pHandler.obtainMessage(RECEIVE_ACTION, receivePacket).sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (datagramSocket != null) {
                    datagramSocket.close();
                }
            }
        }

        public void interrupt() {
            super.interrupt();

            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }
    }

    public static String getBroadcast() throws SocketException {
        System.setProperty("java.net.preferIPv4Stack", "true");
        for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements(); ) {
            NetworkInterface ni = niEnum.nextElement();

            if (!ni.isLoopback()) {
                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                    if (interfaceAddress.getBroadcast() != null) {
                        return interfaceAddress.getBroadcast().toString().substring(1);
                    }
                }
            }
        }
        return null;
    }

    public void norRecv(String filename, byte[] ddd, int[] dddIndex) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(Environment.getExternalStorageDirectory() + File.separator + currentId + ".mp4", true);
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.write(ddd, 0, ddd.length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.flush();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    fileOutputStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public void fecRecv(String fileName, byte[] ddd, int[] dddIndex) {
//create our fec code
        FECCode fec = FECCodeFactory.getDefault(context).createFECCode(recvK, recvN);

        //We only need to store k, packets received
        //Don't forget we need the index value for each packet too
        Buffer[] receiverBuffer = new Buffer[recvK];
        int[] receiverIndex = new int[recvK];

        //this will store the received packets to be decoded
        byte[] received = new byte[recvK * DATA_SIZE];

        System.arraycopy(ddd, 0, received, 0, recvK * DATA_SIZE);
        System.arraycopy(dddIndex, 0, receiverIndex, 0, recvK);

        //create our Buffers for the encoded data
        for (int i = 0; i < recvK; i++)
            receiverBuffer[i] = new Buffer(received, i * DATA_SIZE, DATA_SIZE);

        //finally we can decode
        fec.decode(receiverBuffer, receiverIndex);

//        Bitmap bitmap = BitmapFactory.decodeByteArray(received, 0, received.length);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(Environment.getExternalStorageDirectory() + File.separator + fileName, true);
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.write(received, 0, received.length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.flush();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    fileOutputStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
