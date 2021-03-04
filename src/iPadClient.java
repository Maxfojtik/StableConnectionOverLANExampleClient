import java.io.IOException;

public class iPadClient 
{
	public static void main(String args[]) throws IOException, InterruptedException
	{
		TCPConnection.init();
		while(true)
		{
			Thread.sleep(1000);
		}
	}
}
