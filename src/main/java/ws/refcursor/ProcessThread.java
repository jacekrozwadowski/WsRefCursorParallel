package ws.refcursor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ws.refcursor.util.Method;

import org.apache.log4j.Logger;

@Component
@Scope("prototype")
public class ProcessThread implements Callable<List> {

	private static final Logger log = Logger.getLogger("ProcessThread");
	
	@Autowired
	AppService service;
	
	String xml;
	
	Method method;

	public void setXml(String xml) {
		this.xml = xml;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	@Override
	public List call() throws Exception {
		try {
			log.info(method+" - Partition "+xml.hashCode()+" started");	
			
			List list = new ArrayList(0);
			
			if(Method.GET_BY_NAME.equals(method))
				list = service.getObjectsByName(xml);
			else if(Method.GET_BY_OWNER.equals(method))
				list = service.getObjectsByOwner(xml);			
			
			log.info(method+" - Partition "+xml.hashCode()+" ended");
			return list;
		} catch (Exception e) { 
			e.printStackTrace();
			log.error(method+" - Error: "+e.getMessage());
			throw new RuntimeException(e.getMessage());
		}
	}

	
	
}
