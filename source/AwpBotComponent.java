import java.lang.*;
import java.net.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;



public interface AwpBotComponent
{
	public boolean save(); //return succeed
	public boolean load(); //return no except
	public String handle(String event,AwpBot bot); //return command string
	public String getComponentName();
}