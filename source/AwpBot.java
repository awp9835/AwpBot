import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;



public class AwpBot
{	
	private class InnerWebSocketServer extends WebSocketServer
	{
		Queue<String> OperationQueue;
		WebSocket EventWs, ApiWs;
		String BotId;

		private InnerWebSocketServer() throws UnknownHostException {}
		public InnerWebSocketServer(int port, Queue<String> opetationQueue) throws UnknownHostException 
		{
			super(new InetSocketAddress(port));
			OperationQueue = opetationQueue;
			System.out.println("Server start... at port：" + port + ".");
		}
		public void onOpen(WebSocket conn, ClientHandshake clientHandshake) 
		{
			String s = conn.getRemoteSocketAddress().getAddress().getHostAddress();
			
			boolean valid = true;
			String auth = null;
			String id = null;
			String role = null;
			/*
			if(!clientHandshake.hasFieldValue("Authorization")) 
			{
				valid = false;
			}
			else
			{
				auth = clientHandshake.getFieldValue("Authorization");
				if(auth == null || !auth.trim().equals("Bearer " + token)) valid = false;		
			}
			*/

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
			System.out.println("Server ready.");
		}
	}
	

	ConcurrentLinkedQueue<String>  EventQueue;
	InnerWebSocketServer Server;
	Vector<AwpBotComponent> Components;

	private void start()
	{
		Server.start();
	}
	public AwpBot(int port) throws UnknownHostException
	{
		EventQueue = new ConcurrentLinkedQueue<String>();
		Server = new InnerWebSocketServer(port,EventQueue);
	}

	//real main
	public static void main2(String args[]) throws InterruptedException,UnknownHostException
	{
		AwpBot botserver = null;
		try
		{
			botserver = new AwpBot(Integer.parseInt(args[0]));
		}
		catch(UnknownHostException e)
		{
			throw(e);
		}
		catch(Exception e)
		{
			botserver = new AwpBot(9835);
		}
		botserver.start();
		botserver.load(); 
		Queue<String> EventQueue = botserver.EventQueue;
		int cnt = 0;
		while(true)
		{
			String event = EventQueue.poll();
			if(event == null) 
			{
				Thread.sleep​(1);
				cnt ++;
				if(cnt >= 1000 * 3600 * 24) //save data every day
				{
					cnt = 0;
					botserver.save();
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
				for(AwpBotComponent comp:botserver.Components)
				{
					String command = comp.handle(event, botserver);
					if(command == null) continue;
					else if(command.equals("continue")) continue;
					else if(command.equals("intercept")) break;
					else if(command.equals("break")) break;
					else switch(command)
					{
					case "save":
						botserver.save();
						break;
					case "reload":
					case "load":
						botserver.load();
						break;
					case "exit":
					case "close":
						System.out.println("Server closed.");
						return;
						//break;
					default:
						break;
					}
				}
				cnt += 1000; //+1s
			}
		}
	}

	public static void sendMessage(WebSocket ws,String s)
	{
		try
		{
			ws.send(s);
		}
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
	}

	private void save()
	{
		for(AwpBotComponent comp:Components)
		{
			boolean result = comp.save();
			if(result) System.out.println(comp.getComponentName() + " save succeed.");
			else System.out.println(comp.getComponentName() + " save failed.");
		}
	}
	private void load()
	{
		if(Components == null)
		{
			Components = new Vector<AwpBotComponent>();
			//add components
			//Components.addElement(new AwpBotComponent());
		}

		for(AwpBotComponent comp:Components)
		{
			boolean result = comp.load();
			if(result) System.out.println(comp.getComponentName() + " load succeed.");
			else System.out.println(comp.getComponentName() + " load with exception.");
		}

	}

	public String getBotId() 
	{
		return Server.BotId;
	}
	public WebSocket getApiWs() 
	{
		return Server.ApiWs;
	}
	public WebSocket getEventWs() 
	{
		return Server.EventWs;
	}

	//test main
	public static void main(String args[]) //for module test
	{
		AsftOneBotMessage a = AsftOneBotMessage.createFromCqString("[CQ:face,id=178]看看我刚拍的照片[CQ:image,file=123.jpg]");
		a.appendText("这是增加的元素");
		System.out.println(a.toString());
		System.out.println("");
		System.out.println(a.toCqString());
		System.out.println("");
		System.out.println(a.toJsonString());
		System.out.println("");
		System.out.println(a.getElementDataValueVector("face","id"));
	}
}
