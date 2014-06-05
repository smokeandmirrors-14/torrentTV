import java.util.*;

public class DoTVRetrieve extends TimerTask {
	public Configuration toSend = null;
	boolean runFirstTime = true;
	int timesSinceOutputSchedule = 0;
	public void run()
	{
		if(!runFirstTime)
		{
			runFirstTime = true;
			return;
		}
		if(timesSinceOutputSchedule == 60) timesSinceOutputSchedule = 0;
		(new Retrieve(toSend, timesSinceOutputSchedule == 0)).start();
		timesSinceOutputSchedule++;
	}

}
