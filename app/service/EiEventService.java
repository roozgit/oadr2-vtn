package service;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.inject.Inject;

import models.CustomerForm;
import models.VENStatus;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiResponse;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.w3c.dom.Document;

import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

public class EiEventService{

    static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
    static EntityManager entityManager = entityManagerFactory.createEntityManager();

    public EiEventService(){
    }
    
    public static Result sendMarshalledObject(Object o) throws JAXBException{
        if(o instanceof OadrRequestEvent){
            return sendDistributeFromRequest(o);
        }
        //if its a created event send out a distribute event
        else if(o instanceof OadrCreatedEvent){
            return sendResponseFromCreated(o);
        }
        else{
            return play.mvc.Action.badRequest("Object was not of correct class");
        }
    }    
    
    @Transactional
    public static Result sendResponseFromCreated(Object o) throws JAXBException{
        
        OadrCreatedEvent oCreatedEvent = (OadrCreatedEvent) o;
        oCreatedEvent.getEiCreatedEvent().getVenID();

        onCreatedEvent(oCreatedEvent);
        createNewEm();
        entityManager.persist(oCreatedEvent);
        entityManager.getTransaction().commit();
        
        EiResponse eiResponse = new EiResponse()        
            //.withRequestID(oCreatedEvent.getEiCreatedEvent().getEiResponse().getRequestID())
            //TODO Need to handle non 200 responses
            .withResponseCode("200")
            .withResponseDescription("Optional description!"); 
        
        return play.mvc.Action.ok(marshalObject(eiResponse));
    }
    
    @Transactional
    public static Result sendDistributeFromRequest(Object o) throws JAXBException{
        OadrRequestEvent oRequestEvent = (OadrRequestEvent) o;
        EiResponse eiResponse = new EiResponse(); 
        if(oRequestEvent.getEiRequestEvent().getRequestID() != null){
            eiResponse.setRequestID(oRequestEvent.getEiRequestEvent().getRequestID());
        }
        //TODO Need to handle non 200 responses
        eiResponse.setResponseCode("200");
        
        createNewEm();
        entityManager.persist(oRequestEvent);  
        entityManager.getTransaction().commit();
        
        onRequestEvent(oRequestEvent);    
        OadrDistributeEvent response = new OadrDistributeEvent().withEiResponse(eiResponse);
        //Need to find out how to get EiEvent(s) and add them to this Distribute Event
        //Possibly from the Market Context, but need XPath to access, or find through VEN and Customers
        
        //response.withOadrEvent(null);
        response.withRequestID(eiResponse.getRequestID());

        return play.mvc.Action.ok(marshalObject(response));
    }
    
    public static String marshalObject(Object o) throws JAXBException{  
        JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");    
        Marshaller marshaller = jaxbContext.createMarshaller();      
        StringWriter sw = new StringWriter();
        marshaller.marshal(o, sw);
        play.mvc.Controller.response().setContentType("application/xml");
        //might not need the \n char
        return sw.toString() + '\n';
    }
        
    @SuppressWarnings("unchecked")
    @Transactional
    public static void onRequestEvent(OadrRequestEvent requestEvent){
        VENStatus venStatus = new VENStatus();
        venStatus.setTime(new Date());
        venStatus.setVenID(requestEvent.getEiRequestEvent().getVenID());
        Logger.info(requestEvent.getEiRequestEvent().getVenID());
        
        CustomerForm customer = null;
        EiEvent event = null;
        createNewEm();
        customer = (CustomerForm)entityManager.createQuery("SELECT c FROM Customers c WHERE c.venID = :ven")
                .setParameter("ven", requestEvent.getEiRequestEvent().getVenID())
                .getSingleResult();       
        
        List<VENStatus> statuses = entityManager.createQuery("SELECT v FROM StatusObject v WHERE v.venID = :ven")
            .setParameter("ven", customer.getVenID())
            .getResultList();
        
        if(statuses.size() == 0){    
            Logger.info("Size == 0");
            venStatus.setProgram(customer.getProgramId());
            
            event = (EiEvent)entityManager.createQuery("SELECT event FROM EiEvent event, EiEvent$EventDescriptor$EiMarketContext " +
                    "marketContext WHERE marketContext.marketContext = :market and event.hjid = marketContext.hjid")
                    .setParameter("market", venStatus.getProgram())
                    .getSingleResult();
                    
            if(customer != null && event != null){  
                Logger.info("Persisting i hope!");
                venStatus.setEventID(event.getEventDescriptor().getEventID());
                createNewEm();
                entityManager.persist(venStatus);
                entityManager.getTransaction().commit();
            }
        }
        Logger.info("End request event");
    }

    @Transactional
    public static void onCreatedEvent(OadrCreatedEvent createdEvent){
        VENStatus status = null;
        status = (VENStatus)Persistence.createEntityManagerFactory("Events").createEntityManager().createQuery("SELECT status FROM StatusObject " +
                "status WHERE status.venID = :ven")
                .setParameter("ven", createdEvent.getEiCreatedEvent().getVenID())
                .getSingleResult();
        if(status != null){
            status.setOptStatus(createdEvent.getEiCreatedEvent().getEventResponses().getEventResponse().get(0).getOptType().toString());
            status.setTime(new Date());
            createNewEm();
            entityManager.merge(status);    
            entityManager.getTransaction().commit();
        }
    }
    
    public static Object unmarshalRequest(byte[] chars ) throws JAXBException{    
        JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Object o = unmarshaller.unmarshal(new ByteArrayInputStream(chars));
        return o;
    }
    
    public static void createNewEm(){
        entityManager = entityManagerFactory.createEntityManager();
        if(!entityManager.getTransaction().isActive()){
            entityManager.getTransaction().begin();
        }
    }
    
}