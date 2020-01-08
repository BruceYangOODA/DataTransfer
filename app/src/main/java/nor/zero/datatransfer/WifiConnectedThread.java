package nor.zero.datatransfer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static nor.zero.datatransfer.Constants.*;

public class WifiConnectedThread extends Thread {
    MainActivity mainActivity;
    private Handler handler;
    private final Socket socket;
    private final OutputStream outputStream ;
    private final InputStream inputStream ;
    private static boolean isConnected = true;
    private static final int sleepSpan = 30;
    private static int lengthFileTotalCheck = 0;
    private int currentFileSize = 0;
    static ByteArrayOutputStream byteArrayOutputStream = null;

    public WifiConnectedThread(MainActivity mainActivity,Socket socket){
        this.mainActivity =  mainActivity;
        handler = mainActivity.handler;
        this.socket = socket;
        OutputStream tempOut = null;
        InputStream tempIn = null;
        // 將socket.getInputStream socket.getOutputStream指定給 final物件
        try {
            tempOut = socket.getOutputStream();
            tempIn = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        outputStream = tempOut;
        inputStream = tempIn;
    }

    @Override
    public void run()  {
        int length = 0 ;
        byte[] buffer = new byte[READER_LENGTH];
        // 執行緒活動控制 boolean,若要從外部停止傳接訊息,就把isConnected 改為 false
        while (isConnected) {
            try {
                length = inputStream.read(buffer);
            } catch (IOException e) {
                cancel();
                Log.v("aaa","讀取 inputStream 出問題啦");
            }
            // 傳進來的是有效訊息
            if(length>= DATA_CHECK_LENGTH+CHECK_CODE_LENGTH){
                // 從讀取的資料後6byte取出確認碼byte,並轉換成String
                byte[] byteCheckCode = new byte[CHECK_CODE_LENGTH];
                System.arraycopy(buffer,length-CHECK_CODE_LENGTH,byteCheckCode,0,CHECK_CODE_LENGTH);
                String checkCode = new String(byteCheckCode);
                Log.v("aaa","讀到資料類型: "+checkCode);
                Log.v("aaa","讀到資料長度: "+length);
                switch (checkCode){
                    case DATA_TYPE_MESSAGE:
                        readMessage(buffer,length);
                        break;
                    case DATA_TRANS_START:
                        readFileDetail(buffer,length);
                        break;
                    case DATA_TRANS_END:
                        createFile(buffer,length);
                        break;
                    case DATA_DISCONNECT:
                        readDisconnectMessage();
                        break;
                    default:
                        receiveData(buffer,length);
                        break;
                }
            }

        }

    }

    public void write(String nickName,String chatMessage) {
        // 將String 名字，訊息內容，確認碼 轉換為 byte[]
        byte[] byteSend = formMessageByte(nickName,chatMessage);
        try {
            outputStream.write(byteSend);   //向對方機器送出byte[]
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.v("aaa","發送資料"+byteSend.length);
    }
    // 將String 名字，訊息內容，確認碼 轉換為 byte[]
    private byte[] formMessageByte(String nickName,String chatMessage){
        byte[] result;
        byte[] byteName = nickName.getBytes();
        byte[] byteMessage = chatMessage.getBytes();
        byte[] byteCheckCode = DATA_TYPE_MESSAGE.getBytes();
        result = new byte[DATA_CHECK_LENGTH + byteMessage.length + CHECK_CODE_LENGTH];
        System.arraycopy(byteName,0,result,0,byteName.length);
        System.arraycopy(byteMessage,0,result,DATA_CHECK_LENGTH,byteMessage.length);
        System.arraycopy(byteCheckCode,0,result,
                DATA_CHECK_LENGTH+byteMessage.length,CHECK_CODE_LENGTH);
        return result;
    }

    private void readMessage(byte[] buffer, int length){
        Message msg = byteToMsg(buffer,length);
        handler.sendMessage(msg);   //chatFragment更新讀到的 名字與訊息內容
    }
    private Message byteToMsg(byte[] buffer,int length){
        Message result;
        byte[] byteName = new byte[DATA_CHECK_LENGTH];
        System.arraycopy(buffer,0,byteName,0,DATA_CHECK_LENGTH);
        // 去除byte陣列後方預留空間的空byte
        int trimZero = 0;
        for(int i=0;i<byteName.length;i++){
            if(byteName[i]==0){
                trimZero = i;
                break;
            }
        }
        // 將名字byte陣列 轉換成 String
        String msgName = new String(byteName,0,trimZero);

        //傳進來的buffer 是由 名字byte陣列 內容byte陣列 以及 確認碼byte陣列組成
        int lengthContent = length - DATA_CHECK_LENGTH - CHECK_CODE_LENGTH;
        byte[] byteContent = new byte[lengthContent];
        System.arraycopy(buffer,DATA_CHECK_LENGTH,byteContent,0,lengthContent);
        String msgContent = new String(byteContent);
        Log.v("aaa","msgContent: "+msgContent);
        // 將String 名字 與 內容放入 Message物件,之後傳給handler更新聊天訊息顯示
        result = handler.obtainMessage(DATA_CHAT);
        Bundle bundle = new Bundle();
        bundle.putString(CHAT_MSG_NAME,msgName);
        bundle.putString(CHAT_MSG_CONTENT,msgContent);
        result.setData(bundle);
        return result;
    }

    public void transfer(String filePath,String fileName) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(filePath+"/"+fileName);

        int lengthFileTotal = 0;    //取得檔案大小,讓接收方確認是否收到正確的資料
        if(fileInputStream!= null)
            lengthFileTotal = fileInputStream.available();
        if(lengthFileTotal != 0){   //有取得檔案內容,開始傳輸
            notifyFileDetail(lengthFileTotal,fileName);     //通知對方傳送的檔案名字與大小

            byte[] buffer = new byte[SENDER_LENGTH];
            int length = -1;
            while ((length=fileInputStream.read(buffer)) != -1){
                sleep(sleepSpan);  // 傳遞資料休眠,資料傳太快,會塞車
                if(length == SENDER_LENGTH){    //buffer裝滿,準備傳給對方機器
                    outputStream.write(buffer);
                    outputStream.flush();
                }
                else {  ///buffer沒裝滿,fileInputStream 已經讀完
                    byte[] byteSend = new byte[DATA_CHECK_LENGTH + length + CHECK_CODE_LENGTH];
                    byte[] byteCheckCode = DATA_TRANS_END.getBytes();
                    System.arraycopy(buffer,0,byteSend,DATA_CHECK_LENGTH,length);
                    System.arraycopy(byteCheckCode,0,byteSend,
                            DATA_CHECK_LENGTH + length,CHECK_CODE_LENGTH);
                    outputStream.write(byteSend);
                    outputStream.flush();
                }
                /*CPU不夠 跑不動
                // 通知本機目前傳送進度
                currentFileSize += length;
                Message msg = handler.obtainMessage(DATA_PROGRESS);
                Bundle bundle = new Bundle();
                bundle.putInt(DATA_MSG_FILE_CURRENT_SIZE,currentFileSize);
                msg.setData(bundle);
                handler.sendMessage(msg);*/
            }
            //傳送完畢 暫存檔案大小 歸零
            //currentFileSize = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //通知對方傳送的檔案名字與大小
    private void notifyFileDetail(int lengthFileTotal,String fileName)throws IOException{
        byte[] byteIntValue = intToByteArray(lengthFileTotal);
        byte[] byteFileName = fileName.getBytes();
        byte[] byteCheckCode = DATA_TRANS_START.getBytes();
        int lengthByteSend = DATA_CHECK_LENGTH + byteFileName.length + CHECK_CODE_LENGTH;
        byte[] byteSend = new byte[lengthByteSend];
        System.arraycopy(byteIntValue,0,byteSend,0,byteIntValue.length);
        System.arraycopy(byteFileName,0,byteSend,DATA_CHECK_LENGTH,byteFileName.length);
        System.arraycopy(byteCheckCode,0,byteSend,
                DATA_CHECK_LENGTH + byteFileName.length,CHECK_CODE_LENGTH);
        outputStream.write(byteSend);
        outputStream.flush();
         /*CPU不夠 跑不動
        //通知本機傳送檔案名字與大小
        Message msg = handler.obtainMessage(DATA_TRANS_NOTE);
        Bundle bundle = new Bundle();
        bundle.putString(DATA_MSG_FILE_NAME,fileName);
        bundle.putInt(DATA_MSG_FILE_SIZE,lengthFileTotalCheck);
        msg.setData(bundle);
        handler.sendMessage(msg);*/
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
    // 將收到的檔名 儲存在 mainActivity.fileName 建檔的時候要用
    private void readFileDetail(byte[] buffer,int length){
        byte[] byteIntValue = new byte[4];
        System.arraycopy(buffer,0,byteIntValue,0,byteIntValue.length);
        int lengthFileTotal = byteArrayToInt(byteIntValue);

        int lengthFileName = length - DATA_CHECK_LENGTH - CHECK_CODE_LENGTH;
        byte[] byteFileName = new byte[lengthFileName];
        System.arraycopy(buffer,DATA_CHECK_LENGTH,byteFileName,0,lengthFileName);
        String fileName = new String(byteFileName);
        //設定接收檔案大小的核對值,如果接收完後的檔案等於這個值,才新建檔案
        lengthFileTotalCheck = lengthFileTotal;
        //通知開始接收檔案 檔名與大小
        Message msg = handler.obtainMessage(DATA_RECEIVE_START);
        Bundle bundle = new Bundle();
        bundle.putString(DATA_MSG_FILE_NAME,fileName); //檔案名稱
        bundle.putInt(DATA_MSG_FILE_SIZE,lengthFileTotal);  //  檔案大小
        msg.setData(bundle);
        handler.sendMessage(msg);
        //準備接收資料的容器
        byteArrayOutputStream = new ByteArrayOutputStream();
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

    // inputStream 有資料進來,就放進去byteArrayOutputStream
    private void receiveData(byte[] buffer,int length){
        if(byteArrayOutputStream != null){
            byteArrayOutputStream.write(buffer,0,length);
            Log.v("aaa","目前檔案大小: "+byteArrayOutputStream.size());
            //回報目前傳輸進度,調整 progressDialog 進度
            Message msg = handler.obtainMessage(DATA_PROGRESS);
            Bundle bundle = new Bundle();
            bundle.putInt(DATA_MSG_FILE_CURRENT_SIZE,byteArrayOutputStream.size());
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }

    private void createFile(byte[] buffer,int length){
        if(byteArrayOutputStream != null){
            //收到最後傳出的byte[] ,把byteArrayOutputStream組成完整檔
            byteArrayOutputStream.write(buffer,DATA_CHECK_LENGTH,
                    length - DATA_CHECK_LENGTH - CHECK_CODE_LENGTH);
            Log.v("aaa","收到通知傳輸結束");
            // 確認檔案大小,與readFileDetail() 讀到的大小資料相等,沒有資料MISS
            if(byteArrayOutputStream.size() == lengthFileTotalCheck){
                Log.v("aaa","接收成功: " + byteArrayOutputStream.size());
                lengthFileTotalCheck = 0 ;
                // 歸零,準備下次傳輸
                handler.sendEmptyMessage(DATA_CREATE_FILE);
            }
            else {
                Log.v("aaa","接收失敗: " + byteArrayOutputStream.size());
                lengthFileTotalCheck = 0 ;
                byteArrayOutputStream = null ;
                // 歸零,準備下次傳輸
            }

        }
    }
    // 通知遠端裝置 連線中斷
    void sendDisconnectMessage(){
        byte[] byteSend = new byte[DATA_CHECK_LENGTH + CHECK_CODE_LENGTH];
        byte[] byteCheckCode = DATA_DISCONNECT.getBytes();
        System.arraycopy(byteCheckCode,0,byteSend,DATA_CHECK_LENGTH,CHECK_CODE_LENGTH);
        try {
            outputStream.write(byteSend);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    // 收到 遠端裝置 連線中斷,執行連線中斷
    private synchronized void readDisconnectMessage(){
        cancel();
        mainActivity.wifiConnectedThread = null;
        mainActivity.disconnect();
    }
    public void cancel(){
        try {
            socket.close();
        } catch (IOException e) {
            Log.v("aaa","socket 關不起來 ");
        }
    }
}
