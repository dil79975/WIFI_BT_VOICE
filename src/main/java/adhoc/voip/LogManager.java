package adhoc.voip;


import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by DIL on 2016/3/11.
 */
public class LogManager{
    String fileName;
    TimeManager t = new TimeManager();

    public LogManager(){}

    public LogManager(String Name){
        this.fileName = Name;
    }

    public void Start(String fileName,String time){
        writeDataToFile("WifiDirectLog","來自"+fileName+"\n");
        writeDataToFile("WifiDirectLog","傳送端開始時間"+time+"\n");
        writeDataToFile("WifiDirectLog","接收端開始時間"+t.getTime()+"\n");

        Log.v("GG", " 傳送端開始時間: " + time);
        Log.v("GG", " 接送端開始時間: " + t.getTime());

    }

    public void End(String fileName,String time){
        writeDataToFile("WifiDirectLog","來自"+fileName+"\n");
        writeDataToFile("WifiDirectLog","傳送端結束時間"+time+"\n");
        writeDataToFile("WifiDirectLog","接收端結束時間"+t.getTime()+"\n");

        Log.v("GG", " 傳送端結束時間: " + time);
        Log.v("GG", " 接收端結束時間: " + t.getTime());
    }

    private void writeDataToFile(String filename, String data){
        try {
            FileOutputStream fout = new FileOutputStream("/sdcard/"+filename+".txt",true);
            fout.write(data.getBytes());
            fout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
