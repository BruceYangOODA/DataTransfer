package nor.zero.datatransfer;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static nor.zero.datatransfer.Constants.*;
import static nor.zero.datatransfer.DeviceDetailFragment.etNickName;
import static nor.zero.datatransfer.DeviceListFragment.getDevice;

public class WifiClientThread extends Thread {

    Handler handler;
    MainActivity mainActivity;
    Socket socket = null;
    InputStream inputStream;
    OutputStream outputStream;
    private static int fileTotalLengthCheck = 0;
    private boolean isConnecting = true;
    private boolean isConnected = false;
    private ByteArrayInputStream byteArrayInputStream = null;
    static ByteArrayOutputStream byteArrayOutputStream = null;

    public WifiClientThread(MainActivity mainActivity){
        this.mainActivity = mainActivity;
        handler = mainActivity.handler;
    }

    @Override
    public void run() {
        int length = 0;
        byte[] buffer = new byte[Constants.READER_LENGTH];
        while (isConnecting){
            try {
                socket = new Socket(mainActivity.hostAddress,Constants.WIFI_PORT);
                if(socket != null){     // socket連接成功
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                    isConnecting = false;
                    isConnected = true;
                }

            } catch (IOException e) {
                Log.v("aaa","socket 出問題啦");
            }
        }
        while (isConnected){
            try {
                length = inputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 傳進來的是有效訊息
            if(length>=DATA_CHECK_LENGTH + CHECK_CODE_LENGTH)
            {
                byte[] checkPact = new byte[CHECK_CODE_LENGTH];
                // 讀進來的資料後6個byte是確認碼
                System.arraycopy(buffer,length-CHECK_CODE_LENGTH,checkPact,0,CHECK_CODE_LENGTH);
                String checkStr = new String(checkPact);
                Log.v("aaa","checkPact: "+checkStr);
                Log.v("aaa","length: "+length);
                Log.v("aaa","buffer: "+buffer.length);
                // 確認碼確認訊息是哪一種類型 DATA_TYPE[0]是文字訊息
                //if(checkStr.equals(Constants.DATA_TYPE_MESSAGE))
             //       readMessage(buffer,length);
                switch (checkStr){
                    case DATA_TYPE_MESSAGE:
                        readMessage(buffer,length);
                        break;
                    case DATA_TRANS_START:
                        readFileDetail(buffer,length);
                        break;
                    case DATA_TRANS_CONTINUED:
                        receiveData(buffer,length);
                        break;
                    case DATA_TRANS_END:
                        createFile();
                        break;
                    default:
                        break;
                }
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
        byte[] nameByte = new byte[DATA_CHECK_LENGTH];
        int msgLength = length - DATA_CHECK_LENGTH - CHECK_CODE_LENGTH;
        byte[] msgByte = new byte[msgLength];
        System.arraycopy(buffer,0,nameByte,0,DATA_CHECK_LENGTH);   //名字byte陣列複製
        System.arraycopy(buffer,DATA_CHECK_LENGTH,msgByte,0,msgLength);    //內容byte陣列複製

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
            String nickName = etNickName.getText().toString();
            if(nickName.equals(""))
                nickName = getDevice().deviceName; //DeviceListFragment 取得本機的名字
            int lengthName = DATA_CHECK_LENGTH;
            byte[] byteName = new byte[lengthName];
            byte[] byteTemp = nickName.getBytes();
            System.arraycopy(byteTemp,0,byteName,0,byteTemp.length);
            byte[] byteCheckCode = Constants.DATA_TYPE_MESSAGE.getBytes(); //確認碼 文字訊息
            int lengthCheckCode = byteCheckCode.length;
            int totalLength = lengthName + lengthChat + lengthCheckCode; //組裝訊息的總長度 名字+內容+確認碼
            byte[] byteSend = new byte[totalLength];
            System.arraycopy(byteName,0,byteSend,0,lengthName);
            System.arraycopy(byteChat,0,byteSend,lengthName,lengthChat);
            System.arraycopy(byteCheckCode,0,byteSend,lengthName+lengthChat,lengthCheckCode);

          //  DataOutputStream dos = new DataOutputStream(outputStream);
            try {
                outputStream.write(byteSend);
                //dos.writeUTF(temp);
              //  dos.write(byteSend);
             //   dos.flush();
                outputStream.flush();
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

    public synchronized void transfer(String filePath,String fileName){
        //先傳出檔案檔名與檔案長度
        FileInputStream fileInputStream = null;
        int fileTotalLength = 0;
        int length =-1;
        byte[] byteBuffer = new byte[SENDER_LENGTH];
        byte[] byteCheckCode = DATA_TRANS_CONTINUED.getBytes();
        byte[] byteSend;
        try {
            fileInputStream = new FileInputStream((filePath+"/"+fileName));
            if(fileInputStream != null)
                fileTotalLength = fileInputStream.available();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.v("aaa","fileInputStream"+fileTotalLength);
        if(fileTotalLength>0){  //有讀到檔案內容
            startTransfer(fileName,fileTotalLength);
            try{
                while ((length=fileInputStream.read(byteBuffer))!= -1){
                    // 前面DATA_CHECK_LENGTH 留白防止檔案最後byte[] 不夠大，被接收方判定是無效資料
                    sleep(50);
                    byteSend = new byte[DATA_CHECK_LENGTH+length+CHECK_CODE_LENGTH];
                    System.arraycopy(byteBuffer,0,byteSend,DATA_CHECK_LENGTH,length);
                    System.arraycopy(byteCheckCode,0,byteSend,
                            DATA_CHECK_LENGTH+length,CHECK_CODE_LENGTH);

                    //todo
                    byte[] byteCheckPac = new byte[6];
                    System.arraycopy(byteSend,byteSend.length-6,byteCheckPac,0,6);
                    String check = new String(byteCheckCode);
                    Log.v("aaa","checkPack " +check);

                    outputStream.write(byteSend);
                    Log.v("aaa","傳檔大小"+byteSend.length);
                   // outputStream.flush();
                }
                // 通知對方機器傳檔結束
                byteSend = new byte[DATA_CHECK_LENGTH+CHECK_CODE_LENGTH];
                byteCheckCode = DATA_TRANS_END.getBytes();
                System.arraycopy(byteCheckCode,0,byteSend,DATA_CHECK_LENGTH,CHECK_CODE_LENGTH);
                outputStream.write(byteSend);
                Log.v("aaa","檔案傳送結束");
               // outputStream.flush();
            }
            catch (Exception e){
                Log.v("aaa","錯誤"+e.getMessage());
                e.printStackTrace();
            }
        }
    }

    //先傳出檔案檔名與檔案長度
    private synchronized void startTransfer(String fileName,int fileTotalLength){
        byte[] byteTotalLength = new byte[DATA_CHECK_LENGTH];
        byte[] temp = intToByteArray(fileTotalLength);
        System.arraycopy(temp,0,byteTotalLength,0,temp.length);
        byte[] byteFileName = fileName.getBytes();
        byte[] byteCheckCode = DATA_TRANS_START.getBytes();
        // 傳出byte陣列總長度  前30byte(這個檔案總長度),中間(這個檔案檔名),後6byte(確認碼)
        int lengthSendTotal = byteTotalLength.length + byteFileName.length + byteCheckCode.length;
        byte[] byteSend = new byte[lengthSendTotal];
        System.arraycopy(byteTotalLength,0,byteSend,0,DATA_CHECK_LENGTH);
        System.arraycopy(byteFileName,0,byteSend,DATA_CHECK_LENGTH,byteFileName.length);
        System.arraycopy(byteCheckCode,0,byteSend,DATA_CHECK_LENGTH+byteFileName.length,byteCheckCode.length);

        // DataOutputStream dos = new DataOutputStream(outputStream);
        try{
            outputStream.write(byteSend);
            Log.v("aaa","發送length " + fileTotalLength);
            Log.v("aaa","發送fileName "+ fileName);
           // outputStream.flush();

            //todo
            byte[] byteCheckPac = new byte[6];
            System.arraycopy(byteSend,byteSend.length-6,byteCheckPac,0,6);
            String check = new String(byteCheckCode);
            Log.v("aaa","checkPack " +check);
        }
        catch (Exception e){}

    }

    //將32位元的int值放到長度4的byte[]
    private static byte[] intToByteArray(int num){
        byte[] result = new byte[4];
        result[0] = (byte)(num >>> 24);//取最高8位元放到0下標
        result[1] = (byte)(num >>> 16);//取次高8位元放到1下標
        result[2] = (byte)(num >>> 8); //取次低8位元放到2下標
        result[3] = (byte)(num );      //取最低8位元放到3下標
        return result;
    }
    //將長度4的byte[]轉換為32位元的int值
    private static int byteArrayToInt(byte[] byteSrc){
        byte[] byteNum = new byte[4];
        int i = byteNum.length -1; //陣列長度與指標差1(指標從0開始)
        int j = byteSrc.length -1;
        for(;i>=0;i--,j--){     //從byte的尾部(即int值的最低位),開始copy byte
            if(j>=0)
                byteNum[i] = byteSrc[j];
            else
                byteNum[i] = 0; // 如果byteSrc長度不是4,則byteNum陣列自動補0;不過這裡應該不會發生
        }
        int v0 = (byteNum[0] &0xff ) << 24;    //&0xff 將byte值無差異轉成int,避免java自動轉型,產生數值變動
        int v1 = (byteNum[1] &0xff ) << 16;
        int v2 = (byteNum[2] &0xff ) << 8;
        int v3 = (byteNum[3] &0xff ) ;
        return v0+v1+v2+v3;
    }

    private synchronized void readFileDetail(byte[] buffer,int length){
        byte[] byteLength = new byte[4];
        System.arraycopy(buffer,0,byteLength,0,byteLength.length);
        // 接收到的byte[]轉int值
        int tempLength = byteArrayToInt(byteLength);
        int lengthFileName = length - DATA_CHECK_LENGTH - CHECK_CODE_LENGTH;
        byte[] byteFileName = new byte[lengthFileName];
        System.arraycopy(buffer,DATA_CHECK_LENGTH,byteFileName,0,lengthFileName);
        String fileName = new String(byteFileName);
        Log.v("aaa","tempLength"+tempLength);
        Log.v("aaa","fileTotalLengthCheck"+fileTotalLengthCheck);
        if(fileTotalLengthCheck ==0)
        {
            //通知開始接收檔案
            fileTotalLengthCheck = tempLength;
            Log.v("aaa","fileTotalLengthCheck"+fileTotalLengthCheck);
            Message msg = mainActivity.handler.obtainMessage(DATA_RECEIVE_START);
            Bundle bundle = new Bundle();
            bundle.putString(DATA_MSG_FILE_NAME,fileName);  //檔案名稱
            bundle.putString(DATA_MSG_FILE_SIZE,""+fileTotalLengthCheck);   //  檔案大小
            msg.setData(bundle);
            mainActivity.handler.sendMessage(msg);
            //準備接收資料的容器
            byteArrayOutputStream = new ByteArrayOutputStream();
            Log.v("aaa","接收檔案"+fileName);
            Log.v("aaa","接收檔案大小"+fileTotalLengthCheck);
        }
    }

    private synchronized void receiveData(byte[] buffer,int length){
        if(byteArrayOutputStream!= null){   //檔案接收的容器存在
            // 前面30byte跟後面6byte 是額外的資料
            byteArrayOutputStream.write(buffer,DATA_CHECK_LENGTH,
                    length-DATA_CHECK_LENGTH-CHECK_CODE_LENGTH);
            Log.v("aaa","byteArrayOutputStream"+byteArrayOutputStream.size());
        }
    }

    public void cancel(){
        try {
            if(socket != null)
                socket.close();
            this.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createFile(){
        // 確認檔案大小,與readFileDetail() 讀到的大小資料相等,沒有資料MISS
        Log.v("aaa","收到通知傳輸結束");
        if(byteArrayOutputStream!= null && byteArrayOutputStream.size()==fileTotalLengthCheck){
            // 通知將byteArrayOutputStream 轉成檔案
            mainActivity.handler.sendEmptyMessage(DATA_CREATE_FILE_CLIENT);
            fileTotalLengthCheck = 0;
        }
    }




}
