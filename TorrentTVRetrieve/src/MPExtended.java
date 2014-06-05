import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.*;
import org.json.simple.parser.*;

class MPExtended {
	
	int port;
	String ip;
	public MPExtended(int requestPort, String requestIp)
	{
		this.ip = requestIp;
		this.port = requestPort;
	}
	
	public boolean doTest()
	{
		// run a connection test
		JSONObject result = this.makeRequestForObject("TVAccessService", "TestConnectionToTVService", null);
		if(result != null)
		{
			if(result.get("Result").toString() != "true")
			{
				System.err.println("Connected to MPExtended but MediaPortal is not running.");
				return false;
			}
		}
		else
		{
			System.err.println("Could not connect to MPExtended.");
			return false;
		}
		return true;
		
	}
	
	JSONObject findLastRecording()
	{
		Object[] toReturn = this.makeRequestForArray("TVAccessService", "GetRecordings", null);
		Long finishTime = null;
		JSONObject lastRecording = null;
		for(Object current : toReturn)
		{
			JSONObject jsonObject;
			if(current instanceof JSONObject)
				jsonObject = (JSONObject) current;
			else
				continue;
			if(jsonObject.get("IsRecording").toString() == "true") continue; // don't want anything which isn't a finished file
				// Attributes:
			// Description, KeepUntilDate, ShouldBeDeleted, KepUntil, ChannelName, Title, TimesWatched, SeriesNum, ChannelId, ScheduleId, IsRecording, FileName, EpisodeName, EpisodePart, Genre, StopTime, EndTime, Id,StartTime, EpisodeNum, IsManual, EpisodeNumber, IsChanged
			long thisFinishTime = Long.parseLong(jsonObject.get("StartTime").toString().substring(6, 19));
			if(finishTime == null || thisFinishTime > finishTime)
			{
				finishTime = thisFinishTime;
				lastRecording = jsonObject;
			}
		}
		return lastRecording;
	}
	
	public Integer getChannelId(String epgChannelId)
	{
		Object[] toReturn = this.makeRequestForArray("TVAccessService", "GetChannelsDetailed", null);
		for(Object current : toReturn)
		{
			JSONObject jsonObject;
			if(current instanceof JSONObject)
				jsonObject = (JSONObject) current;
			else
				continue;
			if(jsonObject.get("ExternalId").toString().equals(epgChannelId))
			{
				System.out.println("The EPG channel ID represents the channel: " + jsonObject.get("Title").toString());
				return Integer.parseInt(jsonObject.get("Id").toString());
			}
		}
		return null;
	}
	
	
	public String getFileNameOfLastRecording()
	{
		JSONObject lastRecording;
		if((lastRecording = findLastRecording()) != null)
		{
			return lastRecording.get("FileName").toString();
		}
		return null;
	}
	
	public boolean scheduleRecordingSoon(int minutesUntilStart, int channelId, int legnthToRecord,int timeEitherSide)
	{
		Map<String,String> requestParameters = new HashMap<String,String>();
    	requestParameters.put("username", "ttv");
    	requestParameters.put("title", "TTVFile");
    	requestParameters.put("startTime",new MPExtendedDate(Calendar.getInstance().getTime()).incMinutes(minutesUntilStart).getFormattedDate());
    	requestParameters.put("endTime",new MPExtendedDate(Calendar.getInstance().getTime()).incMinutes(minutesUntilStart + legnthToRecord).getFormattedDate());
    	requestParameters.put("scheduleType","0");
    	requestParameters.put("channelId",Integer.toString(channelId));
    	requestParameters.put("preRecordInterval",Integer.toString(timeEitherSide));
    	requestParameters.put("postRecordInterval",Integer.toString(timeEitherSide));
    	
    	JSONObject result = this.makeRequestForObject("TVAccessService", "AddScheduleDetailed", requestParameters);
    	
    	if(result != null)
    	{
    		if(result.get("Result").toString() == "true") return true;
    	}
    	return false;
	}
	
	Object[] makeRequestForArray(String serviceType, String functionName, Map<String,String> parameters)
	{
		Object returned = this.makeRequest(serviceType, functionName, parameters);
		if(returned != null && returned instanceof JSONArray)
		{
			return ((JSONArray) returned).toArray();
		}
		return null;
	}
	
	JSONObject makeRequestForObject(String serviceType, String functionName, Map<String,String> parameters)
	{
		Object returned = this.makeRequest(serviceType, functionName, parameters);
		if(returned != null && returned instanceof JSONObject)
		{
			return (JSONObject) returned;
		}
		return null;
	}
	
	Object makeRequest(String serviceType, String functionName, Map<String,String> parameters)
	{
		String requestUrl = "http://" + this.ip + ":" + this.port + "/MPExtended/" + serviceType + "/json/" + functionName;
		String params = "";
		if(!(parameters == null || parameters.size() == 0)) // if has some parameters
		{
			params += "?";
			boolean firstElement = true;
			for(Map.Entry<String, String> entry : parameters.entrySet())
			{
				if(firstElement) firstElement = false;
				else params += "&";
				params += entry.getKey() + "=" + entry.getValue();
			}
			try
			{
				requestUrl += URLEncoder.encode(params, "UTF-8");
			}
			catch(Exception ex)
			{
				System.out.println("Could not encode URL.");
				return null;
			}
		}
		String response = "";
		try
		{
//			System.out.print("Request URL: " + requestUrl + ". ");
			
			URL stream = new URL(requestUrl);
			URLConnection yc = stream.openConnection();
	        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
	        String inputLine;
	        while((inputLine = in.readLine()) != null)
	        	response += inputLine;
	        
	        JSONParser parser=new JSONParser();
	        Object parsed = parser.parse(response);	        
//	        System.out.println("Success.");
	        return parsed;
		}
		catch(Exception ex)
		{
			System.out.println("MPExtended request failed! The error is:");
			System.out.println(ex.getMessage());
		}
		return null;
	}


}
