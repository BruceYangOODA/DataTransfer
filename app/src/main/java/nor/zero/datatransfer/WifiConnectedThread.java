package nor.zero.datatransfer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static nor.zero.datatransfer.DeviceListFragment.getDevice;

public class WifiConnectedThread extends Thread {

    private MainActivity mainActivity;
    private ServerSocket serverSocket;
    private Socket socket;
    private Handler handler;
    private int port;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private static boolean OK = false;
    private static byte[] okByte;

    public WifiConnectedThread(MainActivity mainActivity,Socket socket){
        this.mainActivity = mainActivity;
        this.socket = socket;
    }

    @Override
    public void run() {


    }


    public void cancel(){
        try {
            socket.close();
            try {
                this.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
