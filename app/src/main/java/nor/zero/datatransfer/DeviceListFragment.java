package nor.zero.datatransfer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.ListFragment;
import java.util.ArrayList;
import java.util.List;

public class DeviceListFragment extends ListFragment implements WifiP2pManager.PeerListListener {

    private View mContentView = null;
    private List<WifiP2pDevice> peers = new ArrayList<>();
    ProgressDialog progressDialog;
    TextView tvDeviceName;
    MainActivity mainActivity;

    public DeviceListFragment(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.row_devices, peers));
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list,null);

        return mContentView;
    }
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
        // 把對方藍芽裝置的資料 用 介面方法 DeviceActionListener 傳給 mainActivity
        ((DeviceActionListener) getActivity()).remoteDeviceDetail(device);
    }

    // WifiP2pManager.PeerListListener 內建方法
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        //收到回傳peerList 收掉progressDialog
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        peers.clear();
        //把搜索到的 wifi P2P裝置列表 傳給ArrayAdapter，並變更內容
        peers.addAll(peerList.getDeviceList());
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        for(WifiP2pDevice device : peers){
            if(device.status == WifiP2pDevice.CONNECTED)
                mainActivity.deviceDetailFragment.updateRemoteDeviceDetail(device);
        }
    }
    // 重置畫面
    public void resetViews(){
        peers.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        getView().findViewById(R.id.tvNoDevice).setVisibility(View.VISIBLE);
    }
    // 顯示 wifi p2p 訊號搜尋 progressDialog
    void showSearchingProgressDialog(){
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();   //歸零
        }
        progressDialog = ProgressDialog.show(getActivity(),
                getResources().getString(R.string.system_msg_cancel_discover),
                getResources().getString(R.string.system_msg_discover_peers),
                true,true, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Toast.makeText(getActivity(),R.string.system_msg_discover_cancel,Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {//適配器
        private List<WifiP2pDevice> items;
        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                                   List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if (top != null) {
                    top.setText(device.deviceName);
                }
                if (bottom != null) {
                    bottom.setText(getDeviceStatus(device.status));
                }
            }
            return v;
        }
    }
    //從 WiFiDirectBroadcastReceiver 接收到 device資料,更新本機資料
    public void updateThisDevice(WifiP2pDevice device) {//更新設備信息
        // 更新 本機名稱
        tvDeviceName = (TextView) mContentView.findViewById(R.id.tv_my_name);
        tvDeviceName.setText(device.deviceName);
        // 更新 本機連接狀態
        TextView view = (TextView) mContentView.findViewById(R.id.tv_my_status);
        view.setText(getDeviceStatus(device.status));
    }

    public interface DeviceActionListener{//一個activity的回調接口，監聽fragment的交互事件
        void remoteDeviceDetail(WifiP2pDevice device); //切換到 deviceDetailFragment, 更新UI
        //void cancelDisconnect();
        void connect(WifiP2pConfig config);
        void disconnect();
        void connecting(WifiP2pInfo wifiP2pInfo);
    }
}
