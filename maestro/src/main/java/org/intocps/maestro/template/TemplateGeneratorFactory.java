package org.intocps.maestro.template;

public class TemplateGeneratorFactory {
    public static IMaBLTemplateGenerator getTemplateGenerator(Object configuration){
        if(configuration instanceof ScenarioConfiguration){
            return new ScenarioConfigurationToMaBL((ScenarioConfiguration)configuration);
        }
        else if(configuration instanceof MaBLTemplateConfiguration){
            return null;
        }
        return null;
    }
}
