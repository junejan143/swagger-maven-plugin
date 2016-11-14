package com.github.kongchen.swagger.docgen.reader;

import com.github.kongchen.swagger.docgen.GenerateException;
import com.github.kongchen.swagger.docgen.spring.SpringResource;
import com.github.kongchen.swagger.docgen.util.SpringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import io.swagger.models.Operation;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.Parameter;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class SpringMvcExtendReader extends AbstractReader implements ClassSwaggerReader {
    private String resourcePath;

    public SpringMvcExtendReader(Swagger swagger, Log log) {
        super(swagger, log);
    }

    @Override
    public Swagger read(Set<Class<?>> classes) throws GenerateException {
        Map<String, SpringResource> resourceMap = generateResourceMap(classes);
        for (String str : resourceMap.keySet()) {
            SpringResource resource = resourceMap.get(str);
            read(resource);
        }

        return swagger;
    }

    public Swagger read(SpringResource resource) {
        if (swagger == null) {
            swagger = new Swagger();
        }
        List<Method> methods = resource.getMethods();
        Map<String, Tag> tags = new HashMap<String, Tag>();

        //List<SecurityRequirement> resourceSecurities = new ArrayList<SecurityRequirement>();

        // Add the description from the controller api
        Class<?> controller = resource.getControllerClass();
        RequestMapping controllerRM = AnnotationUtils.findAnnotation(controller, RequestMapping.class);

        String[] controllerProduces = new String[0];
        String[] controllerConsumes = new String[0];
        if (controllerRM != null) {
            controllerConsumes = controllerRM.consumes();
            controllerProduces = controllerRM.produces();
        }

        if (controller.isAnnotationPresent(Api.class)) {
            Api api = AnnotationUtils.findAnnotation(controller, Api.class);
            if (!canReadApi(false, api)) {
                return swagger;
            }
            updateTagsForApi(null, api);
            //resourceSecurities = getSecurityRequirements(api);
        }else if (controller.isAnnotationPresent(Controller.class)){//存在@Controller注解
            Controller controllerAnnotation = AnnotationUtils.findAnnotation(controller, Controller.class);
            updateTagsForApi(null, controllerAnnotation);
        }else if (controller.isAnnotationPresent(RestController.class)){//存在@RestController注解
            RestController restControllerAnnotation = AnnotationUtils.findAnnotation(controller, RestController.class);
            updateTagsForApi(null, restControllerAnnotation);
        }
        //获得类的根路径---用在类上的@RequestMapping
        resourcePath = resource.getControllerMapping();
        //collect api from method with @RequestMapping
        Map<String, List<Method>> apiMethodMap = collectApisByRequestMapping(methods);

        for (String path : apiMethodMap.keySet()) {
            for (Method method : apiMethodMap.get(path)) {
                RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
                if (requestMapping == null) {
                    continue;
                }
                //
                //ApiOperation apiOperation = AnnotationUtils.findAnnotation(method, ApiOperation.class);
                //if (apiOperation == null || apiOperation.hidden()) {
                //    continue;
                //}

                Map<String, String> regexMap = new HashMap<String, String>();
                String operationPath = parseOperationPath(path, regexMap);

                //http method
                for (RequestMethod requestMethod : requestMapping.method()) {
                    String httpMethod = requestMethod.toString().toLowerCase();
                    Operation operation = parseMethod(method,requestMapping);

                    updateOperationParameters(new ArrayList<Parameter>(), regexMap, operation);
                    //if (){
                    //
                    //}
                    //updateOperationProtocols(apiOperation, operation);

                    String[] apiProduces = requestMapping.produces();
                    String[] apiConsumes = requestMapping.consumes();

                    apiProduces = (apiProduces.length == 0) ? controllerProduces : apiProduces;
                    apiConsumes = (apiConsumes.length == 0) ? controllerConsumes : apiConsumes;

                    updateOperationConsumes(new String[0], apiConsumes, operation);
                    updateOperationProduces(new String[0], apiProduces, operation);

                    updateTagsForOperation(operation, requestMapping);
                    //updateOperation(apiConsumes, apiProduces, tags, resourceSecurities, operation);
                    updatePath(operationPath, httpMethod, operation);
                }
            }
        }
        return swagger;
    }

    private Operation parseMethod(Method method, Annotation anno) {
        Operation operation = new Operation();
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        Type responseClass = null;
        List<String> consumes = new ArrayList<String>();
        List<String> produces = new ArrayList<String>();
        String operationId = method.getName();
        String responseContainer = null;

        if (anno instanceof ApiOperation){
            ApiOperation apiOperation = AnnotationUtils.findAnnotation(method, ApiOperation.class);
            if (apiOperation.hidden()) {
                return null;
            }
            if (!apiOperation.nickname().isEmpty()) {
                operationId = apiOperation.nickname();
            }
        }else if (anno instanceof RequestMapping){

        }
        //设置响应信息头
        //Map<String, Property> defaultResponseHeaders = parseResponseHeaders(apiOperation.responseHeaders());

        //设置每个方法的概要，默认为方法名,默认没有实现注意事项（Implementation Notes）
        //operation.summary(apiOperation.value()).description(apiOperation.notes());
        operation.summary(method.getName());

        /*自定义扩展  这个是@ApiOperation才有的属性
        Set<Map<String, Object>> customExtensions = parseCustomExtensions(apiOperation.extensions());
        for (Map<String, Object> extension : customExtensions) {
            if (extension == null) {
                continue;
            }
            for (Map.Entry<String, Object> map : extension.entrySet()) {
                operation.setVendorExtension(
                        map.getKey().startsWith("x-")
                                ? map.getKey()
                                : "x-" + map.getKey(), map.getValue()
                );
            }
        }*/

/*      响应状态码
        if (!apiOperation.response().equals(Void.class)) {
            responseClass = apiOperation.response();
        }
        if (!apiOperation.responseContainer().isEmpty()) {
            responseContainer = apiOperation.responseContainer();
        }*/
        //TODO:
        //if (method.isAnnotationPresent(ResponseStatus.class)){
        //    ResponseStatus responseStatus = AnnotationUtils.findAnnotation(method, ResponseStatus.class);
        //    operation.response(responseStatus.code().ordinal(), new Response()
        //            .description("successful operation")
        //            );
        //}

        if (responseClass == null) {
            // pick out response from method declaration
            LOG.info("picking up response class from method " + method);
            responseClass = method.getGenericReturnType();
        }
        if (responseClass instanceof ParameterizedType && ResponseEntity.class.equals(((ParameterizedType) responseClass).getRawType())) {
            responseClass = ((ParameterizedType) responseClass).getActualTypeArguments()[0];
        }
        boolean hasApiAnnotation = false;
        //if (responseClass instanceof Class) {
        //    hasApiAnnotation = AnnotationUtils.findAnnotation((Class) responseClass, Api.class) != null;
        //}
        /*if (responseClass != null
                && !responseClass.equals(Void.class)
                && !responseClass.equals(ResponseEntity.class)
                && !hasApiAnnotation) {
            if (isPrimitive(responseClass)) {
                Property responseProperty;
                Property property = ModelConverters.getInstance().readAsProperty(responseClass);
                if (property != null) {
                    if ("list".equalsIgnoreCase(responseContainer)) {
                        responseProperty = new ArrayProperty(property);
                    } else if ("map".equalsIgnoreCase(responseContainer)) {
                        responseProperty = new MapProperty(property);
                    } else {
                        responseProperty = property;
                    }
                    operation.response(apiOperation.code(), new Response()
                            .description("successful operation")
                            .schema(responseProperty)
                            .headers(defaultResponseHeaders));
                }
            } else if (!responseClass.equals(Void.class) && !responseClass.equals(void.class)) {
                Map<String, Model> models = ModelConverters.getInstance().read(responseClass);
                if (models.isEmpty()) {
                    Property pp = ModelConverters.getInstance().readAsProperty(responseClass);
                    operation.response(apiOperation.code(), new Response()
                            .description("successful operation")
                            .schema(pp)
                            .headers(defaultResponseHeaders));
                }
                for (String key : models.keySet()) {
                    Property responseProperty;

                    if ("list".equalsIgnoreCase(responseContainer)) {
                        responseProperty = new ArrayProperty(new RefProperty().asDefault(key));
                    } else if ("map".equalsIgnoreCase(responseContainer)) {
                        responseProperty = new MapProperty(new RefProperty().asDefault(key));
                    } else {
                        responseProperty = new RefProperty().asDefault(key);
                    }
                    operation.response(apiOperation.code(), new Response()
                            .description("successful operation")
                            .schema(responseProperty)
                            .headers(defaultResponseHeaders));
                    swagger.model(key, models.get(key));
                }
                models = ModelConverters.getInstance().readAll(responseClass);
                for (Map.Entry<String, Model> entry : models.entrySet()) {
                    swagger.model(entry.getKey(), entry.getValue());
                }
            }
        }*/

        operation.operationId(operationId);

        for (String str : requestMapping.produces()) {
            if (!produces.contains(str)) {
                produces.add(str);
            }
        }
        for (String str : requestMapping.consumes()) {
            if (!consumes.contains(str)) {
                consumes.add(str);
            }
        }

        ApiResponses responseAnnotation = AnnotationUtils.findAnnotation(method, ApiResponses.class);
        if (responseAnnotation != null) {
            updateApiResponse(operation, responseAnnotation);
        } else {
            ResponseStatus responseStatus = AnnotationUtils.findAnnotation(method, ResponseStatus.class);
            if (responseStatus != null) {
                operation.response(responseStatus.value().value(), new Response().description(responseStatus.reason()));
            }
        }

        Deprecated annotation = AnnotationUtils.findAnnotation(method, Deprecated.class);
        if (annotation != null) {
            operation.deprecated(true);
        }

        // process parameters
        Class[] parameterTypes = method.getParameterTypes();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        // paramTypes = method.getParameterTypes
        // genericParamTypes = method.getGenericParameterTypes
        for (int i = 0; i < parameterTypes.length; i++) {
            Type type = genericParameterTypes[i];
            List<Annotation> annotations = Arrays.asList(paramAnnotations[i]);
            List<Parameter> parameters = getParameters(type, annotations);
            for (Parameter parameter : parameters) {
                operation.parameter(parameter);
            }
        }

        if (operation.getResponses() == null) {
            operation.defaultResponse(new Response().description("successful operation"));
        }
        // Process @ApiImplicitParams
        //this.readImplicitParameters(method, operation);
        return operation;
    }

    private Map<String, List<Method>> collectApisByRequestMapping(List<Method> methods) {
        Map<String, List<Method>> apiMethodMap = new HashMap<String, List<Method>>();
        for (Method method : methods) {
            if (method.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
                String path;
                if (requestMapping.value().length != 0) {
                    path = generateFullPath(requestMapping.value()[0]);
                } else {
                    path = resourcePath;
                }
                if (apiMethodMap.containsKey(path)) {
                    apiMethodMap.get(path).add(method);
                } else {
                    List<Method> ms = new ArrayList<Method>();
                    ms.add(method);
                    apiMethodMap.put(path, ms);
                }
            }
        }

        return apiMethodMap;
    }

    private String generateFullPath(String path) {
        if (StringUtils.isNotEmpty(path)) {
            return this.resourcePath + (path.startsWith("/") ? path : '/' + path);
        } else {
            return this.resourcePath;
        }
    }

    @Deprecated // TODO: Delete method never used
    private Class<?> getGenericSubtype(Class<?> clazz, Type type) {
        if (!(clazz.getName().equals("void") || type.toString().equals("void"))) {
            try {
                ParameterizedType paramType = (ParameterizedType) type;
                Type[] argTypes = paramType.getActualTypeArguments();
                if (argTypes.length > 0) {
                    return (Class<?>) argTypes[0];
                }
            } catch (ClassCastException e) {
                //FIXME: find out why this happens to only certain types
            }
        }
        return clazz;
    }

    //Helper method for loadDocuments()
    private Map<String, SpringResource> analyzeController(Class<?> controllerClazz, Map<String, SpringResource> resourceMap, String description) {
	String[] controllerRequestMappingValues = SpringUtils.getControllerResquestMapping(controllerClazz);

        // Iterate over all value attributes of the class-level RequestMapping annotation
        for (String controllerRequestMappingValue : controllerRequestMappingValues) {
            for (Method method : controllerClazz.getMethods()) {
                RequestMapping methodRequestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);

                // Look for method-level @RequestMapping annotation
                if (methodRequestMapping != null) {
                    RequestMethod[] requestMappingRequestMethods = methodRequestMapping.method();

                    // For each method-level @RequestMapping annotation, iterate over HTTP Verb
                    for (RequestMethod requestMappingRequestMethod : requestMappingRequestMethods) {
                        String[] methodRequestMappingValues = methodRequestMapping.value();

                        // Check for cases where method-level @RequestMapping#value is not set, and use the controllers @RequestMapping
                        if (methodRequestMappingValues.length == 0) {
                            // The map key is a concat of the following:
                            //   1. The controller package
                            //   2. The controller class name
                            //   3. The controller-level @RequestMapping#value
                            String resourceKey = controllerClazz.getCanonicalName() + controllerRequestMappingValue + requestMappingRequestMethod;
                            if (!resourceMap.containsKey(resourceKey)) {
                                resourceMap.put(
                                        resourceKey,
                                        new SpringResource(controllerClazz, controllerRequestMappingValue, resourceKey, description));
                            }
                            resourceMap.get(resourceKey).addMethod(method);
                        } else {
                            // Here we know that method-level @RequestMapping#value is populated, so
                            // iterate over all the @RequestMapping#value attributes, and add them to the resource map.
                            for (String methodRequestMappingValue : methodRequestMappingValues) {
                                String resourceKey = controllerClazz.getCanonicalName() + controllerRequestMappingValue
                                        + methodRequestMappingValue + requestMappingRequestMethod;
                                if (!methodRequestMappingValue.isEmpty()) {
                                    if (!resourceMap.containsKey(resourceKey)) {
                                        resourceMap.put(resourceKey, new SpringResource(controllerClazz, methodRequestMappingValue, resourceKey, description));
                                    }
                                    resourceMap.get(resourceKey).addMethod(method);
                                }
                            }
                        }
                    }
                }
            }
        }
        controllerClazz.getFields();
        controllerClazz.getDeclaredFields(); //<--In case developer declares a field without an associated getter/setter.
        //this will allow NoClassDefFoundError to be caught before it triggers bamboo failure.

        return resourceMap;
    }

    protected Map<String, SpringResource> generateResourceMap(Set<Class<?>> validClasses) throws GenerateException {
        Map<String, SpringResource> resourceMap = new HashMap<String, SpringResource>();
        for (Class<?> aClass : validClasses) {
            RequestMapping requestMapping = AnnotationUtils.findAnnotation(aClass, RequestMapping.class);
            //This try/catch block is to stop a bamboo build from failing due to NoClassDefFoundError
            //This occurs when a class or method loaded by reflections contains a type that has no dependency
            try {
                resourceMap = analyzeController(aClass, resourceMap, "");
                List<Method> mList = new ArrayList<Method>(Arrays.asList(aClass.getMethods()));
                if (aClass.getSuperclass() != null) {
                    mList.addAll(Arrays.asList(aClass.getSuperclass().getMethods()));
                }
            } catch (NoClassDefFoundError e) {
                LOG.error(e.getMessage());
                LOG.info(aClass.getName());
                //exception occurs when a method type or annotation is not recognized by the plugin
            }
        }

        return resourceMap;
    }

}
