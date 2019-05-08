package com.example.dan87.livestreamom;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer extends Thread{
    //<editor-fold defaultstate="collapsed" desc="Variables">
    private static final int MAX_BUFFER = 15;
    private ServerSocket mServer;
    private PassedObject passedObject ;
    private MainActivity.ShowImage showImage;
    private MainActivity main;
    boolean runningServer = false;
    boolean runningThread = false;
    //</editor-fold>

    SocketServer(PassedObject passedObject, MainActivity.ShowImage showImage, MainActivity main){
        try {
            this.passedObject = passedObject;
            this.showImage = showImage;
            mServer = new ServerSocket(8888);
            this.main = main;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        super.run();
        try {
            while(true){
                if(runningServer){
                    Socket mSocket = mServer.accept();
                    BufferedInputStream inStream = new BufferedInputStream(mSocket.getInputStream());
                    BufferedOutputStream outStream = new BufferedOutputStream(mSocket.getOutputStream());

                    //Define aux vars
                    boolean receiveImage, receiveNext;
                    int len = 0, msgLen;
                    byte[] lenInBytes = new byte[4];
                    byte[] msgAck = intToBytes(1);
                    ByteArrayOutputStream result = new ByteArrayOutputStream();

                    big_loop:
                    do {
                        if (showImage.ok == 1) {
                            //Initialize aux vars
                            receiveImage = false;
                            receiveNext = false;

                            //Do while Client Sent byte[].length
                            do {
                                //Receive byte[].length
                                do {
                                    msgLen = inStream.read(lenInBytes);
                                    result.write(lenInBytes, 0, msgLen);
                                } while (msgLen != 4);
                                lenInBytes = result.toByteArray();
                                len = convertByteArrayToInt(lenInBytes);
                                if (len == 2) {
                                    break big_loop;
                                }
                                if (len != 0) {
                                    receiveImage = true;
                                    if (passedObject.lenSize() == MAX_BUFFER) {
                                        passedObject.lenPoll();
                                    }
                                    passedObject.lenAdd(len);

                                    //Send Ack
                                    outStream.write(msgAck);
                                    outStream.flush();
                                }
                                result.reset();
                            } while (!receiveImage);

                            //Do while Client Sent byte[]
                            do {
                                //Receive byte[]
                                byte[] data = new byte[len];
                                do {
                                    msgLen = inStream.read(data);
                                    result.write(data, 0, msgLen);
                                } while (result.toByteArray().length < len);
                                data = result.toByteArray();
                                if (data.length == len) {
                                    receiveNext = true;
                                    if (passedObject.frameSize() == MAX_BUFFER) {
                                        passedObject.framePoll();
                                    }
                                    passedObject.frameAdd(data);
                                    synchronized (showImage) {
                                        showImage.ok = 0;
                                        showImage.notify();
                                    }

                                    //Send Ack
                                    outStream.write(msgAck);
                                    outStream.flush();
                                }
                                result.reset();
                            } while (!receiveNext);
                        }
                    } while (runningThread);

                    if(!runningThread) {
                        //message client that server is closing
                        byte[] closingMessage = intToBytes(2);
                        outStream.write(closingMessage);
                        outStream.flush();
                    }else {
                        if (len == 2) {
                            //Stop SocketServer communication
                            runningThread = false;
                            runningServer = false;

                            main.showToast("Client closed connection");
                        }
                    }

                    //closing streams & socket
                    inStream.close();
                    outStream.close();
                    mSocket.close();

                    //resetting passedObject
                    passedObject.removeAll();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] intToBytes(final int data) {
        return new byte[] {
                (byte)((data >> 24) & 0xff),
                (byte)((data >> 16) & 0xff),
                (byte)((data >> 8) & 0xff),
                (byte)((data) & 0xff),
        };
    }

    private static int convertByteArrayToInt(byte[] data) {
        if (data == null || data.length != 4) return 0x0;
        // ----------
        return (
                (0xff & data[0]) << 24  |
                        (0xff & data[1]) << 16  |
                        (0xff & data[2]) << 8   |
                        (0xff & data[3])
        );
    }
}