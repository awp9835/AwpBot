import java.lang.*;
import java.net.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;



public interface AwpBotComponent
{
	public boolean save(); //return succeed
	public boolean load(); //return no except
	public String handle(String event, AwpBotInterface bot); //return global command string
	public String getComponentName();
}