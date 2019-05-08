package com.example.dan87.livestreamom;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.LinkedList;
import java.util.Queue;

class PassedObject {
    //<editor-fold defaultstate="collapsed" desc="Variables">
    private Queue<byte[]> framesQueue;
    private Queue<Integer> lenQueue;
    //</editor-fold>

    PassedObject() {
        framesQueue = new LinkedList<>();
        lenQueue = new LinkedList<>();
    }

    synchronized int lenSize() {
        return lenQueue.size();
    }

    synchronized void lenPoll() {
        if(lenQueue.size() > 0){
            lenQueue.poll();
        }
    }

    synchronized int frameSize() {
        return framesQueue.size();
    }

    synchronized void framePoll() {
        if(framesQueue.size() > 0){
            framesQueue.poll();
        }
    }

    synchronized void frameAdd(byte[] data) {
        framesQueue.add(data);
    }

    synchronized void lenAdd(int len) {
        lenQueue.add(len);
    }

    synchronized Bitmap receive(){
        byte[] image = framesQueue.poll();
        int imgLen = lenQueue.poll();
        return BitmapFactory.decodeByteArray(image, 0, imgLen);
    }

    synchronized void removeAll(){
        framesQueue.clear();
        lenQueue.clear();
    }
}
