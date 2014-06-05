import java.util.Timer;
import java.util.TimerTask;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;

public class TorrentTVFetcher {

	public static void main(String[] args) throws Exception {
		
		// Read configuration
    	if(args.length != 1)
    	{
    		System.out.println("Incorrect syntax. Use \"java TorrentTVFetcher [path]\"");
    		return;
    	}
    	
    	Configuration config = new Configuration(args[0],"port slice_size computer_id total_computers","ip library_location epg_channel_id");
    	if(!config.fileValid())
    	{
    		System.err.println("The provided configuration file is invalid.");
    		return;
    	}
    	
    	System.out.println("Torrent TV Fetcher started. Starting a Retrieve every minute, will start on the next minute.");
    	
    	TimerTask tvRetrieve = new DoTVRetrieve();
    	((DoTVRetrieve)tvRetrieve).toSend = config;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(tvRetrieve, getOneMinuteFromNow(), 1000*60); // run once per minute, starting with the next minute.
        // automatically runs the first time (even before initial delay, the TimerTask takes care of this).
    	
	}
	private static Date getOneMinuteFromNow(){
	    Calendar now = new GregorianCalendar();
	    Calendar result = new GregorianCalendar(
	      now.get(Calendar.YEAR),
	      now.get(Calendar.MONTH),
	      now.get(Calendar.DATE),
	      now.get(Calendar.HOUR_OF_DAY),
	      now.get(Calendar.MINUTE)
	    );
	    return result.getTime();
	  }

}
