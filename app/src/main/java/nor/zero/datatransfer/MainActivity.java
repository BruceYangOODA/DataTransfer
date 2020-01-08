package nor.zero.datatransfer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.Dialog;
import android.app.ProgressDialog;
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
    ProgressDialog progressDialog;
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
    String serverSocketHostAddress="";
    //WifiP2pInfo wifiP2pInfo = null;
    private String fileName="";
    private int fileSize = 0;
    private int currentSize = 0;
    boolean isReceive;  //判別是接收檔案或傳送檔案 ,影響progressDialog的Title與Message
    //ServerSocket serverSocket;
    BluetoothAdapter bluetoothAdapter;
    ViewAnimator viewAnimator;
    boolean isBluetoothDevice;
    BluetoothConnectService bluetoothConnectService;
    WifiP2pDevice remoteDevice;

    private TextView tvChatClick,tvP2PListClick,tvP2PDetailClick;
    private static int selectTab = 1;   //目前顯示內容是哪一個Fragment



    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case DATA_CHAT:
                    // 接收到對方機器傳來的信息,更新本機chatFragment顯示UI
                    readOthersWriting(msg);
                    break;
                case DATA_TRANS_NOTE:   //CPU不夠,跑不動
                   // transNote(msg);    //通知傳送檔案檔名與大小
                    break;
                case DATA_RECEIVE_START:
                    receiveFile(msg);   //通知接收的檔名與大小
                    break;
                case DATA_PROGRESS:
                    updateProgress(msg);    //通知目前傳送檔案進度%
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
        registerReceiver(receiver, intentFilter);   //重新註冊 廣播接收器

    }
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);       //註銷 廣播接收器
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
                showRequestWifiServiceDialog();
                /*  //跳到Android wifi系統設定介面的方法
                Intent intent = new Intent();
                ComponentName componentName = new ComponentName("com.android.settings",
                        "com.android.settings.wifi.WifiSettings");
                intent.setComponent(componentName);
                startActivity(intent);*/
                return true;
                //詢問是否啟用藍芽服務 顯示的fragment 是 R.id.menu_toggle_log 的其中一個
            case R.id.menu_bluetooth_service:
                    showDialogRequestBluetoothService();
                return true;
                // 搜索wifiP2P 熱點訊號
            case R.id.menu_wifi_direct_discover:
                if(wifiManager.isWifiEnabled())    //wifi服務有開啟
                    searchingWifiP2PSignal();
                else                                //wifi服務沒有開啟
                    showRequestWifiServiceDialog();
                return true;
                // 查看 bluetoothDevice 裝置列表
            case R.id.menu_bluetooth_connect:
                    showBluetoothDeviceList();
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

    /*
    *
    *   本機主要UI 處理介面
    *
     */
    // 畫面 與 設定 初始化
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
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);//偵測Wifi P2P狀態的改變
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);//偵測Wifi P2P搜到peers改變
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
        bluetoothConnectService = new BluetoothConnectService(this);

        //設置主畫面下方三個Fragment之交換顯示
        flMainContent = findViewById(R.id.flMainContent);
        chatFragment = new ChatFragment(this);
        deviceListFragment = new DeviceListFragment(this);
        deviceDetailFragment = new DeviceDetailFragment(this);

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
    // 更換畫面下方Fragment 內容
    View.OnClickListener tvClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int viewID = v.getId();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            switch (viewID){
                case R.id.tvChatClick:
                    if(selectTab != 0) {
                        hideFragments(transaction);
                        transaction.show(chatFragment);
                        transaction.commit();
                        selectTab = 0;
                        changeTabColor(selectTab);  }
                    return;
                case R.id.tvP2PListClick:
                    if(selectTab != 1){
                        hideFragments(transaction);
                        transaction.show(deviceListFragment);
                        transaction.commit();
                        selectTab =1;
                        changeTabColor(selectTab); }
                    return;
                case R.id.tvP2PDetailClick:
                    if(selectTab !=2){
                        hideFragments(transaction);
                        transaction.show(deviceDetailFragment);
                        transaction.commit();
                        selectTab =2;
                        changeTabColor(selectTab);  }
                    return;
            }
        }
    };
    // 將下方fragment 隱藏起來,之後UI指定其中一個顯現
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
    //  點擊TextView切換目前點選TextView 的UI標示
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
    // 接收到對方機器傳來的信息,更新本機chatFragment顯示UI
    private void readOthersWriting(Message msg){
        String strName = msg.getData().getString(CHAT_MSG_NAME);
        String strContent = msg.getData().getString(CHAT_MSG_CONTENT);
        HashMap<String,String> chatContent = new HashMap<>();
        chatContent.put(CHAT_MSG_NAME,strName);
        chatContent.put(CHAT_MSG_CONTENT,strContent);
        chatFragment.addChatContent(chatContent);   //新增charFragment list內容
    }
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
    // 點選  R.id.menu_bluetooth_connect 將上層 fragment 切換成 藍芽裝置列表畫面
    private void showBluetoothDeviceList(){
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
        }
    }
    // 將 wifiP2p 熱點裝置列表 資料清空
    public void resetView(){
        // 清空DeviceListFragment 之前接收的資料
        if(deviceDetailFragment != null){
            deviceDetailFragment.resetViews();
        }
        if(deviceListFragment != null){
            deviceListFragment.resetViews();
        }
    }


    /*
    *
    *  wifi 訊號處理區塊
    *
    */
    //詢問是否啟用Wifi服務
    private void showRequestWifiServiceDialog(){
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
        // 顯示 wifi p2p 訊號搜尋 progressDialog
        deviceListFragment.showSearchingProgressDialog();
        // 搜尋的頻道 歸零
        channel = wifiP2pManager.initialize(this,getMainLooper(),null);
        //搜尋其他手機wifi P2P peers, 會由WifiP2pManager.PeerListListener 取得 peers 列表
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


    /*
    *
    *    藍芽訊號處理區塊
    *
     */
    //檢察本機是否支援藍芽功能
    public boolean checkBluetoothFunction(){
        boolean result = isBluetoothDevice;
        if(!isBluetoothDevice)
            Toast.makeText(this,getString(R.string.sys_msg_not_support_bluetooth),Toast.LENGTH_SHORT).show();
        return result;
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
    // 開放訊號給其他裝置搜索本機藍芽訊號 60秒(1分鐘)
    private void ensureDiscoverable(){
        if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,60);
            startActivity(discoverableIntent);
        }
    }

    /*
    *   資料傳遞區塊
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
        if(bluetoothConnectService.getState() == STATE_CONNECTED) {  //使用藍芽
            bluetoothConnectService.connectedThread.transfer(filePath, fileName);
        }
        else{  //使用WIFI
            if(wifiConnectedThread!= null) {
                wifiConnectedThread.transfer(filePath, fileName);
            }
        }
    }
    /*CPU不夠,跑不動
    // 通知傳送檔案檔名與大小 給progressDialog核對進度
    void transNote(Message msg){
        fileName = msg.getData().getString(DATA_MSG_FILE_NAME);
        fileSize = msg.getData().getInt(DATA_MSG_FILE_SIZE);
        isReceive = false;
        showProgressDialog();
    }*/
    // 通知傳送檔案檔名與大小 給progressDialog核對進度
    void receiveFile(Message msg){
        // 從另一台機器 接受到檔名與檔案大小
        fileName = msg.getData().getString(DATA_MSG_FILE_NAME);
        fileSize = msg.getData().getInt(DATA_MSG_FILE_SIZE);
        isReceive = true;
        showProgressDialog();
    }
    //調整進度條　進度
    void updateProgress(Message msg){
        float currentSize = (float)msg.getData().getInt(DATA_MSG_FILE_CURRENT_SIZE);
        float tempSize = (float)fileSize;
        int percent = (int)(currentSize/tempSize*100);
        if(progressDialog != null){
            progressDialog.setProgress(percent);
        }
        if(currentSize>=tempSize){   //進度達100%
            progressDialog.dismiss();
            if(!isReceive){  //傳輸端不需要檔名創檔,在這邊歸零
            fileName ="";
            fileSize=0;
            }
        }
    }
    // 顯示目前檔案傳遞進度
    void showProgressDialog(){
        if(progressDialog!= null && progressDialog.isShowing())  //先歸零
            progressDialog.dismiss();
        // UI顯示 檔案接收進度條
        progressDialog = new ProgressDialog(this);
        // 目前是接收還是傳送
        progressDialog.setTitle(isReceive?getString(R.string.dialog_msg_receive_file):
                getString(R.string.dialog_msg_trans_file));
        progressDialog.setMessage(getString(R.string.sys_msg_trans_file)+": " +fileName+"\n"
                +getString(R.string.sys_msg_file_size)+": "+fileSize);
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.show();
    }
    // 將接收到的byte[] 轉為檔案
    void createFile(){
        //收掉進度條
        if(progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
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
            fileSize = 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        //接收檔案容器歸零
        dataByteStream = null;
    }
    //開啟ServerSocket 與 Socket 取得socket物件 給 wifiConnectedThread 傳遞資料
    private synchronized void createConnectedThread(WifiP2pInfo wifiP2pInfo){
        final String hostAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
        deviceListFragment.getView().findViewById(R.id.tvNoDevice).setVisibility(View.INVISIBLE);
        Thread t;
        // 伺服端開啟執行緒 新建ServerSocket 監聽 來自 客戶端 的連接申請,連接後 取得 socket物件
        if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
            t = new Thread("ServerThread"){
                @Override
                public void run() {
                    try{
                        ServerSocket serverSocket = new ServerSocket(WIFI_PORT);
                        Socket tmpSocket;
                        Boolean isConnecting = true;
                        while (isConnecting){
                            try{
                                tmpSocket = serverSocket.accept();
                                Log.v("aaa","伺服端接收到連接");
                                if(tmpSocket != null){
                                    Log.v("aaa","伺服端取得socket物件");
                                    isConnecting = false;
                                    // 將取得的 socket 物件 傳給 wifiConnectedThread 執行 傳遞資料
                                    wifiConnectedThread = new WifiConnectedThread(MainActivity.this,tmpSocket);
                                    wifiConnectedThread.start();
                                    Log.v("aaa","建立伺服端資料執行緒");
                                    try {
                                        this.finalize();
                                    } catch (Throwable throwable) {
                                        throwable.printStackTrace();
                                    }
                                }
                            }catch (Exception e){Log.v("aaa","伺服端socket連接失敗");}
                        }
                    }
                    catch (Exception e){    Log.v("aaa","伺服端socket連接失敗2");  }
                }
            };
            t.start();
        }
        //如果本機是用戶端，新建用戶端執行緒 連接 伺服端的ServerSocket 取得 socket物件
        else if(wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner){
            t = new Thread("ClientThread"){
                @Override
                public void run() {
                    try{
                        Socket tmpSocket;
                        Boolean isConnecting = true;
                        while (isConnecting){
                            try{
                                tmpSocket = new Socket(hostAddress,WIFI_PORT);
                                Log.v("aaa","客戶端完成連接");
                                if(tmpSocket != null){
                                    Log.v("aaa","客戶端取得socket物件");
                                    isConnecting = false;
                                   // socket = tmpSocket;
                                    wifiConnectedThread = new WifiConnectedThread(MainActivity.this,tmpSocket);
                                    wifiConnectedThread.start();
                                    Log.v("aaa","建立客戶端資料執行緒");

                                    try {
                                        this.finalize();
                                    } catch (Throwable throwable) {
                                        throwable.printStackTrace();
                                    }

                                }
                            }catch (Exception e){Log.v("aaa","客戶端socket連接失敗");}
                        }
                    }
                    catch (Exception e){    Log.v("aaa","客戶端socket連接失敗2");  }
                }
            };
            t.start();

        }
        /*
        while (true){
            if(socket!= null){
                wifiConnectedThread = new WifiConnectedThread(this,socket);
                Log.v("aaa","建立資料執行緒");
                break;
            }
        }*/

    }

    /*
    *
    *   DeviceListFragment.DeviceActionListener 區塊
     *
     */
    //DeviceListFragment.DeviceActionListener 切換到 deviceDetailFragment, 更新UI
    @Override
    public void remoteDeviceDetail(WifiP2pDevice device) {

        deviceDetailFragment.remoteDeviceDetail(device);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        hideFragments(transaction);
        transaction.show(deviceDetailFragment);
        transaction.commit();
        selectTab = 2;
        changeTabColor(selectTab);
    }
    //DeviceListFragment.DeviceActionListener 會由 WiFiDirectBroadcastReceiver 回傳 wifiP2pInfo
    @Override
    public void connect(WifiP2pConfig wifiP2pConfig) {
        wifiP2pManager.connect(channel, wifiP2pConfig, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {}
            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    //DeviceListFragment.DeviceActionListener 從 deviceDetailFragment 取得遠端裝置資料
    //開啟ServerSocket 與 Socket 取得socket物件 給 wifiConnectedThread 傳遞資料
    @Override
    public void connecting(WifiP2pInfo wifiP2pInfo){
        //開啟ServerSocket 與 Socket 取得socket物件 給 wifiConnectedThread 傳遞資料
        createConnectedThread(wifiP2pInfo);
    }
    //DeviceListFragment.DeviceActionListener 中止wifi p2p連接, 更新UI
    @Override
    public synchronized void disconnect() {
        // 通知遠端裝置 連線中斷
        if(wifiConnectedThread != null){
            wifiConnectedThread.sendDisconnectMessage();
            wifiConnectedThread.cancel();
            wifiConnectedThread = null;
        }
        // 將 wifiP2p 熱點裝置列表 資料清空
        resetView();
        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }
            @Override
            public void onFailure(int reason) {

            }
        });
    }

    //WifiP2pManager.ChannelListener 內建方法
    @Override
    public void onChannelDisconnected() {
        if(wifiP2pManager != null && !retryChannel){
            Toast.makeText(this,R.string.system_msg_retry_connect,Toast.LENGTH_SHORT).show();
            resetView();
            retryChannel = true;
            wifiP2pManager.initialize(this,getMainLooper(),this);
        }
        else {
            Toast.makeText(this,R.string.system_msg_restart_wifi_p2p,Toast.LENGTH_SHORT).show();
        }
    }




    // 用wifiManager 處理 wifi是否開啟, 這個沒有用到
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled; }


}
