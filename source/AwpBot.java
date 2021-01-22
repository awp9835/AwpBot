import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;



public class AwpBot implements AwpBotInterface, Runnable
{	
	protected class InnerWebSocketServer extends WebSocketServer
	{
		public Queue<String> OperationQueue;
		public WebSocket EventWs, ApiWs;
		public String BotId;
		public String TokenForAuthorization;

		private InnerWebSocketServer() throws UnknownHostException {}
		public InnerWebSocketServer(int port, Queue<String> opetationQueue) throws UnknownHostException 
		{
			super(new InetSocketAddress(port));
			OperationQueue = opetationQueue;
			System.out.println("Server will start at port：" + port + ".");
		}
		public void onOpen(WebSocket conn, ClientHandshake clientHandshake) 
		{
			String s = conn.getRemoteSocketAddress().getAddress().getHostAddress();
			
			boolean valid = true;
			String auth = null;
			String id = null;
			String role = null;
			if(TokenForAuthorization != null && TokenForAuthorization.trim().length() != 0)
			{
				if(!clientHandshake.hasFieldValue("Authorization")) 
				{
					valid = false;
				}
				else
				{
					auth = clientHandshake.getFieldValue("Authorization");
					if(auth == null) valid = false;		
					else if(! auth.trim().equals("Bearer " + TokenForAuthorization.trim())
							&& ! auth.trim().equals("Token " + TokenForAuthorization.trim())
					) valid = false;	
				}
			}

			if(!clientHandshake.hasFieldValue("X-Self-ID")) 
			{
				valid = false;	
			}
			else
			{
				id = clientHandshake.getFieldValue("X-Self-ID");
				if(id == null || id.trim().length() == 0) valid = false;
			}

			if(!clientHandshake.hasFieldValue("X-Client-Role")) 
			{
				valid = false;	
			}
			else
			{
				role = clientHandshake.getFieldValue("X-Client-Role");
				if(role == null) valid = false;	
				else switch(role.trim())
				{
				case "Event":
				case "API":
				case "Universal":
					break;
				default:
					valid = false;
					break;
				}
			}

			if(valid) 
			{
				System.out.println(conn.getRemoteSocketAddress() + " connected.");
				synchronized(this)
				{
					BotId = id.trim();
					switch(role.trim())
					{
					case "Event":
						if(EventWs != null) EventWs.close();
						EventWs = conn;
						break;
					case "API":
						if(ApiWs != null) ApiWs.close();
						ApiWs = conn;
						break;
					default:
					case "Universal":
						if(EventWs != null)  EventWs.close();
						if(ApiWs != null && ApiWs != EventWs) ApiWs.close();
						EventWs = conn;
						ApiWs = conn;
						break;
					}
				}
			}
			else 
			{
				System.out.println(conn.getRemoteSocketAddress() + ": invalid connect.");
				conn.close();
			}	
		}
		public void onClose(WebSocket conn, int code, String reason, boolean remote) 
		{
			String s = conn.getRemoteSocketAddress().getAddress().getHostAddress();
			System.out.println(conn.getRemoteSocketAddress() + " closed.");
		}
		public void onMessage(WebSocket conn, String message) 
		{
			OperationQueue.offer(message);
		}
		public void onError(WebSocket conn, Exception e) 
		{
			if( conn != null ) 
			{
				//System.out.println(conn.toString() + "\n" + e.toString());
			}
		}
		public void onStart() 
		{
			System.out.println("WebSocket server start.");
		}
	}
	

	protected ConcurrentLinkedQueue<String>  EventQueue;
	protected InnerWebSocketServer Server;
	protected Vector<AwpBotComponent> Components;


	public AwpBot(int port) throws UnknownHostException
	{
		EventQueue = new ConcurrentLinkedQueue<String>();
		Server = new InnerWebSocketServer(port,EventQueue);
	}

	//real main
	public static void main(String args[]) throws InterruptedException,UnknownHostException
	{
		AsftOneBotEvent ev;
		AsftOneBotMessage m;
		AsftOneBotApi api;
		AwpBotComponentParent abcp;
		AwpBot awpbot = null;
		try
		{
			awpbot = new AwpBot(Integer.parseInt(args[0]));
		}
		catch(UnknownHostException e)
		{
			throw(e);
		}
		catch(Exception e)
		{
			awpbot = new AwpBot(9835);
		}
		awpbot.run();
	}


	protected void config()
	{
		//Server.TokenForAuthorization = null;
		if(Components == null)
		{
			Components = new Vector<AwpBotComponent>();
			//add components
			//Components.addElement(new AwpBotComponent());
			//Components.addElement(new Component_ApiHandle());
		}
	}
	protected void startws()
	{
		Server.start();
	}
	protected void save()
	{
		for(AwpBotComponent comp:Components)
		{
			boolean result = comp.save();
			if(result) System.out.println(comp.getComponentName() + " save succeed.");
			else System.out.println(comp.getComponentName() + " save failed.");
		}
		System.out.println("Saved.");
	}
	protected void load()
	{
		for(AwpBotComponent comp:Components)
		{
			boolean result = comp.load();
			if(result) System.out.println(comp.getComponentName() + " load succeed.");
			else System.out.println(comp.getComponentName() + " load with exception.");
		}
		System.out.println("Load.");
	}
	protected boolean excommand(String command)
	{
		if(command == null) return true;
		switch(command.trim().toLowerCase())
		{
		case "break":
			return false;
			//break;
		case "save":
			save();
			break;
		case "reload":
		case "load":
			load();
			break;
		case "config":
		case "reconfig":
			config();
			break;
		case "exit":
		case "close":
			save();
			System.out.println("Bot " + getBotId() + ": Closed.");
			{
			Thread th = Thread.currentThread();
			if(th.getName().equals("main")) System.exit(0);
			else th.interrupt();
			}
			return false;
			//break;
		case "stop":
		case "abort":
			System.out.println("Bot " + getBotId() + ": Abort.");
			{
			Thread th = Thread.currentThread();
			if(th.getName().equals("main")) System.exit(0);
			else th.interrupt();
			}
			return false;
			//break;
		default:
		case "continue":
			break;
		}
		return true;
	}


	@Override
	public String getBotId() 
	{
		return Server.BotId;
	}
	@Override
	public WebSocket getApiWs() 
	{
		return Server.ApiWs;
	}
	@Override
	public WebSocket getEventWs() 
	{
		return Server.EventWs;
	}
	@Override
	public void run()
	{
		config();
		startws();
		load(); 
		int cnt = 0;
		while(true)
		{
			String event = EventQueue.poll();
			if(event == null) 
			{
				try
				{
					Thread.sleep​(1);
				}
				catch(InterruptedException e)
				{
					System.out.println(e.toString());
				}
				cnt ++;
				if(cnt >= 1000 * 3600 * 24) //save data every day
				{
					cnt = 0; save();
				}
				if(cnt % 900000 == 450000 ) //force GC every quarter
				{
					System.gc();
				}
				continue;
			}
			else
			{
				//handle messages by every component
				for(AwpBotComponent comp: Components)
				{
					String command = comp.handle(event, this);
					if(excommand(command)) continue;
					else break;
				}
				cnt += 1000; //+1s
			}
		}
	}
}
