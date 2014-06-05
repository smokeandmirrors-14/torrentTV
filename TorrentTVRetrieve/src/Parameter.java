
public class Parameter {
	
	boolean stringType = false;
	String stringValue;
	int intValue;
	boolean valueAssigned = false;
	
	
	public void setValue(String toStore)
	{
		if(!this.valueAssigned)
		{
			this.stringValue = toStore;
			this.stringType = true;
			this.valueAssigned = true;
		}
		else throw new UnsupportedOperationException("Parameter already set!");
	}
	public void setValue(int toStore)
	{
		if(!this.valueAssigned)
		{
			this.intValue = toStore;
			this.valueAssigned = true;	
		}
		else throw new UnsupportedOperationException("Parameter already set!");
	}
	
	public Parameter(boolean isString) // yet to be defined
	{
		this.stringType = isString;
	}
	
	
	// accessor methods
	
	public boolean isString()
	{
		return this.stringType;
	}
	
	public boolean hasValue()
	{
		return this.valueAssigned;
	}
	
	public String getString()
	{
		if(this.stringType)
			return stringValue;
		throw new UnsupportedOperationException("Parameter is not a string.");
	}
	
	public int getInt()
	{
		if(!this.stringType)
			return intValue;
		throw new UnsupportedOperationException("Parameter is not an integer.");
	}
	
}
