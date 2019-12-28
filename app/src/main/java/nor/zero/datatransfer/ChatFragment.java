package nor.zero.datatransfer;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.fragment.app.ListFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFragment extends ListFragment {

    private View mChatView = null;
    private List<HashMap<String,String>> chatList = new ArrayList<>();
    private String[] test = {"123","456"};
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mChatView = inflater.inflate(R.layout.chat_list,null);

        return mChatView;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new ChatListAdapter(getActivity(), R.layout.chat_contnet, test));
    }


    private class ChatListAdapter extends ArrayAdapter<String> {//適配器
        private List<WifiP2pDevice> items;
        public ChatListAdapter(Context context, int textViewResourceId,
                                   String[] content) {
            super(context, textViewResourceId, content);

        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.chat_contnet, null);
            }
            return v;
        }


    }



}
