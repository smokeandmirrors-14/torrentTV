import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;


public class Retrieve extends Thread {
	
	public boolean downloadRunning = false;

	public static int whoWillRun(long timeInMinutes, int numberOfComputers, int thisId, int sliceSize)
	{
		timeInMinutes += 1;
    	if((timeInMinutes % sliceSize) == 0) // is it a minute a slice should be recorded (on some server)
    	{
    		long serverId = timeInMinutes / sliceSize;
    		while(serverId > (numberOfComputers - 1))
    			serverId -= numberOfComputers;
    		if(thisId == serverId) // we're the one to run!
    			return 2;
    		return 1;
    	}
    	return 0;
	}
	
	public static void nextHourSchedule(long timeInMinutes,int numberOfComputers, int thisId, int sliceSize)
	{
		System.out.println("==== Run Schedule for the next 60 minutes ====");
		for(long i = timeInMinutes; i < (timeInMinutes + 60); i++)
		{
			int whoWillRun = whoWillRun((long) i, numberOfComputers,thisId, sliceSize);
			
			String minutes = Integer.toString((int) i%60);
			String toPrint = ((i/60)%60) + ":" + (minutes.length() == 1 ? "0" : "") + minutes + ": ";
			switch(whoWillRun)
			{
				case 2:
					toPrint += "Will run.";
					break;
				case 1:
					toPrint += "Another server will run.";
					break;
				case 0:
					toPrint += "No server will run.";
					break;
			}
			System.out.println(toPrint);
		}
		System.out.println("==== END Run Schedule ====");
	}
	
	Configuration config;
	boolean outputSchedule;
	
	public Retrieve(Configuration providedConfig, boolean shouldOutputSchedule)
	{
		this.config = providedConfig;
		this.outputSchedule = shouldOutputSchedule;
	}
    public void run(){
		
		// Setup MPExtended connection.
    	MPExtended setupRequest = new MPExtended(config.getInt("port"),config.getString("ip"));
    	if(!setupRequest.doTest())
    	{
    		System.out.println("Cannot continue without connecting to MPExtended.");
    		return;
    	}
    	Integer mediaPortalChannelID = setupRequest.getChannelId(config.getString("epg_channel_id"));
    	if(mediaPortalChannelID == null)
    	{
    		System.err.println("The requested EPG channel ID could not be found in MediaPortal. Cannot continue. Is this a valid EPG ID? Does MediaPortal have EPG data and channels mapped?");
    		return;
    	}
    	
    	
    	// Determine if it should be run.
    	long timeInMinutes = System.currentTimeMillis() / 60000L; // get the number of minutes since 1970
    	int thisId = config.getInt("computer_id");
    	int sliceSize = config.getInt("slice_size");
    	int numberOfComputers = config.getInt("total_computers");
    	
    	if(outputSchedule) nextHourSchedule(timeInMinutes,numberOfComputers,thisId, sliceSize); // print out a schedule for diagnostic purposes
    	if(whoWillRun(timeInMinutes, numberOfComputers, thisId, sliceSize) == 2) // if this program is scheduled to start
    	{
        	// Begin timeshift?
    		
    		// Book recording
    		int timeEitherSide = 1;
    		int minutesUntilStartRecording = 1;
    		int recordTime = config.getInt("slice_size");
    		if(!setupRequest.scheduleRecordingSoon(minutesUntilStartRecording, mediaPortalChannelID, recordTime, timeEitherSide))
    			// setup recording in one minute on the defined channel being as long as the slice size, with 1 minute either side
        	{
        		System.err.println("There was a problem scheduling a recording. The program cannot continue.");
        		return;
        	}
        	System.out.println("Recording scheduled. Waiting...");
        	
        	// Wait
        	int minutesToWait = (minutesUntilStartRecording - timeEitherSide) + (2*timeEitherSide + recordTime) + 1; 
        	try
        	{
        		Thread.sleep(60*1000*minutesToWait);
        	}
        	catch(Exception ex)
        	{
        		System.err.println("Something interrupted the sleep timer. Cannot determine if file has been recorded, cannot continue.");
        		return;
        	}
        	
        	System.out.println("Recording finished.");
        	
        	// Transcode
        	String fileName = setupRequest.getFileNameOfLastRecording();
        	if(fileName == null)
        	{
        		System.err.println("The recording could not be found in MPExtended. Cannot continue.");
        		return;
        	}
        	File lastRecording = new File(fileName);
        	if(!lastRecording.exists())
        	{
        		System.err.println("The recording file could not be found. Are you on the same computer at the MediaPortal server? Cannot continue.");
        		return;
        	}
        	System.out.print("Begining transcoding process...");      	

        	// Generate file name to use.

        	int year = Integer.parseInt((new SimpleDateFormat("y").format(Calendar.getInstance().getTime())));
        	int month = Integer.parseInt((new SimpleDateFormat("M").format(Calendar.getInstance().getTime())));
        	int day = Integer.parseInt(new SimpleDateFormat("d").format(Calendar.getInstance().getTime()));
        	String time = (new SimpleDateFormat("HHmm").format(Calendar.getInstance().getTime()));
        	

        	String outputFileName = "channel_" + config.getString("epg_channel_id") + "_year_" + year + "_mon_" + month + "_day_" + day + "_" + time + ".mp4";
        	
        	String outputFilePath = config.getString("library_location") + outputFileName;
        	System.out.println("Transcoding to file: " + outputFilePath);
        	
        	if(!isWindows())
        	{
        		System.err.println("With much regret, I must inform you that transcoding can currently only be performed on Windows machines. Cannot continue. :(");
        		return;
        	}
        		
        	// Stream and transcode
            try{
            	int startDuration = timeEitherSide * 60;
            	int stopDuration = recordTime * 60;
            	
            	String handbrakeCommand = "HandBrakeCLI -i \"" + fileName + "\" -o \"" + outputFilePath + "\" -Z Normal --start-at duration:" +  startDuration + " --stop-at duration:" + stopDuration;
            	String transcodeCommand = "cmd /c start /wait " + handbrakeCommand;
            	
            	System.out.println("Running command:");
            	System.out.println(transcodeCommand);
            	
            	Process tr = Runtime.getRuntime().exec(transcodeCommand);
                tr.waitFor();
            } catch (Exception ex) {
                System.err.println("An error occured while transcoding the video. Cannot continue.");
                return;
            }
            System.out.println("Finished.");
           
            // Check encoded file actually exists
            File encodedFile = new File(outputFilePath);
        	if(!encodedFile.exists())
        	{
        		System.err.println("The newly encoded file could not be found. Is HandBrakeCLI installed on your system? Cannot continue.");
        		return;
        	}
        	System.out.println("Encode sucessful. Handing off to the 'glue'.");
            // Call the glue
        	
    	}
    	else
    	{
    		System.out.println("Not scheduled to run. Exiting.");
    		return;
    	}
    }

	public static boolean isWindows() {

		String OS = System.getProperty("os.name").toLowerCase();
		return (OS.indexOf("win") >= 0);
 
	}

}
