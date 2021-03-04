import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

public class TCPConnection 
{
	static TCPServer server;
	static boolean goodConnection = false;
	static long lastPing = 0;
	static List<InetAddress> listAllBroadcastAddresses() throws SocketException {
	    List<InetAddress> broadcastList = new ArrayList<>();
	    Enumeration<NetworkInterface> interfaces 
	      = NetworkInterface.getNetworkInterfaces();
	    while (interfaces.hasMoreElements()) {
	        NetworkInterface networkInterface = interfaces.nextElement();

	        if (networkInterface.isLoopback() || !networkInterface.isUp()) {
	            continue;
	        }

	        networkInterface.getInterfaceAddresses().stream() 
	          .map(a -> a.getBroadcast())
	          .filter(Objects::nonNull)
	          .forEach(broadcastList::add);
	    }
	    return broadcastList;
	}
	public static void askForServerConnection() throws IOException 
	{
		String localIP = null;
		try(final DatagramSocket socket = new DatagramSocket()){
			  socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			  localIP = socket.getLocalAddress().getHostAddress();
		}
		if(localIP!=null)
		{
			localIP = "iPadClient:"+localIP;
			List<InetAddress> allAddresses = listAllBroadcastAddresses();
			for(int i = 0; i < allAddresses.size(); i++)
			{
				InetAddress address = allAddresses.get(i);
				DatagramSocket socket = new DatagramSocket();
		        socket.setBroadcast(true);
	
		        byte[] buffer = localIP.getBytes();
	
		        try
		        {
			        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 4445);
			        socket.send(packet);
			        System.out.println("Broadcasted on "+address);
		        }
		        catch(Exception e) {}
		        socket.close();
			}
		}
    }
	static void init()
	{
		server = new TCPServer();
		new Thread(server).start();
		server.ready();
	}
	static void parse(String in)
	{
		System.out.println("<- "+in);
		String[] params = in.split(":");
		if(params[0].equals("p"))
		{
			goodConnection = true;
			lastPing = System.currentTimeMillis();
		}
	}
	static void logic()
	{
		if(System.currentTimeMillis()-lastPing>3000)
		{
			//server.disconnect();
		}
	}
	static class TCPServer implements Runnable
	{
		private ServerSocket serverSocket;
	    private Socket clientSocket;
	    private PrintWriter out;
	    private BufferedReader in;
	    boolean run = true;
	    boolean r = false;
	    void ready()
	    {
	    	long lastTime = 0;
	    	while(!r)
	    	{
	    		try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		if(System.currentTimeMillis()-lastTime>1000)
	    		{
	    			lastTime = System.currentTimeMillis();
	    			try {
	    				askForServerConnection();
	    			} catch (IOException e) {
	    				// TODO Auto-generated catch block
	    				e.printStackTrace();
	    			}
	    		}
	    	}
	    }
	    void send(String str) 
	    {
	    	System.out.println("-> "+str);
	    	out.println(str);
	    }
		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(4446);
		        System.out.println("Server Started, waiting for connection");
		        clientSocket = serverSocket.accept();
		        out = new PrintWriter(clientSocket.getOutputStream(), true);
		        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		        r = true;
		        System.out.println("Connected!");
				lastPing = System.currentTimeMillis();
		        while(run)
		        {
		        	parse(in.readLine());
		        }
			} catch (IOException e) {
				//e.printStackTrace();
				System.out.println("Network Error: "+e.getMessage());
			}
			finally
			{
				try {
					out.close();
					in.close();
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(run)
			{
				System.out.println("Restarting thread");
				init();
			}
		}
	}
}
