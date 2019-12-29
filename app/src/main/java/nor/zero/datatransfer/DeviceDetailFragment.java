package nor.zero.datatransfer;

import android.app.ProgressDialog;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import androidx.fragment.app.Fragment;
import static nor.zero.datatransfer.DeviceListFragment.*;


public class DeviceDetailFragment extends Fragment implements WifiP2pManager.ConnectionInfoListener {

    private WifiP2pDevice otherDevice = null;
    private View mContentView;
    private ProgressDialog progressDialog = null;
    private WifiP2pInfo wifiP2pInfo;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_detail,null);
        init();
        return mContentView;
    }

    View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btn_disconnect:   //斷開P2P熱點
                    mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
                    ((DeviceActionListener) getActivity()).disconnect();
                    return;
                case R.id.btn_connect:      //連接P2P熱點
                    WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
                    wifiP2pConfig.deviceAddress = otherDevice.deviceAddress;
                    wifiP2pConfig.wps.setup = WpsInfo.PBC;
                    if(progressDialog!= null && progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    progressDialog = ProgressDialog.show(getActivity(),getResources().getString(R.string.system_msg_cancel_discover),
                            getResources().getString(R.string.system_msg_connect) + " : "
                                    +otherDevice.deviceAddress,true,true);
                    ((DeviceActionListener)getActivity()).connect(wifiP2pConfig);
                    return;

            }
        }
    };

    //WifiP2pManager.ConnectionInfoListener 內建方法
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if(progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
        //判斷本設備是否是組長並顯示組長的ip地址
        wifiP2pInfo = info;
        ((TextView)mContentView.findViewById(R.id.tvGroupOwner)).setText((wifiP2pInfo.isGroupOwner== true)?
                getResources().getString(R.string.system_msg_group_owner_yes) :
                getResources().getString(R.string.system_msg_group_owner_no) );
        ((TextView)mContentView.findViewById(R.id.tvWifiInfo)).setText(
                getResources().getString(R.string.system_msg_group_owner_ip) +
                wifiP2pInfo.groupOwnerAddress.getHostAddress());
        ((TextView)mContentView.findViewById(R.id.tv_his_name)).setText(getDevice().deviceName);
        ((TextView)mContentView.findViewById(R.id.tv_his_status)).setText(getDeviceStatus(getDevice().status));
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
        mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.VISIBLE);
        // 開啟資料傳遞執行緒
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
    }
    //從MainActivity傳遞device數據更新ui
    public void showDetails(WifiP2pDevice device) {
        otherDevice = device;
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.VISIBLE);
        ((TextView)mContentView.findViewById(R.id.tv_his_name)).setText(device.deviceName);
        ((TextView)mContentView.findViewById(R.id.tv_his_status)).setText(getDeviceStatus(device.status));

    }



}
