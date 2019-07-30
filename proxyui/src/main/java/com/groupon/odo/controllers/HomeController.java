/*
 Copyright 2014 Groupon, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
package com.groupon.odo.controllers;

import com.groupon.odo.containers.HttpProxyContainer;
import com.groupon.odo.proxylib.Constants;
import com.groupon.odo.proxylib.HistoryService;
import com.groupon.odo.proxylib.SQLService;
import com.groupon.odo.proxylib.Utils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.annotation.MultipartConfig;
import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.filter.HiddenHttpMethodFilter;

/**
 * Handles requests for the application home page.
 */
@Controller
@ComponentScan(basePackages = {"com.groupon.odo.controllers"}, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = HttpProxyContainer.class)
})
@EnableAutoConfiguration(exclude = {EmbeddedServletContainerAutoConfiguration.class})
@PropertySources(value = {@PropertySource("classpath:application.properties")})
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
    public File baseDirectory;

    private RelaxedPropertyResolver propertyResolver;

    public void setEnvironment(Environment environment) {
        this.propertyResolver = new RelaxedPropertyResolver(environment, "tomcat.");
    }

    @PostConstruct
    public void init() {
        // update SQL schema
        try {
            SQLService.getInstance().updateSchema("/migrations");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void destroy() {
        logger.info("Running destroy");
        try {
            SQLService.getInstance().stopServer();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Simply selects the home view to render by returning its name.
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home(Locale locale, Model model) {
        return "redirect:profiles";
    }

    @Bean
    public EmbeddedServletContainerFactory servletContainer() throws Exception {
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();

        int apiPort = Utils.getSystemPort(Constants.SYS_API_PORT);
        factory.setPort(apiPort);
        factory.setSessionTimeout(10, TimeUnit.MINUTES);
        factory.setContextPath("/testproxy");
        baseDirectory = new File("./tmp");
        factory.setBaseDirectory(baseDirectory);
        List<TomcatConnectorCustomizer> cs = new ArrayList();
        cs.add(tomcatConnectorCustomizers());
        factory.setTomcatConnectorCustomizers(cs);

        if (Utils.getEnvironmentOptionValue(Constants.SYS_LOGGING_DISABLED) != null) {
            HistoryService.getInstance().disableHistory();
        }
        return factory;
    }

    @Bean
    public TomcatConnectorCustomizer tomcatConnectorCustomizers() {
        return connector -> {
            connector.setMaxPostSize(-1);
        };
    }

    @Bean
    public Filter hiddenHttpMethodFilter() {
        HiddenHttpMethodFilter filter = new HiddenHttpMethodFilter();
        return filter;
    }

    public static void main(String[] args) {
        SpringApplication.run(HomeController.class, args);
        SpringApplication.run(HttpProxyContainer.class, args);
    }
}
