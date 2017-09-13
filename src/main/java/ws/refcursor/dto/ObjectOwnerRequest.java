package ws.refcursor.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectOwnerRequest {

	String objectOwner;

	public ObjectOwnerRequest() {
		super();
	}

	public ObjectOwnerRequest(String objectOwner) {
		super();
		this.objectOwner = objectOwner;
	}

	public String getObjectOwner() {
		return objectOwner;
	}

	public void setObjectOwner(String objectOwner) {
		this.objectOwner = objectOwner;
	}

	@Override
	public String toString() {
		return "ObjectOwnerRequest [objectOwner=" + objectOwner + "]";
	}

}
