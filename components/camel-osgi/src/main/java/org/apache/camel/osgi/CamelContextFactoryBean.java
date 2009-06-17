/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.osgi;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.camel.impl.converter.AnnotationTypeConverterLoader;
import org.apache.camel.impl.converter.DefaultTypeConverter;
import org.apache.camel.impl.converter.TypeConverterLoader;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.util.ResolverUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.BundleContextAware;

@XmlRootElement(name = "camelContext")
@XmlAccessorType(XmlAccessType.FIELD)
public class CamelContextFactoryBean extends org.apache.camel.spring.CamelContextFactoryBean implements BundleContextAware {
    protected static final transient Log LOG = LogFactory.getLog(CamelContextFactoryBean.class);
    
    @XmlTransient
    private BundleContext bundleContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using BundleContext: " + bundleContext);
        }
        this.bundleContext = bundleContext;
    }
    
    protected SpringCamelContext createContext() {
        SpringCamelContext context = super.createContext();        
        if (bundleContext != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using OSGI resolvers");
            }
            LOG.debug("Using OsgiFactoryFinder");
            context.setFactoryFinderClass(OsgiFactoryFinder.class);
            LOG.debug("Using OsgiComponentResolver");
            context.setComponentResolver(new OsgiComponentResolver());
            LOG.debug("Using OsgiLanguageResolver");
            context.setLanguageResolver(new OsgiLanguageResolver());
            addOsgiAnnotationTypeConverterLoader(context, bundleContext);            
        } else {
            // TODO: should we not thrown an excpetion to not allow it to startup
            LOG.warn("BundleContext not set, cannot run in OSGI container");
        }
        
        return context;
    }    
    
    /**
     * The factory method for create the ResolverUtil
     * @return a new instance of ResolverUtil
     */
    protected ResolverUtil createResolverUtil() {
        if (bundleContext != null) {
            return new OsgiResolverUtil(bundleContext);
        } else {
            return new ResolverUtil();
        }
    }
    
    protected void addOsgiAnnotationTypeConverterLoader(SpringCamelContext context, BundleContext bundleContext) {
        LOG.debug("Using OsgiAnnotationTypeConverterLoader");

        DefaultTypeConverter typeConverter = (DefaultTypeConverter) context.getTypeConverter();
        List<TypeConverterLoader> typeConverterLoaders = typeConverter.getTypeConverterLoaders();

        // Remove the AnnotationTypeConverterLoader
        TypeConverterLoader atLoader = null; 
        for (TypeConverterLoader loader : typeConverterLoaders) {
            if (loader instanceof AnnotationTypeConverterLoader) {
                atLoader = loader;
                break;
            }
        }
        if (atLoader != null) {
            typeConverterLoaders.remove(atLoader);
        }        
        typeConverterLoaders.add(new OsgiAnnotationTypeConverterLoader(bundleContext));
    }
    
}
