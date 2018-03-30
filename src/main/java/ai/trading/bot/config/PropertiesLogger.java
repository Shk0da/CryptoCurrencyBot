package ai.trading.bot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.PropertySource;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Configuration
public class PropertiesLogger {

    @Autowired
    private AbstractEnvironment environment;

    @PostConstruct
    public void printProperties() {
        log.info("**** APPLICATION PROPERTIES SOURCES ****");

        Set<String> properties = new TreeSet<>();
        for (OriginTrackedMapPropertySource p : findPropertiesPropertySources()) {
            log.info(p.toString());
            properties.addAll(Arrays.asList(p.getPropertyNames()));
        }

        log.info("**** APPLICATION PROPERTIES VALUES ****");
        print(properties);
    }

    private List<OriginTrackedMapPropertySource> findPropertiesPropertySources() {
        List<OriginTrackedMapPropertySource> propertiesPropertySources = new LinkedList<>();
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof OriginTrackedMapPropertySource) {
                propertiesPropertySources.add((OriginTrackedMapPropertySource) propertySource);
            }
        }

        return propertiesPropertySources;
    }

    private void print(Set<String> properties) {
        for (String propertyName : properties) {
            log.info("{}={}", propertyName, environment.getProperty(propertyName));
        }
    }
}
