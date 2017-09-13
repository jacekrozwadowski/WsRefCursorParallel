package ws.refcursor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectNameRequest {

	String objectName;

	public ObjectNameRequest() {
		super();
	}

	public ObjectNameRequest(String objectName) {
		super();
		this.objectName = objectName;
	}

	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	@Override
	public String toString() {
		return "ObjectNameRequest [objectName=" + objectName + "]";
	}
	
}
