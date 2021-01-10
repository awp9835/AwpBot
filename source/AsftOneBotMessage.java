import java.lang.*;
import org.json.*;
import java.util.*;

public class AsftOneBotMessage
{
	//factory function
	public static boolean paraIsTrue(String str)
	{
		if(str == null) return false;
		try
		{
			return Integer.parseInt(str) != 0;
		}
		catch (Exception e)
		{
			switch(str)
			{		
			case "false": case "FALSE": case "False":
			case "F": case "f":
			case "N": case "n": case "No": case "no":
			case "假": case "错":
				return false; //break;

			default:
			case "true": case "TRUE": case "True":
			case "T": case "t":
			case "Y": case "y": case "Yes": case "yes":
			case "真": case "对":
				return true; //break
			}
		}
	}

	//member
	private JSONArray MessageElements;

	//create functions
	private void create_jsonMode(String msgjs)
	{
		try
		{
			MessageElements = new JSONArray(msgjs);
		}
		catch(JSONException e1)
		{
			MessageElements = new JSONArray();
			try
			{
				JSONObject obj = new JSONObject(msgjs);
				MessageElements.put(obj);
			}
			catch(JSONException e2)
			{
				System.out.println(e2.toString());
			}
		}
	}
	private void create_cqMode(String cqstr)
	{
		
		MessageElements = new JSONArray();
		String remain = cqstr;
		while(remain != null)
		{
			String[] part = remain.split("\\[CQ:",2);	
			if(part[0].length() != 0) 
			{	
				try
				{
					JSONObject data = new JSONObject();
					data.put("text",part[0].replaceAll("&#91;","[").replaceAll("&#93;","]").replaceAll("&amp;","&"));
					JSONObject obj = new JSONObject();
					obj.put("type","text");
					obj.put("data",data);
					MessageElements.put(obj);
				}
				catch(JSONException e)
				{
					System.out.println(e.toString());
				}
			}
			if(part.length == 1) //no [CQ:
			{
				remain = null;
			}
			else //has [CQ:
			{
				part = part[1].split("\\]",2); 
				//part[0] is CQCode without CQ:
				String[] cq = part[0].split(",");
				if(cq[0].length() != 0)
				{
					try
					{
						JSONObject data = new JSONObject();
						//Put data
						for(int i = 1;i < cq.length; i++)
						{
							String[] atr = cq[i].split("=",2);
							if(atr[0].trim().length() == 0) continue;
							if(atr.length == 1) 
							{
								data.put(atr[0].trim(),"");
							}
							else
							{
								data.put(atr[0].trim(),
									atr[1].replaceAll("&#91;","[").replaceAll("&#93;","]").replaceAll("&#44;",",").replaceAll("&amp;","&")
								);
							}
						}
						JSONObject obj = new JSONObject();
						obj.put("type",cq[0]);
						obj.put("data",data);
						MessageElements.put(obj);
					}
					catch(JSONException e)
					{
						System.out.println(e.toString());
					}
				}
				if(part.length == 1) //no remain
				{
					remain = null;
				}
				else //has remain
				{
					remain = part[1];
				}
			}
		}
	}
	private void create_strOnly(String str) 
	{
		MessageElements = new JSONArray();
		if(str == null || str.length() == 0) return;
		try
		{
			JSONObject data = new JSONObject();
			data.put("text",str);
			JSONObject obj = new JSONObject();
			obj.put("type","text");
			obj.put("data",data);
			MessageElements.put(obj);
		}
		catch(JSONException e)
		{
			System.out.println(e.toString());
		}
	}
	private AsftOneBotMessage(){}

	public static AsftOneBotMessage createFromText(String text)
	{
		AsftOneBotMessage temp = new AsftOneBotMessage();
		temp.create_strOnly(text);
		return temp;
	}
	public static AsftOneBotMessage createFromCqString(String cqstr)
	{
		AsftOneBotMessage temp = new AsftOneBotMessage();
		temp.create_cqMode(cqstr);
		return temp;
	}
	public static AsftOneBotMessage createFromJsonString(String js)
	{
		AsftOneBotMessage temp = new AsftOneBotMessage();
		temp.create_jsonMode(js);
		return temp;
	}
	public static AsftOneBotMessage createEmpty()
	{
		return createFromText(null);
	}

	//to str functions
	public String toString()
	{
		StringBuilder strb = new StringBuilder(0x40);
		for(Object elem : MessageElements)
		{
			JSONObject obj = (JSONObject)elem; 
			if(obj.optString("type").equals("text"))
			{
				strb.append(obj.optJSONObject("data").optString("text"));
			}
			else
			{
				strb.append("[Object:");
				strb.append(obj.toString());
				strb.append("]");
			}
		}
		return strb.toString();
	}
	public String toCqString()
	{
		StringBuilder strb = new StringBuilder(0x40);
		for(Object elem : MessageElements)
		{
			JSONObject obj = (JSONObject)elem; 
			if(obj.optString("type").equals("text"))
			{
				strb.append(obj.optJSONObject("data").optString("text")
					  .replaceAll("&","&amp;").replaceAll("\\[","&#91;").replaceAll("\\]","&#93;")
					  );
			}
			else
			{
				strb.append("[CQ:");
				strb.append(obj.optString("type"));
				strb.append(",");
				obj = obj.optJSONObject("data");
				Set<String> keys = (Set<String>)obj.keySet();
				for(String key :keys)
				{
					strb.append(key);
					strb.append("=");
					strb.append(obj.optString(key)
						 .replaceAll("&","&amp;").replaceAll("\\[","&#91;").replaceAll("\\]","&#93;").replaceAll(",","&#44;")); 
					strb.append(",");
				}
				strb.delete(strb.length() - 1,strb.length()); //remove last comma
				strb.append("]");
			}
		}
		return strb.toString();
	}
	public String toJsonString()
	{
		return MessageElements.toString();
	}

	//append functions
	public boolean appendAsftOneBotMessage(AsftOneBotMessage secodary)
	{
		for(Object elem : secodary.MessageElements)
		{
			JSONObject obj = (JSONObject)elem; 
			MessageElements.put(obj);
		}
		return true;
	}
	public boolean appendText(String text)
	{
		if(text == null || text.length() == 0) return false;
		try
		{
			JSONObject data = new JSONObject();
			data.put("text",str);
			JSONObject obj = new JSONObject();
			obj.put("type","text");
			obj.put("data",data);
			MessageElements.put(obj);
		}
		catch(JSONException e)
		{
			System.out.println(e.toString());
			return false;
		}
		return true;
	}
	public boolean appendFace(String text)
	{
		if(text == null || text.length() == 0) return false;
		try
		{
			JSONObject data = new JSONObject();
			data.put("text",str);
			JSONObject obj = new JSONObject();
			obj.put("type","text");
			obj.put("data",data);
			MessageElements.put(obj);
		}
		catch(JSONException e)
		{
			System.out.println(e.toString());
			return false;
		}
		return true;
	}

	//get functions
}