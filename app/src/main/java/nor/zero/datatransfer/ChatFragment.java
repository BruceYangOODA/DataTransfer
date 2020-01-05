package nor.zero.datatransfer;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import static nor.zero.datatransfer.DeviceListFragment.*;
import static nor.zero.datatransfer.DeviceDetailFragment.*;

public class ChatFragment extends Fragment {

    private View mChatView = null;
    public static ArrayList<HashMap<String,String>> chatList;
    private ChatListAdapter chatListAdapter;
    private EditText etChatInput;
    private MainActivity mainActivity;
    private ListView listView;
    private static String nickName ="";
    private static HashSet<String> nickNameCheckSet = new HashSet<>();

    public ChatFragment(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    View.OnClickListener btnChatSendClick = new View.OnClickListener() {
        @Override
        public void onClick(View v)  {
            String chatMessage = etChatInput.getText().toString();
            // 不是輸入空字串,就傳遞EditText 的內容,並清空EditText
            if(!chatMessage.equals("")){
                //記錄在本機的ChatFragment
                //DeviceDetailFragment 取得暱稱設定，如果未設定,預設為本機的名字
                nickName = etNickName.getText().toString();
                if(nickName.equals(""))
                    nickName = getDevice().deviceName; //DeviceListFragment 取得本機的名字
                nickNameCheckSet.add(nickName);     //記錄自己名字的記錄,如果重複,set就不會記錄
                HashMap<String,String> addMessage = new HashMap<>();
                addMessage.put(Constants.CHAT_MSG_NAME,nickName);
                addMessage.put(Constants.CHAT_MSG_CONTENT,chatMessage);
                addChatContent(addMessage); //更新本機的聊天訊息
                mainActivity.write(nickName,chatMessage);

                etChatInput.setText("");

                /*  改用wifiConnectedThread
                if(mainActivity.wifiServerThread != null){
                    //寫出給對方機器
                    mainActivity.wifiServerThread.write(chatMessage);
                    etChatInput.setText("");
                }
                else if(mainActivity.wifiClientThread != null){
                    mainActivity.wifiClientThread.write(chatMessage);
                    etChatInput.setText("");
                }*/
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mChatView = inflater.inflate(R.layout.chat_list,null);
        listView = mChatView.findViewById(R.id.lvChat);
        chatListAdapter = new ChatListAdapter();
        chatList = new ArrayList<HashMap<String, String>>();
        listView.setAdapter(chatListAdapter);

        ((Button)mChatView.findViewById(R.id.btnChatSend)).setOnClickListener(btnChatSendClick);
        etChatInput = mChatView.findViewById(R.id.etChatInput);

        return mChatView;
    }


    public void addChatContent(HashMap<String,String > newMsg){
        chatList.add(newMsg);
        chatListAdapter.notifyDataSetChanged();
    }


    private class ChatListAdapter extends BaseAdapter {//適配器

        @Override
        public int getCount() {
            return chatList.size();
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
            //View v = convertView;
            //if (v == null) {
            View v ;
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
             v = vi.inflate(R.layout.chat_contnet, null);
             TextView tvName = v.findViewById(R.id.tvChatID);
             TextView tvChat = v.findViewById(R.id.tvChatContent);
             tvName.setText(chatList.get(position).get(Constants.CHAT_MSG_NAME));
             tvChat.setText(chatList.get(position).get(Constants.CHAT_MSG_CONTENT));
             String nameCheker = tvName.getText().toString();
             boolean isMe = nickNameCheckSet.contains(nameCheker);
             Log.v("aaa",""+nickNameCheckSet.size());
             if(isMe){ //是自己發出的訊息
                 tvChat.setTextColor(getResources().getColor(R.color.Purple));
             }
             else {
                 tvChat.setTextColor(getResources().getColor(R.color.MidnightBlue));
             }

           // }
            return v;
        }


    }



}
