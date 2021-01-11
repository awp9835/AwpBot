import java.lang.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.io.*;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class AwpBot
{	
	private class InnerWebSocketServer extends WebSocketServer
	{
		Queue<AsftPair<WebSocket, String> > OperationQueue;
		Hashtable<String,Vector<WebSocket > >  LinkAlready;
		
		private InnerWebSocketServer() throws UnknownHostException {}
		public InnerWebSocketServer(int port, Queue<AsftPair<WebSocket, String> > opetationQueue) throws UnknownHostException 
		{
			super(new InetSocketAddress(port));
			LinkAlready = new Hashtable<String,Vector<WebSocket > >();
			OperationQueue = opetationQueue;
			System.out.println("Server start... at port：" + port + ".");
		}
		public void onOpen(WebSocket conn, ClientHandshake clientHandshake) 
		{
			String s = conn.getRemoteSocketAddress().getAddress().getHostAddress();
			System.out.println(conn.getRemoteSocketAddress() + " connected.");
			
			synchronized(LinkAlready) 
			{
				Vector<WebSocket> target = LinkAlready.get(s);
				if(target == null) 
				{
					//新的连接
					Vector<WebSocket> nar = new Vector<WebSocket>(0);
					nar.add(conn);
					LinkAlready.put(s,nar);
					return;
				}
				else if(target.size() >= 30) //每个IP最多连接30个ws
				{
					System.out.println("Connect limit : 30.");
					WebSocket ws0 = target.get(0);
					target.add(conn);
					if(ws0 != null)ws0.close();					
				}
				else
				{
					target.add(conn);
				}
			}
		}
		public void onClose(WebSocket conn, int code, String reason, boolean remote) 
		{
			String s = conn.getRemoteSocketAddress().getAddress().getHostAddress();
			System.out.println(conn.getRemoteSocketAddress() + " closed.");
			synchronized(LinkAlready) 
			{
				Vector<WebSocket> target = LinkAlready.get(s);
				if(target == null) return;
				target.remove(conn);
				if(target.isEmpty())
				{
					LinkAlready.remove(s);
				}
			}
		}
		public void onMessage(WebSocket conn, String message) 
		{
			OperationQueue.offer(new AsftPair<WebSocket, String>(conn,message));
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
	

	ConcurrentLinkedQueue<AsftPair<WebSocket, String> > EventQueue;
	InnerWebSocketServer Server;
	Vector<AwpBotComponent> Components;
	boolean AutoSave = false;


	public void start()
	{
		Server.start();
	}
	public AwpBot(int port) throws UnknownHostException
	{
		EventQueue = new ConcurrentLinkedQueue<AsftPair<WebSocket, String> >();
		Server = new InnerWebSocketServer(port,EventQueue);
	}
	public Queue<AsftPair<WebSocket, String> > getEventQueue()
	{
		return EventQueue;
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
		Queue<AsftPair<WebSocket, String> > EventQueue = botserver.getEventQueue();
		int cnt = 0;
		while(true)
		{
			AsftPair<WebSocket, String> cop = EventQueue.poll();
			if(cop == null) 
			{
				Thread.sleep​(1);
				cnt ++;
				if(cnt >= 1000 * 3600 * 24) //save data every day
				{
					cnt = 0;
					if(botserver.AutoSave) botserver.save();
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
					String command = comp.handle(cop.First,cop.Second);
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

	public void save()
	{
		for(AwpBotComponent comp:Components)
		{
			boolean result = comp.save();
			if(result) System.out.println(comp.getComponentName() + " save succeed.");
			else System.out.println(comp.getComponentName() + " save failed.");
		}
	}
	public void load()
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
