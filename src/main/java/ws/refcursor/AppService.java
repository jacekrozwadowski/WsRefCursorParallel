package ws.refcursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import oracle.jdbc.OracleTypes;
import ws.refcursor.props.MyProperties;
import ws.refcursor.util.PojoGenerator;
import ws.refcursor.util.XmlFormatter;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;


@Service
public class AppService {
	
	private static final Logger log = Logger.getLogger("AppService");
	
	private static String GET_OBJECTS_BY_NAME = "WS_REF_CURSOR_SQL.GET_OBJECTS_BY_NAME";
	private static String GET_OBJECTS_BY_OWNER = "WS_REF_CURSOR_SQL.GET_OBJECTS_BY_OWNER";
	
	private Object lock = new Object();
	
	@PersistenceContext
    EntityManager entityManager;
	
	@Autowired
    JdbcTemplate jdbcTemplate;
	
	@Autowired
	private MyProperties properties;
	
	public List callProcForData(String xml, String procName, String pojoClassName) throws Exception {
		SimpleJdbcCall procCall;
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		
		boolean debug = Boolean.parseBoolean(properties.getDebug());
		
		procCall = new SimpleJdbcCall(jdbcTemplate)
    	.withFunctionName(procName)
    	.withoutProcedureColumnMetaDataAccess();
		
		procCall.declareParameters(new SqlOutParameter("ref_cursor", OracleTypes.CURSOR));
		
		procCall.declareParameters(new SqlParameter("I_xml", java.sql.Types.VARCHAR));
		mapSqlParameterSource.addValue("I_xml", xml);
		
		if(debug){
			log.info("Call: "+procCall.getProcedureName()+"("+StringUtils.join(mapSqlParameterSource.getValues().keySet(), ",")+")\n"+
					 "I_xml: "+XmlFormatter.format(xml)
					 );
		}
		
		ArrayList<Map> resultArray = procCall.executeFunction(ArrayList.class,mapSqlParameterSource);
		
		return generateOutput(resultArray, pojoClassName);
	}
	

	
	public List getObjectsByName(String xml) throws Exception {
		return callProcForData(xml, GET_OBJECTS_BY_NAME, "ws.refcursor.Objects$Generated");
	}
	
	public List getObjectsByOwner(String xml) throws Exception {
		return callProcForData(xml, GET_OBJECTS_BY_OWNER, "ws.refcursor.Objects$Generated");
	}
	
	
	private List generateOutput(ArrayList<Map> resultArray, String pojoClassName) throws Exception {
		int resultSizeLimit = properties.getResponseLimit();
		int max = 0;
		if(resultArray==null)
			return new ArrayList(0);
		
		/*
		if(resultArray.size()>=resultSizeLimit) {
			max = resultSizeLimit;
			log.warn("Function result limited from "+resultArray.size()+" to "+resultSizeLimit);
		} else{
			max = resultArray.size();
		}*/
		
		max = resultArray.size();
		
		List<String> header = new ArrayList<String>(0);
		List<List<String>> data = new ArrayList<List<String>>(0);
		
		for (int i = 0; i < max; i++) {
			List<String> row = new ArrayList<String>(0);
			
			if(i==0){
				for(Object key: resultArray.get(i).keySet()){
					header.add(key.toString());
				}
			}
			
			for(String key: header){
				Object o = resultArray.get(i).get(key);
				if(o!=null)
					row.add(resultArray.get(i).get(key).toString());
				else
					row.add("");
			}
			data.add(row);
		}
		
		List responseList = new ArrayList(0);
		if(!header.isEmpty() && !data.isEmpty()){
			Map<String, Class<?>> props = new HashMap<String, Class<?>>();
			for (String property: header) {
				props.put(toProperty(property), String.class);
			}
			
			Class<?> clazz;
			
			synchronized(lock) {
				clazz = PojoGenerator.generate(pojoClassName, props);
			}
			
			for(List<String> row: data){
				Object obj = clazz.newInstance();
				
				for (int index = 0; index < header.size(); index++) {
					String property = header.get(index);
					String value = row.get(index);
					clazz.getMethod(toSetter(property), String.class).invoke(obj, value);
				}
				responseList.add(obj);
			}
			
		}
		
		return responseList;
	}
	
	
	private String toProperty(String property){
		String ret = property;
		if(ret!=null && ret.length()>0){
			ret = ret.replaceAll("[^a-zA-Z0-9]", "");
			if(ret!=null && ret.length()>0){
				ret = StringUtils.stripStart(ret, "1234567890");
				if(ret!=null && ret.length()>0){
					ret = StringUtils.uncapitalize(ret);
				}
			}
		} 
		
		return ret;
	}
	
	private String toSetter(String property){
		String ret = toProperty(property);
		if(ret!=null && ret.length()>0){
			ret = "set"+StringUtils.capitalize(ret);
		}
		return ret;
	}
	
}

