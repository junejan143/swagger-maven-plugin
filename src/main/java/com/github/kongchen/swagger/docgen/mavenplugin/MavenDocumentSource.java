package com.github.kongchen.swagger.docgen.mavenplugin;

import com.github.kongchen.swagger.docgen.AbstractDocumentSource;
import com.github.kongchen.swagger.docgen.GenerateException;
import com.github.kongchen.swagger.docgen.reader.AbstractReader;
import com.github.kongchen.swagger.docgen.reader.JaxrsReader;
import io.swagger.annotations.Api;
import io.swagger.core.filter.SpecFilter;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * @author chekong
 *         05/13/2013
 */
public class MavenDocumentSource extends AbstractDocumentSource {
    private final SpecFilter specFilter = new SpecFilter();

    public MavenDocumentSource(ApiSource apiSource, Log log) throws MojoFailureException {
        super(log, apiSource);
    }

    @Override
    public void doLoadDocuments() throws GenerateException {
        swagger = resolveApiReader().read(apiSource.getValidClasses(Api.class));

    }

    protected AbstractReader getDefaultReader()
    {
        return  new JaxrsReader(swagger, LOG);
    }

}
