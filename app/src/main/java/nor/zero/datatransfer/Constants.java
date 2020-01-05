package nor.zero.datatransfer;

public interface Constants {

    public static final String DOWNLOAD_PATH = "/sdcard/Download";

    public static final String CHAT_FRAGMENT = "chatFragment";
    public static final String DEVICE_LIST_FRAGMENT = "deviceListFragment";
    public static final String DEVICE_DETAIL_FRAGMENT = "deviceDetailFragment";
    public static final int WIFI_PORT = 8898;
    public static final int DATA_CHAT = 0;
    public static final int DATA_RECEIVE_START = 1;
    public static final int DATA_RECIVE_FAIL = 2;
    public static final int DATA_CREATE_FILE = 3;
    public static final int DATA_CREATE_FILE_SERVER = 21;
    public static final int DATA_CREATE_FILE_CLIENT = 22;

    // 檔案傳輸協定確認碼
    public static final String DATA_TYPE_MESSAGE = "*#401!";    //文字訊息,檔案傳輸開始,檔案傳輸結束
    public static final String DATA_TRANS_START = "*#402!";     //開始傳送檔案,內容是檔名與檔案大小
    public static final String DATA_TRANS_CONTINUED = "*#403!";     //持續接收檔案
    public static final String DATA_TRANS_END = "*#200!";     //結束接收檔案


    public static final int CHECK_CODE_LENGTH = 6;       //確認碼byte陣列長度
    public static final int READER_LENGTH = 1024;//990;    //接收資料暫存byte陣列長度
    public static final int SENDER_LENGTH = 990;//900;    //送出資料暫存byte陣列長度 ,之後會跟確認碼等陣列合併
    //public static final int CHAT_NAME_LENGTH = 30;  //存放CHAT_DATA 名字的部分 最大長度是UTF-8編碼10個中文字
    public static final int DATA_CHECK_LENGTH = 30;  //存放CHAT_DATA 名字的部分 最大長度是UTF-8編碼10個中文字

    public static final String CHAT_MSG_NAME = "msgName";
    public static final String CHAT_MSG_CONTENT = "msgContent";
    public static final String DATA_MSG_FILE_NAME = "dataName";
    public static final String DATA_MSG_FILE_SIZE = "dataSize";


    //FileListFragment 使用常數
    public static final String FILE_TYPE = "fileType";
    public static final String FILE_NAME = "fileName";
    public static final String FILE_ABSOLUTE_PATH = "filePath";


}
