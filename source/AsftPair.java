import java.lang.*;
import java.io.*;
public class AsftPair <T1,T2> implements Serializable
{
	public T1 First = null;
	public T2 Second = null;
	public T1 first(){return First;}
	public T2 second(){return Second;}
	public AsftPair(T1 t1,T2 t2)
	{
		First = t1;
		Second = t2;
	}
}