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
			System.out.println("正在启动服务...端口："+port);
		}
		public void onOpen(WebSocket conn, ClientHandshake clientHandshake) 
		{
			String s = conn.getRemoteSocketAddress().getAddress().getHostAddress();
			System.out.println(conn.getRemoteSocketAddress() + " 已连接");
			
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
					System.out.println("同一IP连接数达到上限");
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
			System.out.println(conn.getRemoteSocketAddress() + " 已断开连接");
			//接下来将断开的用户移除
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
			System.out.println("服务已启动");
		}
	}
	

	ConcurrentLinkedQueue<AsftPair<WebSocket, String> > EventQueue = null;
	InnerWebSocketServer Server = null;
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
	
	public static void main2(String args[]) throws InterruptedException,UnknownHostException
	{
		AwpBot server = null;
		try
		{
			server = new AwpBot(Integer.parseInt(args[0]));
		}
		catch(UnknownHostException e)
		{
			throw(e);
		}
		catch(Exception e)
		{
			server = new AwpBot(9835);
		}
		server.start();
		Queue<AsftPair<WebSocket, String> > EventQueue = server.getEventQueue();
		server.load(); //读取数据
		int cnt = 0;
		while(true)
		{
			AsftPair<WebSocket, String> cop = EventQueue.poll();
			if(cop == null) 
			{
				Thread.sleep​(1);
				cnt ++;
				if(cnt >= 1000 * 3600 * 24) //24小时保存一次数据
				{
					cnt = 0;
					if(server.AutoSave) server.save();
				}
				if(cnt % 600000 == 599999 ) //一小时进行一次强制GC
				{
					System.gc();
				}
				continue;
			}
			else
			{
				//在这里处理消息队列

				AsftOneBotMessage a;
				cnt += 1000;
			}
			
		}
	}
	public void sendMessage(WebSocket ws,String s)
	{
		try
		{
			ws.send(s);
		}
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
		finally
		{
		}
	}
	public void save()
	{
		try 
		{
			//在这里添加保存的代码
		} 
		catch (Exception e) 
		{
			
        }
	}
	public void load()
	{
		try 
		{
			//在这里添加载入的代码
		} 
		catch (Exception e) 
		{
			
		}
	}

	public static void main(String args[]) //for module test
	{
		AsftOneBotMessage a = AsftOneBotMessage.createFromText(null);
		System.out.println(a.toString());
		System.out.println("");
		System.out.println(a.toCqString());
		System.out.println("");
		System.out.println(a.toJsonString());
	}
}
