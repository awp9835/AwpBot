import java.lang.*;
import org.java_websocket.WebSocket;

public interface AwpBotInterface
{
	public String getBotId();
	public WebSocket getApiWs();
	public WebSocket getEventWs();
}