package internal.org.springframework.content.rest.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.utils.ContentStoreUtils;

@ContentRestController
public class ContentEntityRestController extends AbstractContentPropertyController {

	private static final String BASE_MAPPING = "/{repository}/{id}";

	private ContentStoreService storeService;
	
	@Autowired 
	public ContentEntityRestController(ContentStoreService storeService) {
		super();
		this.storeService = storeService;
	}

	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.GET, headers="accept!=application/hal+json")
	public ResponseEntity<InputStreamResource> getContent(final RootResourceInformation rootInfo,
														  @PathVariable String repository, 
														  @PathVariable String id, 
														  @RequestHeader("Accept") String mimeType) 
			throws HttpRequestMethodNotSupportedException {
		
		Object domainObj = getDomainObject(rootInfo.getInvoker(), id);

		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, domainObj.getClass());
		if (info == null) {
			throw new IllegalArgumentException("Entity not a content repository");
		}

		InputStream content = info.getImpementation().getContent(domainObj);
		if (content == null) {
			throw new ResourceNotFoundException();
		}
		
		Object contentTypeObj = BeanUtils.getFieldWithAnnotation(domainObj, MimeType.class);
		String contentType = contentTypeObj != null ? contentTypeObj.toString() : null;
		if (mimeType == null || mimeType.contains("*/*") || mimeType.equals(contentType)) {
			final HttpHeaders headers = new HttpHeaders();
			if (BeanUtils.hasFieldWithAnnotation(domainObj, MimeType.class)) {
				headers.add("Content-Type", BeanUtils.getFieldWithAnnotation(domainObj, MimeType.class).toString());
			}
			if (BeanUtils.hasFieldWithAnnotation(domainObj, ContentLength.class))
				headers.add("Content-Length", BeanUtils.getFieldWithAnnotation(domainObj, ContentLength.class).toString());
			
			InputStreamResource inputStreamResource = new InputStreamResource(info.getImpementation().getContent(domainObj));
			return new ResponseEntity<InputStreamResource>(inputStreamResource, headers, HttpStatus.OK);
		} else {
			final HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", mimeType);
//			if (BeanUtils.hasFieldWithAnnotation(contentPropertyValue, ContentLength.class))
//				headers.add("Content-Length", BeanUtils.getFieldWithAnnotation(contentPropertyValue, ContentLength.class).toString());
			
			ContentStore<Object,Serializable> impl = info.getImpementation();
			
			if (impl instanceof Renderable) {
				InputStream is = ((Renderable)impl).getRendition(domainObj, mimeType);
				if (is != null) {
					InputStreamResource inputStreamResource = new InputStreamResource(is);
					return new ResponseEntity<InputStreamResource>(inputStreamResource, headers, HttpStatus.OK);
				} else {
					return new ResponseEntity<InputStreamResource>(null, headers, HttpStatus.NOT_ACCEPTABLE);
				}
			}
		}
		return null;
	}
	
	/**
	 * Handles all PUTed requests 
	 * 
	 * This method is also called by modern browsers and IE >= 10
	 * @throws IOException 
	 * @throws HttpRequestMethodNotSupportedException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@RequestMapping(value = BASE_MAPPING, method = RequestMethod.PUT, headers="accept!=application/hal+json")
	@ResponseBody
	public void setContent(final HttpServletRequest request,
									final RootResourceInformation rootInfo,
			        				@PathVariable String repository, 
									@PathVariable String id) 
			throws IOException, HttpRequestMethodNotSupportedException, InstantiationException, IllegalAccessException {

		RepositoryInvoker invoker = rootInfo.getInvoker();
		Object domainObj = getDomainObject(invoker, id);

		ContentStoreInfo info = ContentStoreUtils.findContentStore(storeService, domainObj.getClass());
		if (info == null) {
			throw new IllegalArgumentException("Entity not a content repository");
		}

		info.getImpementation().setContent(domainObj, request.getInputStream());
		
		if (BeanUtils.hasFieldWithAnnotation(domainObj, MimeType.class)) {
			BeanUtils.setFieldWithAnnotation(domainObj, MimeType.class, request.getHeader("Content-Type"));
		}
		
		invoker.invokeSave(domainObj);
	}
}
