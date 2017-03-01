package eu.h2020.symbiote;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.h2020.symbiote.cloud.model.CloudResource;
import eu.h2020.symbiote.db.ResourceRepository;
import eu.h2020.symbiote.messaging.interworkinginterface.IIResourceMessageHandler;
import eu.h2020.symbiote.messaging.rap.RAPResourceMessageHandler;

/**! \class PlatformInformationManager
 * \brief PlatformInformationManager handles the registration of the resources within the platform
 **/

/**
 * This class handles the initialization from the platform. Initially created by jose
 *
 * @author: jose, Elena Garrido
 * @version: 06/10/2016

 */
@Component
public class PlatformInformationManager {

  private static final Log logger = LogFactory.getLog(PlatformInformationManager.class);


  @Autowired
  private IIResourceMessageHandler ifresourceRegistrationMessageHandler;

  @Autowired
  private RAPResourceMessageHandler rapresourceRegistrationMessageHandler;

  @Autowired
  private ResourceRepository resourceRepository;

  @PostConstruct
  private void init() {
  }

  private List<CloudResource>  addOrUpdateInInternalRepository(List<CloudResource>  resources){
	 return resources.stream().map(resource -> {
		  CloudResource existingResource = resourceRepository.getByInternalId(resource.getInternalId());
	      if (existingResource != null) {
	    	  logger.info("update will be done");
	      }
	      return resourceRepository.save(resource);
	 })
     .collect(Collectors.toList());
  }

  private CloudResource deleteInInternalRepository(String resourceId){
    if (!"".equals(resourceId)) {
    	CloudResource existingResource = resourceRepository.getByInternalId(resourceId);
        if (existingResource != null) {
          resourceRepository.delete(resourceId);
          return existingResource;
        }
    }
    return null;
  }

//! Create a resource.
/*!
 * The addResource method stores \a ResourceBean passed as parameter in the  
 * mondodb database and send the information to the \a Interworking Interface and \a Resource Access Proxy component.
 *
 * \param resource \a ResourceBean to be created within the system
 * \return \a addResource returns the \a ResourceBean where the Symbiote id is included. 
 * An exception can be thrown when no \a internalId is indicated within the \a ResourceBean 
 */
  public List<CloudResource> addResources(List<CloudResource> resource) {
	List<CloudResource> listWithStmbioteId = ifresourceRegistrationMessageHandler.sendResourcesRegistrationMessage(resource);
	List<CloudResource> result  = addOrUpdateInInternalRepository(listWithStmbioteId);
    rapresourceRegistrationMessageHandler.sendResourcesRegistrationMessage(result);
    return result;
  }

//! Update a resource.
/*!
 * The updateResource method updates the \a ResourceBean passed as parameter into the   
 * mondodb database and sends the information to the \a Interworking Interface and \a Resource Access Proxy component.
 *
 * \param resource \a ResourceBean to be updated within the system
 * \return \a updateResource returns the \a ResourceBean where the Symbiote id is included. 
 */
  public List<CloudResource>  updateResource(List<CloudResource>  resources) {
	List<CloudResource> listWithStmbioteId = ifresourceRegistrationMessageHandler.sendResourceUpdateMessage(resources);
	List<CloudResource> result  = addOrUpdateInInternalRepository(listWithStmbioteId);
    rapresourceRegistrationMessageHandler.sendResourcesUpdateMessage(result);
    return result;
  }

//! Delete a resource.
/*!
 * The deleteResource method removes the \a ResourceBean identified by the id passed as a parameter in the \a internalId variable.   
 * It removes it from the mondodb database and request the removal of the information to the \a Interworking Interface and the \a Resource Access Proxy component.
 *
 * \param resourceId \a internalId to the resource to be removed 
 * \return \a deleteResource returns the \a ResourceBean that has been just removed 
 */
  public CloudResource deleteResource(String resourceId) {
	CloudResource result = null;  
    String id = ifresourceRegistrationMessageHandler.sendResourceUnregistrationMessage(resourceId);
    if (id!=null)
        result  = deleteInInternalRepository(resourceId);
    rapresourceRegistrationMessageHandler.sendResourceUnregistrationMessage(id);
    
    return result;
  }


  public List<CloudResource> getResources() {
    return resourceRepository.findAll();
  }

//! Get a resource.
/*!
 * The getResource method retrieves \a ResourceBean identified by \a resourceId 
 * from the mondodb database and will return it.
 *
 * \param resourceId id from the resource to be retrieved from the database
 * \return \a getResource returns the \a ResourceBean, 
 */
  public CloudResource getResource(String resourceId) {
	if (!"".equals(resourceId)) {
	     return resourceRepository.getByInternalId(resourceId);
	}
	return null;
  }

/*
  public List<ResourceBean> registerResources(List<String> resourceIds) {

    PlatformBean platformInfo = getPlatformInfo();
    List<ResourceBean> resources = new ArrayList<>();

    if (platformInfo != null) {
      if (platformInfo.getSymbioteId() != null) {

        for (String id : resourceIds) {
          ResourceBean resource = resourceRepository.findOne(id);
          if (resource != null) {
            resources.add(resource);
          }
        }

        resources = coreClient.registerResource(platformInfo.getSymbioteId(), resources);

        return addResources(resources);
      }
    }

    return resources;
  }
*/
}
