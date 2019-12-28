package nor.zero.datatransfer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.WrapperListAdapter;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener,
        DeviceListFragment.DeviceActionListener {

    private boolean mLogShown;
    private WifiManager wifiManager;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private boolean isWifiP2pEnabled = false;
    private FrameLayout flMainContent;
   // private FragmentManager fragmentManager;  XX
   // private FragmentTransaction transaction;
    private BroadcastReceiver receiver = null;
    public static final String CHAT_FRAGMENT = "chatFragment";
    public static final String DEVICE_LIST_FRAGMENT = "deviceListFragment";
    public static final String DEVICE_DETAIL_FRAGMENT = "deviceDetailFragment";
    private static ChatFragment chatFragment;
    private static DeviceListFragment deviceListFragment;
    private static DeviceDetailFragment deviceDetailFragment;

   // private TextView tvP2PListClick,tvP2PDetailClick;
    private static int selectTab = 0;   //目前顯示內容是哪一個Fragment



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
        logToggle.setTitle(mLogShown ? R.string.menu_show_log: R.string.menu_hide_log );
        return super.onPrepareOptionsMenu(menu);
    }
    //設置ActionBar選單功能切換
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_toggle_log:
                mLogShown = !mLogShown;
                ViewAnimator viewAnimator = (ViewAnimator) findViewById(R.id.viewAnimator_main);
                if(mLogShown){
                    viewAnimator.setDisplayedChild(1);  //顯示Log檔案
                }
                else {
                    viewAnimator.setDisplayedChild(0);  //顯示檔案讀取頁面
                }
                //呼叫 onPrepareOptionsMenu 更新切換後的顯示標題
                supportInvalidateOptionsMenu();
                return true;
            // 開啟系統wifi服務
            case R.id.menu_wifi_connect:
                if(!wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(true);
                    Toast.makeText(this,R.string.system_msg_connect_wifi,Toast.LENGTH_LONG).show();
                }
                /*  //跳到wifi系統設定
                Intent intent = new Intent();
                ComponentName componentName = new ComponentName("com.android.settings",
                        "com.android.settings.wifi.WifiSettings");
                intent.setComponent(componentName);
                startActivity(intent);*/
                return true;
            case R.id.menu_bluetooth_connect:

                return true;
            case R.id.menu_wifi_direct_discover:
                if(isWifiP2pEnabled){
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    hideFragments(transaction);
                    transaction.show(deviceListFragment);
                    transaction.commit();
                    selectTab = 1;
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
                else {
                    wifiManager.setWifiEnabled(true);
                    Toast.makeText(this,R.string.system_msg_connect_wifi,Toast.LENGTH_LONG).show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 更換畫面下方Fragment 內容
    View.OnClickListener tvClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int viewID = v.getId();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
      /* //X     ChatFragment chatFragment = (ChatFragment)
                    getSupportFragmentManager().findFragmentByTag(CHAT_FRAGMENT);
            DeviceListFragment deviceListFragment = (DeviceListFragment)
                    getSupportFragmentManager().findFragmentByTag(DEVICE_LIST_FRAGMENT);*/
            switch (viewID){
                case R.id.tvChatClick:
                    if(selectTab != 0)
                    {
                        hideFragments(transaction);
                        transaction.show(chatFragment);
                        transaction.commit();
                        selectTab = 0;
                    }
                    return;

                case R.id.tvP2PListClick:
                    if(selectTab != 1){
                        hideFragments(transaction);
                        transaction.show(deviceListFragment);
                        transaction.commit();
                        selectTab =1;
                    }

                    return;
                case R.id.tvP2PDetailClick:
                    if(selectTab !=2){
                        hideFragments(transaction);
                        transaction.show(deviceDetailFragment);
                        transaction.commit();
                        selectTab =2;
                    }
                    return;
            }
        }
    };

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

    private void init(){
        //設置畫面下方三個Fragment交換顯示
        flMainContent = findViewById(R.id.flMainContent);
        chatFragment = new ChatFragment();
        deviceListFragment = new DeviceListFragment();
        deviceDetailFragment = new DeviceDetailFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.flMainContent,chatFragment,CHAT_FRAGMENT);
        transaction.add(R.id.flMainContent,deviceListFragment,DEVICE_LIST_FRAGMENT);
        transaction.add(R.id.flMainContent,deviceDetailFragment,DEVICE_DETAIL_FRAGMENT);
        transaction.hide(deviceListFragment);
        transaction.hide(deviceDetailFragment);
        transaction.commit();
        TextView tvChatClick = findViewById(R.id.tvChatClick);
        TextView tvP2PListClick = findViewById(R.id.tvP2PListClick);
        TextView tvP2PDetailClick = findViewById(R.id.tvP2PDetailClick);
        tvChatClick.setOnClickListener(tvClickListener);
        tvP2PListClick.setOnClickListener(tvClickListener);
        tvP2PDetailClick.setOnClickListener(tvClickListener);


        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);//狀態的改變
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);//搜到列表改變
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);//鏈接狀態是否改變
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);//設備的詳細信息改變
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiP2pManager = (WifiP2pManager) getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(this,getMainLooper(),null);


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
    }

    @Override
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
    public void disconnect() {
        if(deviceDetailFragment != null){
            deviceDetailFragment.resetViews();
        }
        if(deviceListFragment != null){
            deviceListFragment.resetViews();
        }
        wifiP2pManager.removeGroup(channel, null);
    }

    public void resetData(){
        // 清空DeviceListFragment 之前接收的資料
        deviceListFragment.getView().findViewById(R.id.tvNoDevice).setVisibility(View.VISIBLE);
    }

    @Override
    public void onChannelDisconnected() {

    }
}
