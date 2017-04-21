package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeProcessor;

public class NashornJavaParser extends ModelParser {
    
    private final ObjectMapper objectMapper = new ObjectMapper();    

    public NashornJavaParser(Settings settings, TypeProcessor typeProcessor) {
        super(settings, typeProcessor);
        objectMapper.registerModules(ObjectMapper.findModules(settings.classLoader));
    }

    @Override
    protected BeanModel parseBean(SourceType<Class<?>> sourceClass) {
        final List<PropertyModel> properties = new ArrayList<>();
        final List<MethodModel> methodModels = new ArrayList<>();

        final BeanHelper beanHelper = getBeanHelper(sourceClass.type);
        if (beanHelper != null) {
            for (BeanPropertyWriter beanPropertyWriter : beanHelper.getProperties()) {
                
                System.out.println("prop="+beanPropertyWriter);                
                
                final Member propertyMember = beanPropertyWriter.getMember().getMember();
                
                Type propertyType = getGenericType(propertyMember);
                boolean optional = false;
                PropertyModel.PullProperties pullProperties = null;
                final Member originalMember = beanPropertyWriter.getMember().getMember();
                
                properties.add(processTypeAndCreateProperty(beanPropertyWriter.getName(), propertyType, optional, sourceClass.type, originalMember, pullProperties));
            }
        }

        /* Add all method despite them being simple getters or setters */
        final List<Method> methods = Arrays.asList(sourceClass.type.getMethods());
        Collections.sort(methods, new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        for (Method method : methods) {
            Type returnType = method.getGenericReturnType();            
            methodModels.add(processTypeAndParametersAndCreateMethod(method.getName(), returnType, false, sourceClass.type, method));
        }        
        
        /* Create properties for each `Object getValue()` and `void setValue(Object value)` */
        // TODO        
        
        final String discriminantProperty = null;
        final String discriminantLiteral = null;
        final List<Class<?>> taggedUnionClasses = null;
        
        final Type superclass = sourceClass.type.getGenericSuperclass() == Object.class ? null : sourceClass.type.getGenericSuperclass();
        if (superclass != null) {
            addBeanToQueue(new SourceType<>(superclass, sourceClass.type, "<superClass>"));
        }
        final List<Type> interfaces = Arrays.asList(sourceClass.type.getGenericInterfaces());
        for (Type aInterface : interfaces) {
            addBeanToQueue(new SourceType<>(aInterface, sourceClass.type, "<interface>"));
        }
        return new BeanModel(sourceClass.type, superclass, taggedUnionClasses, discriminantProperty, discriminantLiteral, interfaces, properties, methodModels, null);
    }
    
    private BeanHelper getBeanHelper(Class<?> beanClass) {
        if (beanClass == null) {
            return null;
        }
        try {
            final DefaultSerializerProvider.Impl serializerProvider1 = (DefaultSerializerProvider.Impl) objectMapper.getSerializerProvider();
            final DefaultSerializerProvider.Impl serializerProvider2 = serializerProvider1.createInstance(objectMapper.getSerializationConfig(), objectMapper.getSerializerFactory());
            final JavaType simpleType = objectMapper.constructType(beanClass);
            final JsonSerializer<?> jsonSerializer = BeanSerializerFactory.instance.createSerializer(serializerProvider2, simpleType);
            if (jsonSerializer == null) {
                return null;
            }
            if (jsonSerializer instanceof BeanSerializer) {
                return new BeanHelper((BeanSerializer) jsonSerializer);
            } else {
                final String jsonSerializerName = jsonSerializer.getClass().getName();
                if (settings.displaySerializerWarning) {
                    System.out.println(String.format("Warning: Unknown serializer '%s' for class '%s'", jsonSerializerName, beanClass));
                }
                return null;
            }
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        }
    }

    private static class BeanHelper extends BeanSerializer {
        private static final long serialVersionUID = 1;

        public BeanHelper(BeanSerializer src) {
            super(src);
        }

        public BeanPropertyWriter[] getProperties() {
            return _props;
        }
    }
    
    private static Type getGenericType(Member member) {
        if (member instanceof Method) {
            return ((Method) member).getGenericReturnType();
        }
        if (member instanceof Field) {
            return ((Field) member).getGenericType();
        }
        return null;
    }
    
    private static boolean isSupported(JsonTypeInfo jsonTypeInfo) {
        return jsonTypeInfo != null &&
                jsonTypeInfo.include() == JsonTypeInfo.As.PROPERTY &&
                (jsonTypeInfo.use() == JsonTypeInfo.Id.NAME || jsonTypeInfo.use() == JsonTypeInfo.Id.CLASS);
    }

    private String getDiscriminantPropertyName(JsonTypeInfo jsonTypeInfo) {
        return jsonTypeInfo.property().isEmpty()
                ? jsonTypeInfo.use().getDefaultPropertyName()
                : jsonTypeInfo.property();
    }

    private String getTypeName(final Class<?> cls) {
        return cls.getName().substring(cls.getName().lastIndexOf(".") + 1);
    }
    
}
