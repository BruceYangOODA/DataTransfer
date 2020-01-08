package nor.zero.datatransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

import static nor.zero.datatransfer.Constants.*;

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
        //偵測系統WIFI P2P 網路組態,用 wifiManager 取代,用處不大
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                mainActivity.setIsWifiP2pEnabled(true); // 目前wifi 開著
            }
            else if(state == WifiP2pManager.WIFI_P2P_STATE_DISABLED){
                mainActivity.setIsWifiP2pEnabled(false);    //目前wifi 關著
                mainActivity.resetView();
                  }
        }
        //WIFI P2P 搜索 peers 當 peers的數量改變時
        else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            if (wifiP2pManager == null)
                return;
            WifiP2pManager.PeerListListener temp = (WifiP2pManager.PeerListListener)
                    mainActivity.deviceListFragment;
            // 通知 PeerListListener.onPeersAvailable() 收到的 peers list
            wifiP2pManager.requestPeers(channel,temp);

        }
        //WIFI P2P 網路組態設定改變時 網路連接狀態改變 Peers連接 或 斷開
        else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            if(wifiP2pManager == null)
                return;
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            //已經連接上其他的設備，請求連接信息找到組長的ip
            if(networkInfo.isConnected()){  //連接上另一台 p2p 裝置
                // 通知 WifiP2pManager.ConnectionInfoListener     wifiP2pInfo 資料
                wifiP2pManager.requestConnectionInfo(channel,mainActivity.deviceDetailFragment);
            }
            else if(!networkInfo.isConnected()){  //從另一台 p2p 裝置 斷開
                mainActivity.disconnect();
               //mainActivity.resetView();   //重置fragment畫面
            }
        }
        // 更新本機設備信息
        else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            mainActivity.deviceListFragment.updateThisDevice(device);
        }

    }
}
