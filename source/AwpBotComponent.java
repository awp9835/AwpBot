import java.lang.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public interface AwpBotComponent
{
	public boolean save(); //return succeed
	public boolean load(); //return no except
	public String handle(WebSocket ws, String message); //return command string
	public String getComponentName();
}