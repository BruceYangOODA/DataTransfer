package nor.zero.datatransfer;

public interface Constants {

    public static final String CHAT_FRAGMENT = "chatFragment";
    public static final String DEVICE_LIST_FRAGMENT = "deviceListFragment";
    public static final String DEVICE_DETAIL_FRAGMENT = "deviceDetailFragment";
    public static final int WIFI_PORT = 8898;
    public static final int DATA_CHAT = 0;

    // 文字訊息,檔案傳輸開始,檔案傳輸結束
    public static final String[] DATA_TYPE = {"*#401!","*#403!","*#404!"};
    public static final int CHECK_LENGTH = 6;       //確認碼byte陣列長度
    public static final int READER_LENGTH = 990;    //接收資料暫存byte陣列長度
    public static final int SENDER_LENGTH = 900;    //送出資料暫存byte陣列長度 ,之後會跟確認碼等陣列合併
    public static final int CHAT_NAME_LENGTH = 30;  //存放CHAT_DATA 名字的部分 最大長度是UTF-8編碼10個中文字

    public static final String CHAT_MSG_NAME = "msgName";
    public static final String CHAT_MSG_CONTENT = "msgContent";

}
