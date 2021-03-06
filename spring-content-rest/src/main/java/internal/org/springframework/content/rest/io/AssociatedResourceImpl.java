package internal.org.springframework.content.rest.io;

import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.resource.HttpResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Represents a Spring Content Resource that is associated with a Spring Data Entity.
 *
 * Overrides the ContentLength and LastModifiedDate with the values stored on the
 * Spring Data entity, rather than the values of the actual content itself (these are
 * used as fallback values though).
 *
 * Also sets appropriate headers to pass the Spring Data Entity recorded filename, if exists.
 */
public class AssociatedResourceImpl<S> implements HttpResource, AssociatedResource<S> {

    private final ConfigurableConversionService conversionService = new DefaultConversionService();

    {
        Jsr310Converters.getConvertersToRegister().forEach(conversionService::addConverter);
    }

    private S entity;
    private Resource original;

    public AssociatedResourceImpl(S entity, Resource original) {
        this.entity = entity;
        this.original = original;
    }

    public S getAssociation() {
        return entity;
    }

    @Override
    public long contentLength() throws IOException {
        Long contentLength = (Long) BeanUtils.getFieldWithAnnotation(entity, ContentLength.class);
        if (contentLength == null) {
            contentLength = original.contentLength();
        }
        return contentLength;
    }

    @Override
    public long lastModified() throws IOException {
        Object lastModified = BeanUtils.getFieldWithAnnotation(entity, LastModifiedDate.class);
        if (lastModified == null) {
            return original.lastModified();
        }

        return Stream.of(lastModified)
        .map(it -> conversionService.convert(it, Date.class))//
        .map(it -> conversionService.convert(it, Instant.class))//
        .map(it -> it.toEpochMilli())
        .findFirst().orElseThrow(() -> new IllegalArgumentException(format("Invalid data type for @LastModifiedDate for Entity %s", entity)));
    }

    @Override
    public boolean exists() {
        return (original != null ? original.exists() : false);
    }

    @Override
    public boolean isReadable() {
        return original.isReadable();
    }

    @Override
    public boolean isOpen() {
        return original.isOpen();
    }

    @Override
    public boolean isFile() {
        return original.isFile();
    }

    @Override
    public URL getURL() throws IOException {
        return original.getURL();
    }

    @Override
    public URI getURI() throws IOException {
        return original.getURI();
    }

    @Override
    public File getFile() throws IOException {
        return original.getFile();
    }

    @Override
    public ReadableByteChannel readableChannel() throws IOException {
        return original.readableChannel();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        return original.createRelative(relativePath);
    }

    @Override
    @Nullable
    public String getFilename() {
        return original.getFilename();
    }

    @Override
    public String getDescription() {
        return original.getDescription();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return original.getInputStream();
    }

    @Override
    public HttpHeaders getResponseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        // Modified to show download
        Object originalFileName = BeanUtils.getFieldWithAnnotation(entity, OriginalFileName.class);
        if (originalFileName != null && StringUtils.hasText(originalFileName.toString())) {
            ContentDisposition.Builder builder = ContentDisposition.builder("form-data").name( "attachment").filename((String)originalFileName, Charset.defaultCharset());
            headers.setContentDisposition(builder.build());
        }
        return headers;
    }
}
