package ws.refcursor.util;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.transform.stream.StreamResult;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.XMLContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.Marshaller;
import org.xml.sax.InputSource;

public class XMLConverter {

    private Marshaller marshaller;

	public Marshaller getMarshaller() {
		return marshaller;
	}

	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	public String marshall(Object object) throws IOException{
    	StringWriter writer = new StringWriter();
        try{        	
        	marshaller.marshal(object, new StreamResult(writer));
            return writer.getBuffer().toString();
        } finally {
            if(writer != null) {
            	writer.close();
            }
        }
    }
	
	
	public static String marshallCastor(Object obj){
		StringWriter writer = new StringWriter();
		try {
			//Load Mapping
            Mapping mapping = new Mapping();
            ClassPathResource cpr = new ClassPathResource("/ws/refcursor/util/mapping-castor.xml");
            mapping.loadMapping(new InputSource(cpr.getInputStream()), "CastorXmlMapping");
            XMLContext context = new XMLContext();
            context.addMapping(mapping);
            
            org.exolab.castor.xml.Marshaller marshaller = context.createMarshaller();
            marshaller.setSuppressXSIType(true);
            marshaller.setWriter(writer);
            marshaller.marshal(obj);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		return writer.getBuffer().toString();
	}
	
}
