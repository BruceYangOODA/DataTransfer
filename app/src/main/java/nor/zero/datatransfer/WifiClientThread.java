package nor.zero.datatransfer;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static nor.zero.datatransfer.DeviceListFragment.getDevice;

public class WifiClientThread extends Thread {

    Handler handler;
    MainActivity mainActivity;
    Socket socket;
    InputStream inputStream;
    OutputStream outputStream;

    public WifiClientThread(MainActivity mainActivity){
        this.mainActivity = mainActivity;
        handler = mainActivity.handler;
    }

    @Override
    public void run() {
        int length = 0;
        byte[] buffer = new byte[Constants.READER_LENGTH];
        try {
            socket = new Socket(mainActivity.hostAddress,Constants.WIFI_PORT);

        } catch (IOException e) {
            Log.v("aaa","socket 出問題啦");
        }
        while (true){
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                length = inputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 傳進來的是有效訊息
            if(length>Constants.CHECK_LENGTH+Constants.CHAT_NAME_LENGTH)
            {
                byte[] checkPact = new byte[Constants.CHECK_LENGTH];
                // 讀進來的資料後6個byte是確認碼
                System.arraycopy(buffer,length-Constants.CHECK_LENGTH,checkPact,0,Constants.CHECK_LENGTH);
                String checkStr = new String(checkPact);
                // 確認碼確認訊息是哪一種類型 DATA_TYPE[0]是文字訊息
                if(checkStr.equals(Constants.DATA_TYPE[0]))
                    readMessage(buffer,length);
/*
                try {
                    String str = "整合正能量整合正能量整合正能量整合正能量*#401!";
                    byte[] sendBy = str.getBytes();
                    //DataOutputStream dos = new DataOutputStream(outputStream);
                    //dos.writeUTF(str);
                    DataOutputStream dos = new DataOutputStream(outputStream);
                    // dos.write(sendBy,0,sendBy.length);
                    // dos.flush();
                    dos.writeUTF(str);

                    Log.v("aaa","送出資料:" +sendBy.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
            }

        }

    }

    private void readMessage(byte[] buffer,int length){
        byte[] nameByte = new byte[Constants.CHAT_NAME_LENGTH];
        int msgLength = length-Constants.CHAT_NAME_LENGTH-Constants.CHECK_LENGTH;
        byte[] msgByte = new byte[msgLength];
        System.arraycopy(buffer,0,nameByte,0,Constants.CHAT_NAME_LENGTH);   //名字byte陣列複製
        System.arraycopy(buffer,Constants.CHAT_NAME_LENGTH,msgByte,0,msgLength);    //內容byte陣列複製

        // 去除名字陣列後方預留空間的空byte
        int trimZero = 0;
        for(int i=0;i<nameByte.length;i++){
            if(nameByte[i]==0){
                trimZero = i;
                break;
            }
        }
        try {
            String msgName = new String(nameByte,0,trimZero);  //訊息的名字
            String msgContent = new String(msgByte);  //訊息的內容
            Message msg = handler.obtainMessage(Constants.DATA_CHAT);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.CHAT_MSG_NAME,msgName);
            bundle.putString(Constants.CHAT_MSG_CONTENT,msgContent);
            msg.setData(bundle);
            handler.sendMessage(msg);   //傳給MainActivity 新增ChatFragment 內容
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write(String chatMessage){
        byte[] byteChat = chatMessage.getBytes();
        int lengthChat = byteChat.length;
        // 不接受長文,超過900byte的不處理
        if(lengthChat<Constants.SENDER_LENGTH){
            String chatName = getDevice().deviceName; //DeviceListFragment 取得本機的名字
            int lengthName = Constants.CHAT_NAME_LENGTH;
            byte[] byteName = new byte[lengthName];
            byte[] byteTemp = chatName.getBytes();
            System.arraycopy(byteTemp,0,byteName,0,byteTemp.length);
            byte[] byteCheckCode = Constants.DATA_TYPE[0].getBytes(); //確認碼 文字訊息
            int lengthCheckCode = byteCheckCode.length;
            int totalLength = lengthName + lengthChat + lengthCheckCode; //組裝訊息的總長度 名字+內容+確認碼
            byte[] byteSend = new byte[totalLength];
            System.arraycopy(byteName,0,byteSend,0,lengthName);
            System.arraycopy(byteChat,0,byteSend,lengthName,lengthChat);
            System.arraycopy(byteCheckCode,0,byteSend,lengthName+lengthChat,lengthCheckCode);
            String temp = null;

            temp = new String(byteSend);
            DataOutputStream dos = new DataOutputStream(outputStream);
            try {
                //dos.writeUTF(temp);
                dos.write(byteSend);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.v("aaa","送出資料:" +byteSend.length);

/*
            Log.v("aaa","send: "+temp);
            Log.v("aaa","lengthName"+lengthName);
            Log.v("aaa","lengthChat"+lengthChat);
            Log.v("aaa","lengthCheckCode"+lengthCheckCode);
            Log.v("aaa","totalLength"+totalLength);
            Log.v("aaa","byteSend"+byteSend.length);*/
/*
            try {
                outputStream.write(byteSend);
                Log.v("aaa","Send outputStream");
            } catch (IOException e) {
               Log.v("aaa","outputStream wrong");
            }*/

        }
    }

    public void cancel(){
        try {
            socket.close();
            try {
                this.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }




}
