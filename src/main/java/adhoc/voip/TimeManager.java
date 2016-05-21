package adhoc.voip;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeManager {

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public String getTime(){
        String str = formatter.format(new Date());
        return str;
    }
}


