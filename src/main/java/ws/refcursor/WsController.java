package ws.refcursor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

import ws.refcursor.dto.ErrorResponse;
import ws.refcursor.dto.ObjectNameRequest;
import ws.refcursor.dto.ObjectOwnerRequest;
import ws.refcursor.props.MyProperties;
import ws.refcursor.util.RequestValidator;
import ws.refcursor.util.WsThreadPoolExecutor;
import ws.refcursor.util.XMLConverter;
import ws.refcursor.util.ErrorCodes;
import ws.refcursor.util.Method;


@RestController
public class WsController {
	
	private static final Logger log = Logger.getLogger("WsController");
	
	@Autowired
	ConfigurableApplicationContext ctx;
	
	@Autowired
	private MyProperties properties;
	
	@Autowired
	AppService service;
	
	@Autowired
	XMLConverter xMLConverter;
	
	@RequestMapping("/isAlive")
    public String isAlive(@RequestBody String req) {
		return req;
    }
	
	@RequestMapping("/getObjectsByName")
    public List getObjectsByName(@RequestBody List<ObjectNameRequest> request) {
		return getData(request);
	}
	
	@RequestMapping("/getObjectsByOwner")
    public List getObjectsByOwner(@RequestBody List<ObjectOwnerRequest> request) {
		return getData(request);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/getObject/{objectName}")
	public List getObjectByName(@PathVariable String objectName) throws Exception {
		if(objectName.length()>30)
			throw new Exception(ErrorCodes.getErrorMsg(ErrorCodes.ERROR.INVALID_LENGTH));
		
		List<ObjectNameRequest> request = new ArrayList<ObjectNameRequest>();
		request.add(new ObjectNameRequest(objectName));
		return getData(request);
	}
	
	@ExceptionHandler({ Exception.class })
    public String handleException(Exception e) {
		return e.getMessage();
    }
	
	
	private List<ErrorResponse> checkRequest(Object req) {
		RequestValidator rval = new RequestValidator();
		List<ErrorResponse> errors = new ArrayList<ErrorResponse>(0);
		if(req!=null){
			if(req instanceof ObjectNameRequest)
				errors.addAll(rval.checkObjectNameRequest((ObjectNameRequest)req));
			else if(req instanceof ObjectOwnerRequest)
				errors.addAll(rval.checkObjectOwnerRequest((ObjectOwnerRequest)req));
			
		}
		
		return errors;
	}
	
	private List getData(List<?> request) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		
		Method method = Method.ND;
		List response = new ArrayList(0);
		
		if(request!=null && request.size()>0){
			if(request.get(0) instanceof ObjectNameRequest)
				method = Method.GET_BY_NAME;
			else if(request.get(0) instanceof ObjectOwnerRequest)
				method = Method.GET_BY_OWNER;
		} else {
			log.warn(method+" - Request object is empty. Please send some data");
			response.add(new ErrorResponse(ErrorCodes.ERROR.NO_REQUEST_DATA, ""));
			stopWatch.stop();
			log.info(method+" execution time: "+stopWatch.getTotalTimeMillis()+ "ms");
			return response;
		}
		
		int requestLimit = properties.getRequestLimit();
		int responseLimit = properties.getResponseLimit();
		
		if(request.size()>requestLimit){
			log.warn(method+" - Request size "+request.size()+" limited to "+requestLimit);
			response.add(new ErrorResponse(ErrorCodes.ERROR.REQUEST_OVERLIMIT, request.size()+"/"+requestLimit));
			stopWatch.stop();
			log.info(method+" execution time: "+stopWatch.getTotalTimeMillis()+ "ms");
			return response;
		}
		
		
		List<ErrorResponse> errors = new ArrayList<ErrorResponse>(0);
		for(Object req: request) {
			errors.addAll(checkRequest(req));
			if(errors.size()>0){
				log.warn(method+" - Invalid input object format "+req.toString());
				response.clear();
				response.add(new ErrorResponse(ErrorCodes.ERROR.INVALID_REQUEST_DATA, req.toString()));
				response.addAll(errors);
				stopWatch.stop();
				log.info(method+" execution time: "+stopWatch.getTotalTimeMillis()+ "ms");
				return response;
			}
		}
			
		try {
			
			boolean parallel = Boolean.parseBoolean(properties.getParallel());
			
			if(Method.GET_BY_NAME.equals(method)) {
				if(parallel){
					response.addAll(runInParallel(request, method));
				} else {
					response.addAll(service.getObjectsByName(xMLConverter.marshall(request)));
				}
			} else if(Method.GET_BY_OWNER.equals(method)) {
				if(parallel){
					response.addAll(runInParallel(request, method));
				} else {
					response.addAll(service.getObjectsByOwner(xMLConverter.marshall(request)));
				}
			} 
			
		} catch(Exception e){
			log.error(method+" - "+ExceptionUtils.getStackTrace(e) );
			response.add(new ErrorResponse(ErrorCodes.ERROR.CODE_ERROR, e.getMessage(), ""));
			stopWatch.stop();
			log.info(method+" execution time: "+stopWatch.getTotalTimeMillis()+ "ms");
			return response;
		}
			
			
		if(response.size()>responseLimit){
			log.warn(method+" - Response size "+response.size()+" limited to "+responseLimit);
			ErrorResponse er = new ErrorResponse(ErrorCodes.ERROR.RESPONSE_OVERLIMIT, response.size()+"/"+responseLimit);
			response.clear();
			response.add(er);
			stopWatch.stop();
			log.info(method+" execution time: "+stopWatch.getTotalTimeMillis()+ "ms");
			return response;
		}
		
		
		stopWatch.stop();
		log.info(method+" execution time: "+stopWatch.getTotalTimeMillis()+ "ms");
		return response;
			
	}
	
	private List runInParallel(List request, Method method) throws Exception{
		
		List<List<?>> partitions = Lists.partition(request, properties.getPartitionSize());
		BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>( Math.round(properties.getRequestLimit()/properties.getPartitionSize())+1 );
		WsThreadPoolExecutor executor = 
				new WsThreadPoolExecutor(properties.getPoolSize(), 
										 properties.getPoolSize(),
										 10, TimeUnit.MICROSECONDS, queue);
		
		for (List<?> partition: partitions) {			
			ProcessThread pt = (ProcessThread) ctx.getBean(ProcessThread.class);
			pt.setXml(xMLConverter.marshall(new ArrayList(partition)));
			pt.setMethod(method);
			executor.submit(pt);
		}
		
		executor.shutdown();
		while (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
			log.info(method+" - awaiting completion of threads "+(executor.getQueue().size()+executor.getActiveCount())+"/"+partitions.size());
		}
		
		if(executor.isError()){
			StringBuffer sb = new StringBuffer(); 
			for(String error: executor.getErrors()){
				sb.append(error);
				sb.append(System.lineSeparator());
			}
			
			throw new Exception(method+" - one of the threads finish with exception: "+sb.toString());
		} 
		
		return executor.getResults();
	}
	
}
