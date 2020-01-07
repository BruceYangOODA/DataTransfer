package nor.zero.datatransfer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import static nor.zero.datatransfer.Constants.*;

public class FileListFragment extends Fragment {


    private View mContentView;
    private ListView lvFileList;
    private ArrayList<HashMap<String,String>> dataList;
    private static String FILE_PATH = "/sdcard";
    private MainActivity mainActivity;
    private MyAdapter myAdapter;

    public FileListFragment(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(mContentView == null){
            mContentView = inflater.inflate(R.layout.file_list,null);
            ((Button)mContentView.findViewById(R.id.btn_backward)).setOnClickListener(btnClickListener);
            initView();
            fetchRemoteData(FILE_PATH);

        }
        return mContentView;

    }

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String fileType = dataList.get(position).get(FILE_TYPE);
            // 如果是資料夾類型 ,把FILE_PATH置換為 該資料夾
            if(fileType.equals(getString(R.string.sys_msg_file_type_directory))){
                FILE_PATH = dataList.get(position).get(FILE_ABSOLUTE_PATH);
                fetchRemoteData(FILE_PATH);
            }
            // 如果是檔案類型，詢問是否要把檔案傳給另一台手機
            else{
                //todo
                if (true)//(mainActivity.isWifiP2pEnabled)
                {
                    final String fileName = dataList.get(position).get(FILE_NAME);
                    //temp
                    final int fileLength ;
                    int temp = 0;
                    try {
                        FileInputStream fis = new FileInputStream(FILE_PATH +"/"+fileName);
                        temp = fis.available();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    fileLength = temp;
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(getString(R.string.sys_msg_trans_file))
                            .setMessage(getString(R.string.sys_msg_transfer) +" "+ fileName + " \n"+fileLength)
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    synchronized (this) {
                                        dialog.dismiss();
                                        mainActivity.transfer(FILE_PATH, fileName);
                                    }

                                }
                            })
                            .setNegativeButton(getString(R.string.no),null)
                            .show();
                }



/*
                String temp = FILE_PATH+"/"+fileName;
                Log.v("aaa","filePath: " + temp);
                File file = new File(temp);
                Log.v("aaa","fileSize " + file.length());
                Log.v("aaa","fileURI " + file.toURI());

                try {
                    FileInputStream fileInputStream = new FileInputStream(temp);
                    String temp1,temp2,temp3;
                    int index = fileName.lastIndexOf('.');
                    temp1 = fileName.substring(0,index);
                    temp2 = fileName.substring(index+1);
                    temp3 = temp1+"COPY."+temp2;
                    FileOutputStream fileOutputStream = new FileOutputStream(FILE_PATH+"/"+temp3);
                    byte[] byteTemp = new byte[fileInputStream.available()];
                    fileInputStream.read(byteTemp);
                    fileOutputStream.write(byteTemp);
                    fileInputStream.close();
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }*/

            }
        }
    };

    View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //回到上一層資料夾路徑
            if(!FILE_PATH.equals("/sdcard")){
                String temp = FILE_PATH;
                int index = FILE_PATH.lastIndexOf('/');
                FILE_PATH = temp.substring(0,index);
                fetchRemoteData(FILE_PATH);
            }

        }
    };

    private void initView(){

        lvFileList = mContentView.findViewById(R.id.lvFileList);
        dataList = new ArrayList<>();
        myAdapter = new MyAdapter();
        lvFileList.setAdapter(myAdapter);
        lvFileList.setOnItemClickListener(itemClickListener);
    }
    private void fetchRemoteData(String PATH_NAME){
        dataList.clear();
        ArrayList<HashMap<String,String>> tempData = new ArrayList<>(); //暫時把檔案類型的item放這裡面
        String fileType ="";
        String fileName ="";
        int index;
        File[] files = new File(PATH_NAME).listFiles();     //取得PATH_NAME 路徑下的檔案
        for(File file : files){
            HashMap<String,String> item = new HashMap<>();
            if(file.isDirectory())  //檔案是資料夾類型 還是 檔案類型
            {
                fileType = getString(R.string.sys_msg_file_type_directory);
                index = file.toString().lastIndexOf('/');
                fileName = file.toString().substring(index+1);
                item.put(FILE_TYPE,fileType);
                item.put(FILE_NAME,fileName);
                item.put(FILE_ABSOLUTE_PATH,file.getPath());
                dataList.add(item);
            }
            else
            {
                fileType = getString(R.string.sys_msg_file_type_file);
                index = file.toString().lastIndexOf('/');
                fileName = file.toString().substring(index+1);
                item.put(FILE_TYPE,fileType);
                item.put(FILE_NAME,fileName);
                tempData.add(item);

            }
        }
        dataList.addAll(tempData);  //合併 資料夾與檔案類型 item
        myAdapter.notifyDataSetChanged();
    }




    private class MyAdapter extends BaseAdapter{


        @Override
        public int getCount() {
            return dataList.size();
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
            View view = getLayoutInflater().inflate(R.layout.file_items,null);
            ((TextView)view.findViewById(R.id.tvFileType)).setText(dataList.get(position).get(FILE_TYPE));
            ((TextView)view.findViewById(R.id.tvFileName)).setText(dataList.get(position).get(FILE_NAME));
            return view;
        }
    }


}
