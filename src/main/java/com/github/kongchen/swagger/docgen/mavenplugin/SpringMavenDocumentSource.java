package com.github.kongchen.swagger.docgen.mavenplugin;

import com.github.kongchen.swagger.docgen.AbstractDocumentSource;
import com.github.kongchen.swagger.docgen.GenerateException;
import com.github.kongchen.swagger.docgen.reader.AbstractReader;
import com.github.kongchen.swagger.docgen.reader.SpringMvcApiReader;
import io.swagger.annotations.Api;
import io.swagger.core.filter.SpecFilter;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.util.Set;

/**
 * @author tedleman
 *         01/21/15
 * @author chekong
 * 05/13/2013
 */
public class SpringMavenDocumentSource extends AbstractDocumentSource {
    private final SpecFilter specFilter = new SpecFilter();

    public SpringMavenDocumentSource(ApiSource apiSource, Log log) throws MojoFailureException {
        super(log, apiSource);
    }

    @Override
    public void doLoadDocuments() throws GenerateException
    {
        Set<Class<?>> clazzs=apiSource.getValidClasses(Api.class);
        //排除@Controller注解
		/*Iterator<Class<?>> it=clazzs.iterator();
		Class<?> clazz;
		while(it.hasNext())
		{
			clazz=it.next();
			if(clazz.getAnnotation(Controller.class)==null)
				it.remove();
		}*/
        swagger = resolveApiReader().read(clazzs);
    }

    protected AbstractReader getDefaultReader()
    {
        return  new SpringMvcApiReader(swagger, LOG);
    }

}
