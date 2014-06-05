import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;


public class Configuration {

	Map<String, Parameter> parameters;
	public Configuration(String fileName,String ints,String strings)
	{
		// setup parameter expectations
		parameters = new HashMap<String, Parameter>();
    	
		StringTokenizer readInts = new StringTokenizer(ints);
		while(readInts.hasMoreTokens())
		{
			parameters.put(readInts.nextToken(), new Parameter(false));
		}
		StringTokenizer readStrings = new StringTokenizer(strings);
		while(readStrings.hasMoreTokens())
		{
			parameters.put(readStrings.nextToken(), new Parameter(true));	
		}
		
    	
    	try
    	{
    		BufferedReader readConfiguration = new BufferedReader(new FileReader(fileName));
        	String currentLine;
        	try
        	{
        		while((currentLine = readConfiguration.readLine()) != null)
        		{
	        		Scanner scanConfig = new Scanner(currentLine);
	        		String thisString;
	        		if((thisString = scanConfig.next()) != null)
	        		{
	        			if(parameters.containsKey(thisString)) // if is a valid parameter we are looking for. Parameter name should be first part of each line, followed by value. What follows doesn't matter
	        			{
	        				Parameter currentParameter = parameters.get(thisString);
	        				String value = scanConfig.next();
	        				if(value == null) continue; // invalid line
	        				if(currentParameter.isString())
	        					currentParameter.setValue(value);
	        				else
	        					currentParameter.setValue(Integer.parseInt(value));
	        			}
	        		}
        		}
        		readConfiguration.close();
        	}
        	catch(IOException ex)
        	{
        		System.err.println("An unknown error occured while reading this file.");
        		return;
        	}
    	}
    	catch(FileNotFoundException ex)
    	{
    		System.err.println("The specified file could not be found.");
    		return;
    	}
    	
    	boolean allHaveParameters = true;
    	System.out.println("Provided parameters:");
    	for (Map.Entry<String, Parameter> entry : parameters.entrySet())
    	{
    		Parameter thisValue = entry.getValue();
    		if(!thisValue.hasValue())
    			allHaveParameters = false;
    		String toOutput;
    		if(!thisValue.isString())
    			toOutput = String.valueOf(thisValue.getInt());
    		else toOutput = thisValue.getString();
    		if(!thisValue.hasValue())
    			toOutput = "<Undefined>";
    		System.out.println("- " + entry.getKey() + " : " + toOutput);
    	}
    	if(!allHaveParameters)
    	{
    		System.err.println("You did not define all the required parameters.");
    		return;
    	}
    	this.valid = true;
	}
	
	boolean valid = false;
	public boolean fileValid()
	{
		return this.valid;
	}
	
	public String getString(String key)
	{
		if(parameters.containsKey(key))
		{
			return parameters.get(key).getString();
		}
		return null;
	}
	
	public Integer getInt(String key)
	{
		if(parameters.containsKey(key))
		{
			return parameters.get(key).getInt();
		}
		return null;
	}

}
