package nor.zero.datatransfer;

import android.app.ProgressDialog;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import androidx.fragment.app.Fragment;
import static nor.zero.datatransfer.DeviceListFragment.getDeviceStatus;
import static nor.zero.datatransfer.DeviceListFragment.DeviceActionListener;


public class DeviceDetailFragment extends Fragment implements WifiP2pManager.ConnectionInfoListener {

    private WifiP2pDevice remoteDevice = null;
    private View mContentView;
    ProgressDialog progressDialog ;
    static EditText etNickName;

    public DeviceDetailFragment(MainActivity mainActivity){
        progressDialog = mainActivity.progressDialog;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_detail,null);
        init();
        return mContentView;
    }

    //從MainActivity傳遞device數據更新ui
    void remoteDeviceDetail(WifiP2pDevice device) {
        remoteDevice = device;
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.VISIBLE);
        ((TextView)mContentView.findViewById(R.id.tv_his_name)).setText(device.deviceName);
        ((TextView)mContentView.findViewById(R.id.tv_his_status)).setText(getDeviceStatus(device.status));
    }
    // 從DeviceListFragment 傳資料過來 更新遠端裝置 名稱 與 連線狀態
    void updateRemoteDeviceDetail(WifiP2pDevice device){
        ((TextView)mContentView.findViewById(R.id.tv_his_name)).setText(device.deviceName);
        ((TextView)mContentView.findViewById(R.id.tv_his_status)).setText(getDeviceStatus(device.status));
        // 防呆 把 連接 的按鈕隱藏起來,不要再發出連接訊號
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    //從WiFiDirectBroadcastReceiver 取得 wifiP2PInfo資料 WifiP2pManager.ConnectionInfoListener 內建方法
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        if(progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
        //判斷本設備是否是組長並顯示組長的ip地址
        ((TextView)mContentView.findViewById(R.id.tvGroupOwner)).setText((wifiP2pInfo.isGroupOwner== true)?
                getResources().getString(R.string.system_msg_group_owner_yes) :
                getResources().getString(R.string.system_msg_group_owner_no) );
        // 顯示 伺服端的 IP 位址
        ((TextView)mContentView.findViewById(R.id.tvWifiInfo)).setText(
                getResources().getString(R.string.system_msg_group_owner_ip) +
                wifiP2pInfo.groupOwnerAddress.getHostAddress());
        // 防呆 把 連接 的按鈕隱藏起來,不要再發出連接訊號
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
        mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.VISIBLE);
        // 把wifiP2pInfo 資料傳給 mainActivity ,開啟 ServerSocket 與 Socket
        ((DeviceActionListener) getActivity()).connecting(wifiP2pInfo);
    }

    // 斷開連接,重置fragment畫面
    public void resetViews(){
        if(mContentView != null){
            mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
            mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.GONE);
            ((TextView)mContentView.findViewById(R.id.tv_his_name)).setText(R.string.icon_no_device);
            ((TextView)mContentView.findViewById(R.id.tv_his_status)).setText(R.string.icon_no_device_status);
            ((TextView)mContentView.findViewById(R.id.tvGroupOwner)).setText(R.string.empty_message);
            ((TextView)mContentView.findViewById(R.id.tvWifiInfo)).setText("");
        }
    }

    private void init(){
        ((Button)mContentView.findViewById(R.id.btn_connect)).setOnClickListener(btnClickListener);
        ((Button)mContentView.findViewById(R.id.btn_disconnect)).setOnClickListener(btnClickListener);
        etNickName = mContentView.findViewById(R.id.etNickName);
    }
    // 按鈕 OnClickListener ,兩顆的事件寫在一起
    View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btn_disconnect:   //斷開P2P熱點
                    // 顯示 連接按鈕 , 之前隱藏起來是為了 防呆
                    mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
                    ((DeviceActionListener) getActivity()).disconnect();
                    return;
                case R.id.btn_connect:      //連接P2P熱點
                    WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
                    wifiP2pConfig.deviceAddress = remoteDevice.deviceAddress;
                    wifiP2pConfig.wps.setup = WpsInfo.PBC;
                    if(progressDialog!= null && progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    // 顯示 progressDialog
                    progressDialog = ProgressDialog.show(getActivity(),getResources().getString(R.string.system_msg_cancel_discover),
                            getResources().getString(R.string.system_msg_connect) + " : "
                                    +remoteDevice.deviceAddress,true,true);
                    //用wifiP2pConfig 與 遠端裝置 remoteDevice 連接
                    //會由 WiFiDirectBroadcastReceiver 回傳 wifiP2pInfo
                    ((DeviceActionListener)getActivity()).connect(wifiP2pConfig);
                    return;
            }
        }
    };

}
