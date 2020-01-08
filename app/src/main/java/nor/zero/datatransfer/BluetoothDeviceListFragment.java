package nor.zero.datatransfer;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import static nor.zero.datatransfer.Constants.*;

public class BluetoothDeviceListFragment extends Fragment {
   // private BluetoothDeviceListFragment adapter;
    private ArrayList<HashMap<String,String>> newDeviceList,pairedDeviceList;
    private View mContentView = null;
    BluetoothAdapter bluetoothAdapter;
    private static final String NAME = "deviceName";
    private static final String ADDRESS = "deviceAddress";
    ItemAdapter newDeviceAdapter,pairedDeviceAdapter;
    Button btnScan,btnListen,btnStop;
   //X  static boolean isSearching = false;
    private ProgressDialog progressDialog = null;
    public TextView tvCurrentState,tvDeviceName;
    MainActivity mainActivity;
    private boolean isListen = false;


    public BluetoothDeviceListFragment(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState) {
        //用bluetoothAdapter 取得藍芽裝置配對清單,用showPairedDevice() 顯示清單內容
        mContentView = inflater.inflate(R.layout.bluetooth_fragment,null);
        pairedDeviceList = new ArrayList<>();
        pairedDeviceAdapter = new ItemAdapter(pairedDeviceList);
        ListView pairedListView = mContentView.findViewById(R.id.lv_paired_devices);
        pairedListView.setAdapter(pairedDeviceAdapter);
        pairedListView.setOnItemClickListener(pairedDeviceClickListener);

        //搜尋新增藍芽裝置清單設置,用doDiscovery()取得新裝置清單
        newDeviceList = new ArrayList<>();
        newDeviceAdapter = new ItemAdapter(newDeviceList);
        ListView newDeviceListView = mContentView.findViewById(R.id.lv_new_devices);
        newDeviceListView.setAdapter(newDeviceAdapter);
        newDeviceListView.setOnItemClickListener(newDeviceClickListener);

        //註冊找到藍芽裝置Receiver,取得新裝置清單
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getContext().registerReceiver(broadcastReceiver,filter);
        //註冊藍芽裝置搜尋結束Receiver
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getContext().registerReceiver(broadcastReceiver,filter);

        //點擊按鈕,搜尋藍芽裝置信號
        btnScan = mContentView.findViewById(R.id.btnScanDevice);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDiscovery();
            }
        });
        //點擊按鈕,開啟執行緒監聽socket 或 停止監聽
        btnListen = mContentView.findViewById(R.id.btnStartListen);
        btnListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeListenState();
            }
        });
        //點擊按鈕,停止執行緒動作
        btnStop = mContentView.findViewById(R.id.btnStopLink);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopConnection();
            }
        });

        //顯示目前訊號連接狀態
        tvCurrentState = mContentView.findViewById(R.id.tvCurrentState);
        //顯示目前連接的藍芽裝置名稱
        tvDeviceName = mContentView.findViewById(R.id.tvDeviceName);

        return mContentView;
    }

    // 取得系統 藍芽裝置配對清單 ,並顯示
    void showPairedDevice(){
        if(pairedDeviceList.size()==0){  //未讀取,開始讀取一遍
            //取得系統藍芽裝置綁定清單
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if(pairedDevices.size()>0){
                for(BluetoothDevice device : pairedDevices){
                    HashMap<String,String> item = new HashMap<>();
                    item.put(NAME,device.getName());
                    item.put(ADDRESS,device.getAddress());
                    pairedDeviceList.add(item);
                }
            }
        }
        pairedDeviceAdapter.notifyDataSetChanged();
    }


    // 點擊的是曾經配對過的藍芽裝置
    AdapterView.OnItemClickListener pairedDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            startConnectDevice(view,true);
        }
    };
    // 點擊的是未曾配對過的藍芽裝置
    AdapterView.OnItemClickListener newDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            startConnectDevice(view,false);
        }
    };
    private void startConnectDevice(View view,boolean secure){
        // 防呆操作, 如果是在連線狀態,不能再開另一條連線
        if(mainActivity.bluetoothConnectService.currentState != STATE_CONNECTED){
            // 如果還未搜尋藍芽信號完畢,中止搜尋信號
            bluetoothAdapter.cancelDiscovery();
            TextView tvAddress = view.findViewById(R.id.tv_device_address);
            String address = tvAddress.getText().toString();
            Log.v("aaa","address: "+address);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            //啟用BluetoothConnectService 中的執行緒取得資料連結
            MainActivity mainActivity = (MainActivity) getActivity();
            if(mainActivity.bluetoothConnectService == null)
                mainActivity.bluetoothConnectService = new BluetoothConnectService(mainActivity);
            mainActivity.bluetoothConnectService.connect(device,secure);
        }
    }
    //搜索藍芽裝置信號
    private void doDiscovery(){
        if(!mainActivity.checkBluetoothFunction()) //本機不支援藍芽功能
            return;

        if(bluetoothAdapter.isEnabled()){   //藍芽功能開啟中
            if(bluetoothAdapter.isDiscovering())    //如果訊號搜索中,先停止,不要開雙重訊號
                bluetoothAdapter.cancelDiscovery();
            bluetoothAdapter.startDiscovery();
            // UI進度條 搜尋訊號中
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();   //先歸零
            progressDialog = progressDialog.show(getActivity(), getString(R.string.system_msg_cancel_discover),
                    getString(R.string.system_msg_discover_bluetooth_device),
                    true, true, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            bluetoothAdapter.cancelDiscovery();
                            Toast.makeText(getContext(),R.string.system_msg_discover_pause,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        else    //請求打開藍芽功能
            mainActivity.showDialogRequestBluetoothService();

    }
    //開啟執行緒監聽socket 或 停止監聽
    private void changeListenState(){
        if(!mainActivity.checkBluetoothFunction())  //本機不支援藍芽功能
            return;
        if(bluetoothAdapter.isEnabled()){   //藍芽功能開啟中
            if(isListen){       //監聽中,停止監聽
                btnListen.setText(getString(R.string.btn_start_listen));
                mainActivity.bluetoothConnectService.stop();
                isListen = !isListen;
            }
            else {  //沒有監聽,開始監聽
                btnListen.setText(getString(R.string.btn_stop_listen));
                mainActivity.bluetoothConnectService.startServerListen();
                isListen = !isListen;
            }
        }
        else    //請求打開藍芽功能
            mainActivity.showDialogRequestBluetoothService();
    }
    // 停止執行緒動作
    private void stopConnection(){
        if(!mainActivity.checkBluetoothFunction())  //本機不支援藍芽功能
            return;
        if(bluetoothAdapter.isEnabled()){
            isListen = false;
            mainActivity.bluetoothConnectService.stop();
        }
        else     //請求打開藍芽功能
            mainActivity.showDialogRequestBluetoothService();
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //找到新裝置
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                //從intent,取得device物件資料
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //如果找到的裝置不在系統藍芽裝置綁定清單裡面,就新增進新清單
                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                    HashMap<String,String> item = new HashMap<>();
                    item.put(NAME,device.getName());
                    item.put(ADDRESS,device.getAddress());
                    newDeviceList.add(item);
                    newDeviceAdapter.notifyDataSetChanged();    //更新資料
                }
            }
            //當藍芽裝置搜尋完畢
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                //中止 搜尋信號中progressDialog顯示
                if(progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
            }
        }
    };

    private class ItemAdapter extends BaseAdapter{

        ArrayList<HashMap<String,String>> itemList;
        public ItemAdapter(ArrayList<HashMap<String,String>> itemList){
            this.itemList = itemList;
        }
        @Override
        public int getCount() {
            return itemList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View mView;
            mView = getLayoutInflater().inflate(R.layout.bluetooth_item,null);
            TextView tvName = mView.findViewById(R.id.tv_device_name);
            TextView tvAddress = mView.findViewById(R.id.tv_device_address);
            tvName.setText(itemList.get(position).get(NAME));
            tvAddress.setText(itemList.get(position).get(ADDRESS));

            return mView;

        }
    }


}
