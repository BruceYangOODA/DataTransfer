package nor.zero.datatransfer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static nor.zero.datatransfer.Constants.*;

public class BluetoothConnectService {
    static int currentState ;
    private Handler handler;
    BluetoothAdapter bluetoothAdapter;
    AcceptThread secureAcceptThread;
    AcceptThread insecureAcceptThread;
    LinkingThread linkingThread;
    ConnectedThread connectedThread;
    //ByteArrayOutputStream byteArrayOutputStream = null;
   // BluetoothServerSocket bluetoothServerSocket;
    // 曾經連接過的藍芽裝置 與 未曾連接過的裝置
    private static final String NAME_SECURE = "BluetoothSecure";
    private static final String NAME_INSECURE = "BluetoothInsecure";
    // UUID 註冊藍芽裝置彼此認識的連接阜號
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public BluetoothConnectService(Handler handler){
        this.handler = handler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        currentState = STATE_NONE;
        updateHandlerUI(getState());

    }


    public int getState(){
        return currentState;
    }

    public void reStart(){
        startServerListen();   // 把連接、接收、傳輸資料 Thread歸零,並開啟監聽伺服端口
        updateHandlerUI(getState());  // 修改UI 顯示目前連接狀態
    }
    public void stop(){
        if(currentState != STATE_NONE){     //目前沒有連線就不執行,防呆操作
            initThread();   // 把連接、接收、傳輸資料 Thread歸零
            currentState = STATE_NONE ;
            updateHandlerUI(getState());
        }
    }
    // 修改UI 顯示目前連接狀態
    private void updateHandlerUI(int currentState){
        Message msg = handler.obtainMessage(BLUETOOTH_THREAD_STATE);
        Bundle bundle = new Bundle();
        bundle.putInt(CURRENT_STATE,currentState);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }
    // 把連接、接收、傳輸資料 Thread歸零
    private synchronized void initThread(){
        if (linkingThread != null){
            linkingThread.cancel();
            linkingThread = null;
        }
        if(secureAcceptThread != null){
            secureAcceptThread.cancel();
            secureAcceptThread = null;
        }
        if(insecureAcceptThread != null){
            insecureAcceptThread.cancel();
            insecureAcceptThread = null;
        }
        if(connectedThread != null){
            connectedThread.cancel();
            connectedThread = null;
        }
    }
    // 創建 執行緒 監聽socket連接
    public synchronized void startServerListen(){
        initThread();
        secureAcceptThread = new AcceptThread(true);
        secureAcceptThread.start();
        insecureAcceptThread = new AcceptThread(false);
        insecureAcceptThread.start();
        updateHandlerUI(getState());
    }
    // 創建 執行緒 連接對方ServerSocket端口
    public synchronized void connect(BluetoothDevice device,boolean secure){
        initThread();   //執行緒歸零
        linkingThread = new LinkingThread(device,secure);
        linkingThread.start();
        updateHandlerUI(getState());  // 修改UI 顯示目前連接狀態
        Log.v("aaa","連接中: " + device.getName());
    }
    // 已連接上藍芽裝置 ,創建執行緒傳進 socket 物件 傳遞資料
    private synchronized void connected(BluetoothSocket socket,BluetoothDevice device,final String socketType){
        if(currentState == STATE_LISTEN){
            initThread();
        }
        else if(currentState == STATE_CONNECTING)
            linkingThread = null;

        connectedThread = new ConnectedThread(socket,socketType);
        connectedThread.start();
        updateHandlerUI(getState());
        // 修改Bluetooth Fragment的裝置UI顯示
        Message msg = handler.obtainMessage(BLUETOOTH_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME,device.getName());
        msg.setData(bundle);
        handler.sendMessage(msg);
        Log.v("aaa","連接上: " + device.getName());
    }
    // 連接失敗, reStart() 將執行緒歸零 重來
    private void connectionFailed(){
        BluetoothConnectService.this.stop();   //連接失敗,執行緒歸零重來
        Log.v("aaa","連接失敗");
    }
    // 連接失敗, reStart() 將執行緒歸零 重來
    private void connectionLost(){
        BluetoothConnectService.this.stop();   //連接失敗,執行緒歸零重來
        Log.v("aaa","連接遺失");
    }

    // 藍芽 ServerSocket 伺服端監聽執行緒
    private class AcceptThread extends Thread{
        private final BluetoothServerSocket serverSocket;
        private String socketType;

        public AcceptThread(boolean secure){
            BluetoothServerSocket temp = null;
            socketType = secure ? "Secure":"Insecure";
            // 打開伺服器端口,監聽 socket 連接
            try{
                if(secure){
                    temp = bluetoothAdapter.
                            listenUsingRfcommWithServiceRecord(NAME_SECURE,MY_UUID_SECURE);
                }
                else {
                    temp = bluetoothAdapter.
                            listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);
                }
            }
            catch (Exception e){}
            serverSocket = temp;
            currentState = STATE_LISTEN;    //這個執行緒運作時,連線狀態是 等待連接
                                    // 若執行connected(socket),狀態會變為 STATE_CONNECTED
        }

        public void run() {
            BluetoothSocket socket = null;
            // 持續監聽,直到已連接上另一台藍芽裝置
            while ( currentState != STATE_CONNECTED){
                try{
                    socket = serverSocket.accept();
                    Log.v("aaa","監聽到socket連接");
                }
                catch (Exception e){
                }
                if(socket != null) {    //有連接上另一台裝置
                    synchronized (BluetoothConnectService.this){
                        switch (currentState){
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // 連接上另一台裝置,把socket物件傳給另一個執行緒 執行資料傳輸
                                connected(socket,socket.getRemoteDevice(),socketType);
                                Log.v("aaa","得到socket物件");
                                Log.v("aaa","連接Thread");
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try{
                                    socket.close(); //異常狀況,先關閉socket,重連
                                }
                                catch (Exception e){}
                                break;
                        }
                    }
                }
            }
        }

        public void cancel(){
            try{
                serverSocket.close();   //關閉伺服端監聽口
            }
            catch (Exception e){}
        }
    }

    //藍芽 Socket 用戶端連接執行緒
    private class LinkingThread extends Thread{
        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        private String socketType;

        public LinkingThread(BluetoothDevice device,boolean secure){
            this.device = device;   //這個device 是對方藍芽裝置
            BluetoothSocket temp = null;
            socketType = secure ?"Secure" : "Insecure";
            try{
                if(secure){ //連接曾經配對過的藍芽裝置
                    temp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                }
                else { //連接未曾配對過的藍芽裝置
                    temp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            }
            catch (Exception e){}
            socket = temp;
            currentState = STATE_CONNECTING; //這個執行緒存在時,會從連線狀態 等待連接轉為嘗試連接
        }

        public void run() {
            Log.v("aaa","socket連接中");
            // 如果藍芽裝置還在搜尋裝置,就取消搜尋裝置
            bluetoothAdapter.cancelDiscovery();
            try{
                // class建構時,有設 UUID,依UUID 去找 ServerSocket
                socket.connect();
                Log.v("aaa","socket連接中");
            }
            catch (IOException e){
                try{
                    socket.close(); //不能連接上伺服器端口,關閉socket釋放資源
                }
                catch (IOException e2){connectionFailed();}
                connectionFailed();
                return;
            }
            // 將socket物件 傳給傳輸執行緒 傳輸資料
            connected(socket,device,socketType);
        }

        public void cancel(){
            try{
                socket.close();
                 }
            catch (Exception e){}
        }
    }

    // 這個執行緒會傳進一個 socket 物件, 得到socket 輸出,輸入流 來傳遞資料
    class ConnectedThread extends Thread{
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        int sleepSpan = 30;
        int lengthFileTotalCheck = 0;
        ByteArrayOutputStream byteArrayOutputStream = null;


        public ConnectedThread(BluetoothSocket socket,String socketType){
            Log.v("aaa","連接到的是: "+socketType);
            this.socket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null ;
            currentState = STATE_CONNECTED;     //這個執行緒存在時,連接狀態是 已連接
            try{
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            }
            catch (IOException e){}
            inputStream = tempIn;   //輸入流
            outputStream = tempOut; //輸出流
        }

        @Override
        public void run() {
            int length = 0;
            byte[] buffer = new byte[READER_LENGTH];
            while (currentState == STATE_CONNECTED){
                try{
                    length = inputStream.read(buffer);
                    Log.v("aaa","LIVE");
                    Log.v("aaa","length"+length);
                }
                catch (IOException e){
                    connectionLost(); //socket 抓不到資料,執行緒歸零,重來
                }
                // 傳進來的是有效訊息
                if(length>DATA_CHECK_LENGTH + CHECK_CODE_LENGTH){
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
                            Log.v("aaa","傳送資料: "+length);
                        }
                        else {  ///buffer沒裝滿,fileInputStream 已經讀完
                            byte[] byteSend = new byte[DATA_CHECK_LENGTH + length + CHECK_CODE_LENGTH];
                            byte[] byteCheckCode = DATA_TRANS_END.getBytes();
                            System.arraycopy(buffer,0,byteSend,DATA_CHECK_LENGTH,length);
                            System.arraycopy(byteCheckCode,0,byteSend,
                                    DATA_CHECK_LENGTH + length,CHECK_CODE_LENGTH);
                            outputStream.write(byteSend);
                            outputStream.flush();
                            Log.v("aaa","傳送資料: "+length);
                            Log.v("aaa","資料傳送結束");
                        }
                    }
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
            Log.v("aaa","傳送檔名: "+fileName);
            Log.v("aaa","檔案大小: "+lengthFileTotal);
        }
        //將32位元的int值放到長度4的byte[]
        private byte[] intToByteArray(int num){
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
            //通知開始接收檔案
            Message msg = handler.obtainMessage(DATA_RECEIVE_START);
            Bundle bundle = new Bundle();
            bundle.putString(DATA_MSG_FILE_NAME,fileName); //檔案名稱
            bundle.putString(DATA_MSG_FILE_SIZE,""+lengthFileTotalCheck);  //  檔案大小
            msg.setData(bundle);
            handler.sendMessage(msg);
            //準備接收資料的容器
            byteArrayOutputStream = new ByteArrayOutputStream();
        }
        //將長度4的byte[]轉換為32位元的int值
        private int byteArrayToInt(byte[] byteSrc){
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

        public void cancel(){
            try{
                socket.close();
            }
            catch (Exception e){}
        }
    }

}
