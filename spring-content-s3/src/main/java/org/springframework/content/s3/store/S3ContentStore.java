package org.springframework.content.s3.store;

import java.io.Serializable;

import org.springframework.content.commons.repository.ContentStore;

public interface S3ContentStore<E,CID extends Serializable> extends ContentStore<E,CID> {
}
