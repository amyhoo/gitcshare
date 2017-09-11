package au.com.suncorp.process;

import java.util.Map;

public interface ExecutionStep {
	
	public void execute(Map map) throws Exception;

}
