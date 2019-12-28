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
import android.widget.TextView;


import androidx.fragment.app.Fragment;
import static nor.zero.datatransfer.DeviceListFragment.*;


public class DeviceDetailFragment extends Fragment implements WifiP2pManager.ConnectionInfoListener {

    private WifiP2pDevice device = null;
    private View mContentView;
    private ProgressDialog progressDialog = null;
    private WifiP2pInfo wifiP2pInfo;
    private static  final int wifiPort = 8898;
/*
    public DeviceDetailFragment (DeviceListFragment deviceListFragment){
        this.deviceListFragment = deviceListFragment;
    }
    */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_detail,null);

        return mContentView;
    }

    View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btn_connect:
                    WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
                    wifiP2pConfig.deviceAddress = device.deviceAddress;
                    wifiP2pConfig.wps.setup = WpsInfo.PBC;
                    if(progressDialog!= null && progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                    progressDialog = ProgressDialog.show(getActivity(),getResources().getString(R.string.system_msg_cancel_discover),
                            R.string.system_msg_connect + " : "+device.deviceAddress,true,true);
                    ((DeviceActionListener)getActivity()).connect(wifiP2pConfig);
                    return;
                case R.id.btn_disconnect:
                    ((DeviceActionListener) getActivity()).disconnect();
                    return;
            }
        }
    };

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
        ((TextView)mContentView.findViewById(R.id.tv_his_status)).setText(getDeviceStatus(getDevice().status));


        //如果本機是伺服端

        if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
            WifiServerThread wifiServerThread = new WifiServerThread();
            wifiServerThread.start();
        }
        else if(wifiP2pInfo.groupFormed){
            WifiClientThread wifiClientThread = new WifiClientThread();
            wifiClientThread.start();
        }


    }
    public void resetViews(){
        if(mContentView != null){
            mContentView.findViewById(R.id.btn_connect).setVisibility(View.INVISIBLE);
            mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.INVISIBLE);
            ((TextView)mContentView.findViewById(R.id.tv_his_name)).setText(R.string.icon_no_device);
            ((TextView)mContentView.findViewById(R.id.tv_his_status)).setText(R.string.icon_no_device_status);
            ((TextView)mContentView.findViewById(R.id.tvGroupOwner)).setText(R.string.empty_message);
            ((TextView)mContentView.findViewById(R.id.tvWifiInfo)).setText("");
        }

    }

    //從MainActivity傳遞device數據更新ui
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
        mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.GONE);
        ((TextView)mContentView.findViewById(R.id.tv_his_name)).setText(device.deviceName);
        ((TextView)mContentView.findViewById(R.id.tv_his_status)).setText(getDeviceStatus(device.status));
        ((Button)mContentView.findViewById(R.id.btn_connect)).setOnClickListener(btnClickListener);

    }

}
