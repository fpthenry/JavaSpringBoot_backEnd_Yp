package com.mycompany.myapp;

import com.mycompany.myapp.config.AsyncSyncConfiguration;
import com.mycompany.myapp.config.DatabaseTestcontainer;
import com.mycompany.myapp.config.ElasticsearchTestConfiguration;
import com.mycompany.myapp.config.ElasticsearchTestContainer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        JavaSpringBootBackEndApp.class,
        AsyncSyncConfiguration.class,
        com.mycompany.myapp.config.JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
        ElasticsearchTestContainer.class,
        ElasticsearchTestConfiguration.class,
    }
)
public @interface IntegrationTest {}
