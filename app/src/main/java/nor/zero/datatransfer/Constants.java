package nor.zero.datatransfer;

public interface Constants {

    public static final String DOWNLOAD_PATH = "/sdcard/Download";

    public static final String CHAT_FRAGMENT = "chatFragment";
    public static final String DEVICE_LIST_FRAGMENT = "deviceListFragment";
    public static final String DEVICE_DETAIL_FRAGMENT = "deviceDetailFragment";
    public static final int WIFI_PORT = 8898;
    public static final int DATA_CHAT = 0;

    /*
    *   Handler 使用常數
     */
    public static final int DATA_RECEIVE_START = 1;
    public static final int DATA_RECEIVE_FAIL = 2;
    public static final int DATA_PROGRESS = 3;  //通知目前傳收檔案進度%
    public static final int DATA_CREATE_FILE = 4;
    public static final int DATA_TRANS_NOTE = 5; //開始傳送檔案,通知檔案大小
   // public static final int DATA_TRANS_PROGRESS = 6;    //通知目前傳送檔案進度%

    public static final int DATA_CREATE_FILE_SERVER = 21;
    public static final int DATA_CREATE_FILE_CLIENT = 22;
    public static final int BLUETOOTH_THREAD_STATE = 7;
    public static final int BLUETOOTH_DEVICE_NAME = 8;


    // 檔案傳輸協定確認碼
    public static final String DATA_TYPE_MESSAGE = "*#401!";    //文字訊息,檔案傳輸開始,檔案傳輸結束
    public static final String DATA_TRANS_START = "*#402!";     //開始傳送檔案,內容是檔名與檔案大小
    public static final String DATA_TRANS_CONTINUED = "*#403!";     //持續接收檔案
    public static final String DATA_TRANS_END = "*#200!";     //結束接收檔案
    public static final String DATA_DISCONNECT = "*#404!";     //結束連線


    public static final int CHECK_CODE_LENGTH = 6;       //確認碼byte陣列長度
    public static final int READER_LENGTH = 990;//990;    //接收資料暫存byte陣列長度,藍芽訊號最多讀990byte
    public static final int SENDER_LENGTH = 900;//900;    //送出資料暫存byte陣列長度 ,之後會跟確認碼等陣列合併
    //public static final int CHAT_NAME_LENGTH = 30;  //存放CHAT_DATA 名字的部分 最大長度是UTF-8編碼10個中文字
    public static final int DATA_CHECK_LENGTH = 30;  //存放CHAT_DATA 名字的部分 最大長度是UTF-8編碼10個中文字

    public static final String CHAT_MSG_NAME = "msgName";
    public static final String CHAT_MSG_CONTENT = "msgContent";
    public static final String DATA_MSG_FILE_NAME = "fileName";
    public static final String DATA_MSG_FILE_SIZE = "fileSize";
    public static final String DATA_MSG_FILE_CURRENT_SIZE = "currentSize" ;


    //FileListFragment 使用常數
    public static final String FILE_TYPE = "fileType";
    public static final String FILE_NAME = "fileName";
    public static final String FILE_ABSOLUTE_PATH = "filePath";

    /*
    *BluetoothConnectService 使用常數
    */
    public static final String CURRENT_STATE = "currentState" ;
    public static final String DEVICE_NAME = "deviceName" ;
    public static final int STATE_NONE = 0; //目前無連接
    public static final int STATE_LISTEN = 1; //目前監聽中
    public static final int STATE_CONNECTING = 2; //目前連接中
    public static final int STATE_CONNECTED = 3; //已連接上藍芽裝置


}
