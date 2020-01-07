package nor.zero.datatransfer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.WrapperListAdapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static nor.zero.datatransfer.Constants.*;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener,
        DeviceListFragment.DeviceActionListener {

    private boolean mLogShown;
    private WifiManager wifiManager;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private FrameLayout flMainContent;
   // private FragmentManager fragmentManager;  XX
   // private FragmentTransaction transaction;
    private BroadcastReceiver receiver = null;
    /*
    public static final String CHAT_FRAGMENT = "chatFragment";
    public static final String DEVICE_LIST_FRAGMENT = "deviceListFragment";
    public static final String DEVICE_DETAIL_FRAGMENT = "deviceDetailFragment";
    public static final int WIFI_PORT = 8898;
    public static final int DATA_CHAT = 0;*/
    static ChatFragment chatFragment;
    static DeviceListFragment deviceListFragment;
    static DeviceDetailFragment deviceDetailFragment;
    static BluetoothDeviceListFragment bluetoothFragment;
    public WifiServerThread wifiServerThread  = null;
    public WifiClientThread wifiClientThread = null ;
    public WifiConnectedThread wifiConnectedThread = null;
    static String hostAddress="";
    WifiP2pInfo wifiP2pInfo = null;
    private String fileName="";
    ServerSocket serverSocket;
    BluetoothAdapter bluetoothAdapter;
    ViewAnimator viewAnimator;
    boolean isBluetoothDevice;
    BluetoothConnectService bluetoothConnectService;

    private TextView tvChatClick,tvP2PListClick,tvP2PDetailClick;
    private static int selectTab = 1;   //目前顯示內容是哪一個Fragment



    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case DATA_CHAT:
                    // 從另一台機器 傳來的本機Thread接收到的信息
                    String strName = msg.getData().getString(CHAT_MSG_NAME);
                    String strContent = msg.getData().getString(CHAT_MSG_CONTENT);
                    HashMap<String,String> chatContent = new HashMap<>();
                    chatContent.put(CHAT_MSG_NAME,strName);
                    chatContent.put(CHAT_MSG_CONTENT,strContent);
                    chatFragment.addChatContent(chatContent);   //新增charFragment list內容
                    break;
                case DATA_RECEIVE_START:
                    // 從另一台機器 接受到檔名與檔案大小
                    String tempFileName = msg.getData().getString(DATA_MSG_FILE_NAME);
                    String fileSize = msg.getData().getString(DATA_MSG_FILE_SIZE);
                    Log.v("aaa","收到檔名: "+fileName);
                    Log.v("aaa","收到大小: "+fileSize);
                    fileName = tempFileName;
                    Toast.makeText(getBaseContext(),getString(R.string.sys_msg_receive)+tempFileName+"\n"+
                            getString(R.string.sys_msg_file_size)+fileSize,Toast.LENGTH_SHORT).show();
                    break;
                case DATA_RECEIVE_FAIL:
                    Toast.makeText(getBaseContext(),getString(R.string.sys_msg_receive_fail),Toast.LENGTH_SHORT).show();
                    break;
                case DATA_CREATE_FILE:
                    createFile();
                    Toast.makeText(getBaseContext(),getString(R.string.sys_msg_receive_success),Toast.LENGTH_SHORT).show();
                    break;
                case DATA_CREATE_FILE_SERVER:
                    // 停用wifiServerThread ,所以這個沒有在用
                    // createFile(wifiServerThread.byteArrayOutputStream);
                    // wifiServerThread.byteArrayOutputStream = null; //接收檔案容器歸零
                    Toast.makeText(getBaseContext(),getString(R.string.sys_msg_receive_success),Toast.LENGTH_SHORT).show();
                    break;
                case DATA_CREATE_FILE_CLIENT:
                    // 停用wifiClientThread ,所以這個沒有在用
                    // createFile(wifiClientThread.byteArrayOutputStream);
                    // wifiClientThread.byteArrayOutputStream = null;  //接收檔案容器歸零
                    Toast.makeText(getBaseContext(),getString(R.string.sys_msg_receive_success),Toast.LENGTH_SHORT).show();
                    break;
                    //修改UI 顯示目前藍芽傳輸執行緒連接狀態
                case BLUETOOTH_THREAD_STATE:
                    int currentState = msg.getData().getInt(CURRENT_STATE);
                    updateBluetoothState(currentState);
                    break;
                    // 修改Bluetooth Fragment的裝置UI顯示
                case BLUETOOTH_DEVICE_NAME:
                    String deviceName = msg.getData().getString(DEVICE_NAME);
                    bluetoothFragment.tvDeviceName.setText(deviceName);
                    break;
                default:
                    break;
            }
        }
    };
    //修改UI 顯示目前藍芽傳輸執行緒連接狀態
    private void updateBluetoothState(int currentState){
        switch (currentState){
            case STATE_NONE:
                bluetoothFragment.tvCurrentState.setText(getString(R.string.tv_no_link));
                bluetoothFragment.btnListen.setText(getString(R.string.btn_start_listen));
                bluetoothFragment.tvDeviceName.setText("");
                break;
            case STATE_LISTEN:
                bluetoothFragment.tvCurrentState.setText(getString(R.string.tv_listening));
                break;
            case STATE_CONNECTING:
                bluetoothFragment.tvCurrentState.setText(getString(R.string.tv_linking));
                break;
            case STATE_CONNECTED:
                bluetoothFragment.tvCurrentState.setText(getString(R.string.tv_linked));
                bluetoothFragment.btnListen.setText(getString(R.string.btn_start_listen));
                break;
                default:
                    break;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, channel, this);
        registerReceiver(receiver, intentFilter);

    }
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
        // 中止資料傳遞執行緒
        if(bluetoothConnectService != null)
            bluetoothConnectService.stop();
    }
    //創建ActionBar選單功能
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    //設置ActionBar顯示標題之切換
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logToggle = menu.findItem(R.id.menu_toggle_log);
        logToggle.setVisible(findViewById(R.id.viewAnimator_main) instanceof ViewAnimator);
        logToggle.setTitle(mLogShown ? R.string.menu_show_bluetooth: R.string.menu_show_file);
        return super.onPrepareOptionsMenu(menu);
    }
    //設置ActionBar選單功能
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_toggle_log:
                mLogShown = !mLogShown;
                if(mLogShown){
                    viewAnimator.setDisplayedChild(1);  //顯示藍芽裝置列表
                    bluetoothFragment.showPairedDevice(); //顯示配對的藍芽裝置
                }
                else {
                    viewAnimator.setDisplayedChild(0);  //顯示檔案讀取頁面
                }
                //呼叫 Android內建功能 onPrepareOptionsMenu 更新切換後的顯示標題
                supportInvalidateOptionsMenu();
                return true;
            // 開啟系統wifi服務
            case R.id.menu_wifi_connect:
                showDialogRequestWifiService();
                /*  //開啟Android wifi系統設定介面
                Intent intent = new Intent();
                ComponentName componentName = new ComponentName("com.android.settings",
                        "com.android.settings.wifi.WifiSettings");
                intent.setComponent(componentName);
                startActivity(intent);*/
                return true;
                //詢問是否啟用藍芽服務
            case R.id.menu_bluetooth_service:
                    showDialogRequestBluetoothService();
                return true;
                // 搜索wifiP2P 熱點訊號
            case R.id.menu_wifi_direct_discover:
                if(wifiManager.isWifiEnabled())    //wifi服務有開啟
                    searchingWifiP2PSignal();
                else                                //wifi服務沒有開啟
                    showDialogRequestWifiService();
                return true;
                // 搜尋bluetooth裝置
            case R.id.menu_bluetooth_connect:
                   searchingBluetoothDevice();
                return true;
                //  開放藍芽訊號給其他裝置搜索
            case R.id.menu_bluetooth_discoverable:
                if(checkBluetoothFunction()){      //手機有支援藍芽功能
                    if(!bluetoothAdapter.isEnabled())
                        showDialogRequestBluetoothService();  //如果藍芽裝置沒有開啟,詢問是否開啟
                    else
                        ensureDiscoverable();       //開放搜索
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void init(){
        //檢察系統藍芽功能
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(this,getString(R.string.sys_msg_not_support_bluetooth),
                    Toast.LENGTH_SHORT).show();
            isBluetoothDevice = false;  //關於藍芽功能服務的程式不會運作
        }
        else
            isBluetoothDevice = true;

        //檢察系統wifi狀態
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiP2pManager = (WifiP2pManager) getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(this,getMainLooper(),null);
        // 監聽wifi狀態改變
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);//狀態的改變
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);//搜到列表改變
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);//鏈接狀態是否改變
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);//設備的詳細信息改變

        // 裝上方Fragment 的切換容器
        viewAnimator = (ViewAnimator) findViewById(R.id.viewAnimator_main);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //畫面上方Fragment顯示
        FileListFragment fileListFragment = new FileListFragment(this);
        bluetoothFragment = new BluetoothDeviceListFragment(this);
        //FrameLayout flFileList = findViewById(R.id.flFileList);
        transaction.add(R.id.flFileList,fileListFragment);
        transaction.add(R.id.flBluetoothList,bluetoothFragment);
        // 處理藍芽資料傳輸的執行緒群
        bluetoothConnectService = new BluetoothConnectService(handler);

        //設置主畫面下方三個Fragment之交換顯示
        flMainContent = findViewById(R.id.flMainContent);
        chatFragment = new ChatFragment(this);
        deviceListFragment = new DeviceListFragment();
        deviceDetailFragment = new DeviceDetailFragment();


        transaction.add(R.id.flMainContent,chatFragment,Constants.CHAT_FRAGMENT);
        transaction.add(R.id.flMainContent,deviceListFragment,Constants.DEVICE_LIST_FRAGMENT);
        transaction.add(R.id.flMainContent,deviceDetailFragment,Constants.DEVICE_DETAIL_FRAGMENT);
        transaction.hide(chatFragment);
        transaction.hide(deviceDetailFragment);
        transaction.commit();
        tvChatClick = findViewById(R.id.tvChatClick);
        tvP2PListClick = findViewById(R.id.tvP2PListClick);
        tvP2PDetailClick = findViewById(R.id.tvP2PDetailClick);
        tvChatClick.setOnClickListener(tvClickListener);
        tvP2PListClick.setOnClickListener(tvClickListener);
        tvP2PDetailClick.setOnClickListener(tvClickListener);

    }

    // 搜索wifiP2P 熱點訊號
    private void searchingWifiP2PSignal(){
        if(selectTab != 1){         //如果fragment不是顯示wifi熱點顯示fragment,就切換至該fragment
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            hideFragments(transaction);
            transaction.show(deviceListFragment);
            transaction.commit();
            selectTab = 1;
            changeTabColor(selectTab);
        }
        //搜尋其他手機wifi P2P訊號
        deviceListFragment.onInitiateDiscovery();
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, R.string.system_msg_discover_initiated,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, R.string.system_msg_discover_failed,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    //詢問是否啟用Wifi服務
    private void showDialogRequestWifiService(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_msg_system_setting))
                .setMessage(wifiManager.isWifiEnabled() ?        //隨著wifi服務狀態變更顯示訊息 啟動/關閉
                        getString(R.string.dialog_msg_disable_wifi) :
                        getString(R.string.dialog_msg_enable_wifi))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.no), null)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (wifiManager.isWifiEnabled()) {    //目前wifi服務開啟
                            wifiManager.setWifiEnabled(false);
                            Toast.makeText(getBaseContext(), getString(R.string.dialog_msg_disable_wifi),
                                    Toast.LENGTH_SHORT).show();
                        } else {      //目前wifi服務關閉
                            wifiManager.setWifiEnabled(true);
                            Toast.makeText(getBaseContext(), getString(R.string.dialog_msg_enable_wifi),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }
    //詢問是否啟用藍芽服務
    public void showDialogRequestBluetoothService(){
        if(isBluetoothDevice) {      //手機有藍芽功能,程式可以正常運作
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.dialog_msg_system_setting))
                    .setMessage(bluetoothAdapter.isEnabled() ?        //隨著藍芽服務狀態變更顯示訊息 啟動/關閉
                            getString(R.string.dialog_msg_disable_bluetooth) :
                            getString(R.string.dialog_msg_enable_bluetooth))
                    .setCancelable(false)
                    .setNegativeButton(getString(R.string.no), null)
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (bluetoothAdapter.isEnabled()) { //目前藍芽服務開啟
                                bluetoothAdapter.disable();  //關閉藍芽服務
                                Toast.makeText(getBaseContext(), getString(R.string.dialog_msg_disable_bluetooth),
                                        Toast.LENGTH_SHORT).show();
                            } else {  //目前藍芽服務關閉
                                bluetoothAdapter.enable();  //開啟藍芽服務
                                Toast.makeText(getBaseContext(), getString(R.string.dialog_msg_enable_bluetooth),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .show();
        }
        else
            Toast.makeText(this,getString(R.string.sys_msg_not_support_bluetooth),Toast.LENGTH_SHORT).show();
    }
    //檢察本機是否支援藍芽功能
    public boolean checkBluetoothFunction(){
        boolean result = isBluetoothDevice;
        if(!isBluetoothDevice)
            Toast.makeText(this,getString(R.string.sys_msg_not_support_bluetooth),Toast.LENGTH_SHORT).show();
        return result;
    }
    // 開放訊號給其他裝置搜索本機藍芽訊號 60秒(1分鐘)
    private void ensureDiscoverable(){
        if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,60);
            startActivity(discoverableIntent);
        }
    }
    //
    private void searchingBluetoothDevice(){
        if(!checkBluetoothFunction()){     //本機不支援藍芽功能
            return;
        }
        if(!mLogShown){  //目前顯示的是檔案頁面
            //顯示搜尋到的bluetooth 裝置清單Fragment
            mLogShown = !mLogShown ;
            viewAnimator.setDisplayedChild(1);
            bluetoothFragment.showPairedDevice(); //顯示配對的藍芽裝置
            //呼叫 Android內建功能 onPrepareOptionsMenu 更新切換後的顯示標題
            supportInvalidateOptionsMenu();
            //todo
        }

    }

    // 更換畫面下方Fragment 內容
    View.OnClickListener tvClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int viewID = v.getId();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            switch (viewID){
                case R.id.tvChatClick:
                    if(selectTab != 0)
                    {
                        hideFragments(transaction);
                        transaction.show(chatFragment);
                        transaction.commit();
                        selectTab = 0;
                        changeTabColor(selectTab);
                    }
                    return;

                case R.id.tvP2PListClick:
                    if(selectTab != 1){
                        hideFragments(transaction);
                        transaction.show(deviceListFragment);
                        transaction.commit();
                        selectTab =1;
                        changeTabColor(selectTab);
                    }

                    return;
                case R.id.tvP2PDetailClick:
                    if(selectTab !=2){
                        hideFragments(transaction);
                        transaction.show(deviceDetailFragment);
                        transaction.commit();
                        selectTab =2;
                        changeTabColor(selectTab);
                    }
                    return;
            }
        }
    };
    //  點擊fragment切換TextView, 切換目前點選TextView UI顯示
    private void changeTabColor(int select){
        switch (select){
            case 0:
                tvChatClick.setBackgroundColor(getResources().getColor(R.color.Aquamarine));
                tvP2PListClick.setBackgroundColor(getResources().getColor(R.color.MediumAquamarine));
                tvP2PDetailClick.setBackgroundColor(getResources().getColor(R.color.MediumAquamarine));
                break;
            case 1:
                tvChatClick.setBackgroundColor(getResources().getColor(R.color.MediumAquamarine));
                tvP2PListClick.setBackgroundColor(getResources().getColor(R.color.Aquamarine));
                tvP2PDetailClick.setBackgroundColor(getResources().getColor(R.color.MediumAquamarine));
                break;
            case 2:
                tvChatClick.setBackgroundColor(getResources().getColor(R.color.MediumAquamarine));
                tvP2PListClick.setBackgroundColor(getResources().getColor(R.color.MediumAquamarine));
                tvP2PDetailClick.setBackgroundColor(getResources().getColor(R.color.Aquamarine));
                break;
                default:
                    break;
        }
    }




    /* 資料傳遞區塊
    *   先判讀有藍芽裝置連接 就用 內部類別 ConnectedThread 傳輸資料
    *   再判讀有P2P熱點連接  次用 P2P熱點傳輸
    */
    void write(String nickName,String chatMessage) {
        if(bluetoothConnectService.getState() == STATE_CONNECTED)   //使用藍芽
            bluetoothConnectService.connectedThread.write(nickName,chatMessage);
        else{   //使用WIFI
            if(wifiConnectedThread != null)
                wifiConnectedThread.write(nickName,chatMessage);
        }
    }
    void transfer(String filePath,String fileName){
        if(bluetoothConnectService.getState() == STATE_CONNECTED)   //使用藍芽
            bluetoothConnectService.connectedThread.transfer(filePath,fileName);
        else{  //使用WIFI
            if(wifiConnectedThread!= null)
                wifiConnectedThread.transfer(filePath,fileName);
        }
    }
    // 將接收到的byte[] 轉為檔案
    void createFile(){
        byte[] dataByteStream;
        if(bluetoothConnectService.getState() == STATE_CONNECTED)   //使用藍芽
            dataByteStream = bluetoothConnectService.connectedThread.byteArrayOutputStream.toByteArray();
        else    //使用WIFI
            dataByteStream = wifiConnectedThread.byteArrayOutputStream.toByteArray();

        // 有接收到檔名,可以建立檔案名稱
        String filePath = DOWNLOAD_PATH+"/"+fileName;
        Log.v("aaa","創建檔案: "+filePath);
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(filePath);
            fileOutputStream.write(dataByteStream);
            fileOutputStream.close();
            Log.v("aaa","創建檔案成功");
            fileName=""; //檔案名稱歸零 ,準備下一次創建

        } catch (Exception e) {
            e.printStackTrace();
        }
        //接收檔案容器歸零
        dataByteStream = null;

    }


    private void hideFragments(FragmentTransaction transaction){
        if(chatFragment != null){
            transaction.hide(chatFragment);
        }
        if(deviceListFragment != null){
            transaction.hide(deviceListFragment);
        }
        if(deviceDetailFragment != null){
            transaction.hide(deviceDetailFragment);
        }
    }



    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        deviceDetailFragment.showDetails(device);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        hideFragments(transaction);
        transaction.show(deviceDetailFragment);
        transaction.commit();
        selectTab = 2;
        changeTabColor(selectTab);
    }

    public void wifiSendMessage(String chatMessage){}

    public void cancelDisconnect() {

    }

    @Override
    public void connect(WifiP2pConfig wifiP2pConfig) {
        wifiP2pManager.connect(channel, wifiP2pConfig, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void connecting(WifiP2pInfo wifiP2pInfo){
        hostAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
        deviceListFragment.getView().findViewById(R.id.tvNoDevice).setVisibility(View.INVISIBLE);
        this.wifiP2pInfo = wifiP2pInfo;
        //如果本機是伺服端，新建伺服器執行緒 或沿用上一個Thread
        // todo
        if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
            if(wifiServerThread != null){
                //wifiServerThread.start();
            }
            else{
                //建立P2P熱點連接後,只會只用到socket物件,因此捨棄Server,將socket物件傳給ConnectedThread監聽資料傳遞
                //wifiServerThread = new WifiServerThread(this,Constants.WIFI_PORT);
                //wifiServerThread.start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            serverSocket = new ServerSocket(WIFI_PORT);
                            Socket socket = null;
                            Boolean isConnecting = true;
                            while (isConnecting){
                                try{
                                    socket = serverSocket.accept();
                                    if(socket != null){
                                        isConnecting = false;
                                        wifiConnectedThread = new WifiConnectedThread(handler,socket);
                                        wifiConnectedThread.start();
                                        try {
                                            this.finalize();
                                        } catch (Throwable throwable) {
                                            throwable.printStackTrace();
                                        }

                                    }
                                }catch (Exception e){Log.v("aaa","socket連接失敗");}
                            }
                        }
                        catch (Exception e){    Log.v("aaa","socket連接失敗");  }
                    }
                }).start();
            }
        }
        //如果本機是用戶端，新建用戶端執行緒 或沿用上一個Thread
        else if(wifiP2pInfo.groupFormed){
            if(wifiClientThread != null){
               // wifiClientThread.start();
            }
            else {
               // wifiClientThread = new WifiClientThread(this);
               // wifiClientThread.start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            Socket socket = null;
                            Boolean isConnecting = true;
                            while (isConnecting){
                                try{
                                    socket = new Socket(hostAddress,WIFI_PORT);
                                    if(socket != null){
                                        isConnecting = false;
                                        wifiConnectedThread = new WifiConnectedThread(handler,socket);
                                        wifiConnectedThread.start();
                                        try {
                                            this.finalize();
                                        } catch (Throwable throwable) {
                                            throwable.printStackTrace();
                                        }

                                    }
                                }catch (Exception e){Log.v("aaa","socket連接失敗");}
                            }
                        }
                        catch (Exception e){    Log.v("aaa","socket連接失敗");  }
                    }
                }).start();
            }
        }


    }

    @Override
    public void disconnect() {
        //todo
        if(wifiServerThread != null)
      //      wifiServerThread.stop();
        if(wifiClientThread != null)
        //    wifiClientThread.stop();

        wifiP2pInfo = null;
        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // 重置Fragment畫面
                resetData();
            }
            @Override
            public void onFailure(int reason) {

            }
        });
    }

    public void resetData(){
        // 清空DeviceListFragment 之前接收的資料
        if(deviceDetailFragment != null){
            deviceDetailFragment.resetViews();
        }
        if(deviceListFragment != null){
            deviceListFragment.resetViews();
        }

    }

    //WifiP2pManager.ChannelListener 內建方法
    @Override
    public void onChannelDisconnected() {
        if(wifiP2pManager != null && !retryChannel){
            Toast.makeText(this,R.string.system_msg_retry_connect,Toast.LENGTH_SHORT).show();
            resetData();
            retryChannel = true;
            wifiP2pManager.initialize(this,getMainLooper(),this);
        }
        else {
            Toast.makeText(this,R.string.system_msg_restart_wifi_p2p,Toast.LENGTH_SHORT).show();
        }
    }


}
