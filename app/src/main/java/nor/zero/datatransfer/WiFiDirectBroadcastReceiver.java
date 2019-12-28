package nor.zero.datatransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import static nor.zero.datatransfer.MainActivity.*;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private MainActivity mainActivity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel, MainActivity mainActivity) {
        super();
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
        this.mainActivity = mainActivity;
}
    //由背景廣播接收器接收目前網路組態設定
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        //WIFI P2P 網路組態設定改變時 啟用Wifi P2P功能
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                mainActivity.setIsWifiP2pEnabled(true);
            }
            else{
                mainActivity.setIsWifiP2pEnabled(false);
                mainActivity.resetData();   }
        }
        //WIFI P2P 網路組態設定改變時 進行Peers 搜索
        else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            //被PeerListListener.onPeersAvailable()通知
            if (wifiP2pManager != null) {
                wifiP2pManager.requestPeers(channel, (WifiP2pManager.PeerListListener)
                        mainActivity.getSupportFragmentManager().findFragmentByTag(DEVICE_LIST_FRAGMENT));
            }
        }
        //WIFI P2P 網路組態設定改變時 網路連接狀態改變 Peers連接 或 斷開
        else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            if(wifiP2pManager == null)
                return;
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            //已經連接上其他的設備，請求連接信息找到組長的ip
            if(networkInfo.isConnected()){
                DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment) mainActivity
                        .getSupportFragmentManager().findFragmentByTag(DEVICE_DETAIL_FRAGMENT);
                // 通知 WifiP2pManager.ConnectionInfoListener
                wifiP2pManager.requestConnectionInfo(channel,deviceDetailFragment);
            }
            else{
                mainActivity.resetData();   //斷開連接設備,重新discover
            }
        }
        // 更新本機設備信息
        else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            DeviceListFragment deviceListFragment = (DeviceListFragment) mainActivity
                    .getSupportFragmentManager().findFragmentByTag(DEVICE_LIST_FRAGMENT);
            deviceListFragment.updateThisDevice((WifiP2pDevice)intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        }

    }
}
