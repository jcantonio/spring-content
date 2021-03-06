package org.springframework.content.commons.repository.events;

import java.io.Serializable;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.AfterStoreEvent;
import org.springframework.content.commons.repository.ContentStore;

public class AfterGetContentEvent extends AfterStoreEvent {

	private static final long serialVersionUID = -3209578443616933734L;

	public AfterGetContentEvent(Object source, ContentStore<Object, Serializable> store) {
		super(source, store);
	}

    public AfterGetContentEvent(Object source, PropertyPath propertyPath, ContentStore<Object, Serializable> store) {
        super(source, propertyPath, store);
    }
}
