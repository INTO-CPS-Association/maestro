package org.intocps.maestro.webapi;

import com.fasterxml.classmate.TypeResolver;
import org.intocps.maestro.webapi.controllers.Maestro2SimulationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Autowired
    private TypeResolver typeResolver;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.basePackage(SwaggerConfig.class.getPackage().getName()))
                .paths(PathSelectors.any()).build()
                .additionalModels(typeResolver.resolve(Maestro2SimulationController.InitializationData.ZeroCrossingConstraint.class));
    }


}