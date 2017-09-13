package ws.refcursor.util;

import java.util.ArrayList;
import java.util.List;

import ws.refcursor.dto.ErrorResponse;
import ws.refcursor.dto.ObjectNameRequest;
import ws.refcursor.dto.ObjectOwnerRequest;

public class RequestValidator {
	
	public List<ErrorResponse> checkObjectNameRequest(ObjectNameRequest req){
		List<ErrorResponse> ret = new ArrayList<ErrorResponse>(0);
		
		if(req==null){
			ret.add(new ErrorResponse(ErrorCodes.ERROR.NULL_OBJECT, ""));
		} else {
			//objectName
			ret.addAll(validObjectName(req.getObjectName()));
		}
	
		return ret;
	}
	
	public List<ErrorResponse> checkObjectOwnerRequest(ObjectOwnerRequest req){
		List<ErrorResponse> ret = new ArrayList<ErrorResponse>(0);
		
		if(req==null){
			ret.add(new ErrorResponse(ErrorCodes.ERROR.NULL_OBJECT, ""));
		} else {
			//objectOwner
			ret.addAll(validObjectOwner(req.getObjectOwner()));
		}
	
		return ret;
	}
	
	
	private List<ErrorResponse> validObjectName(String objectName){
		List<ErrorResponse> ret = new ArrayList<ErrorResponse>(0);
		if(objectName==null || (objectName!=null && objectName.length()==0)){
			ret.add(new ErrorResponse(ErrorCodes.ERROR.MANDATORY_FIELD_EMPTY, "objectName"));
		} else {
			if(objectName.length()>30)
				ret.add(new ErrorResponse(ErrorCodes.ERROR.INVALID_LENGTH, "Object Name length max 30 characters"));
		}
		
		return ret;
	}
	
	private List<ErrorResponse> validObjectOwner(String objectOwner) {
		List<ErrorResponse> ret = new ArrayList<ErrorResponse>(0);
		if(objectOwner==null || (objectOwner!=null && objectOwner.length()==0)){
			ret.add(new ErrorResponse(ErrorCodes.ERROR.MANDATORY_FIELD_EMPTY, "objectOwner"));
		} else {
			if(objectOwner.length()>30)
				ret.add(new ErrorResponse(ErrorCodes.ERROR.INVALID_LENGTH, "Object Owner length max 30 characters"));
		}
		
		return ret;
	}

			
		
}
