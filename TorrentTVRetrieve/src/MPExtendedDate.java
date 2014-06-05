import java.text.SimpleDateFormat;
import java.util.Date;


public class MPExtendedDate extends Date {
	SimpleDateFormat MPExtendedStyle;
	SimpleDateFormat OutputStyle;
	
	public MPExtendedDate(Date toComeFrom)
	{
		super();
		super.setTime(toComeFrom.getTime());
		setupDateFormat();
	}
	
	void setupDateFormat()
	{
		this.MPExtendedStyle = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
	}
	
	
	public String getFormattedDate()
	{
		return MPExtendedStyle.format(this);
	}
	
	public MPExtendedDate incMinutes(int minutes)
	{
		this.setTime(this.getTime() + minutes*1000*60);
		return this;
	}	
}
