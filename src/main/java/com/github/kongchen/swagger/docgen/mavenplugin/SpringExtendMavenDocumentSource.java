package com.github.kongchen.swagger.docgen.mavenplugin;

import com.github.kongchen.swagger.docgen.AbstractDocumentSource;
import com.github.kongchen.swagger.docgen.GenerateException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * @author tedleman
 *         01/21/15
 * @author chekong
 * 05/13/2013
 */
public class SpringExtendMavenDocumentSource extends AbstractDocumentSource {
    //private final SpecFilter specFilter = new SpecFilter();

    public SpringExtendMavenDocumentSource(ApiSource apiSource, Log log) throws MojoFailureException {
        super(log, apiSource);
    }

    @Override
    public void doLoadDocuments() throws GenerateException {
        Set<Class<?>> clazzs=apiSource.getValidClasses(Controller.class);
        clazzs.addAll(apiSource.getValidClasses(RestController.class));

        swagger = resolveApiReader().read(clazzs);
    }

    @Override
    public void loadDocuments() throws GenerateException {
        swagger = resolveApiReader().read(apiSource.getValidClasses(Controller.class));
    }


}
