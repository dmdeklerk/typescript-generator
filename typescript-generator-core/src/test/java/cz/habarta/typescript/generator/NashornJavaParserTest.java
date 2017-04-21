package cz.habarta.typescript.generator;

import java.io.StringWriter;
import java.util.List;

import org.junit.Test;

import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.emitter.Emitter;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.NashornJavaParser;

public class NashornJavaParserTest {
    
    public static class MyDummyBean {
        
        private String privateStringProperty;
        private boolean privateBooleanProperty;
        
        /* Property getter - string */
        public String getStringProperty() {
            return privateStringProperty;
        }
        
        /* Property getter - boolean */
        public boolean isBoolenProperty() {
            return privateBooleanProperty;
        }
        
        public String someMethod(String arg1, int arg2, List<String> arg3) {
            return "";
        }        
    }
    
    @Test
    public void test() {
        final StringWriter stringWriter = new StringWriter();
        Settings settings = new Settings();        
        
        settings.mapPackagesToNamespaces = true;
        
        DefaultTypeProcessor typeProcessor = new DefaultTypeProcessor();
        NashornJavaParser parser = new NashornJavaParser(settings, typeProcessor);
        ModelCompiler modelCompiler = new ModelCompiler(settings, typeProcessor);
        Emitter emitter = new Emitter(settings);
        final Class<?> bean = MyDummyBean.class;
        final Model model = parser.parseModel(bean);
        final TsModel tsModel = modelCompiler.javaToTypeScript(model);
        emitter.emit(tsModel, stringWriter, "dummy", true, false, 0);

        System.out.println("model="+model);
        System.out.println("tsModel="+tsModel);
        System.out.println(stringWriter.toString());
    }    
 
    
}
