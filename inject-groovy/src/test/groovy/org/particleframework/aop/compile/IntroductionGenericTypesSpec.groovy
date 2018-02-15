/*
 * Copyright 2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.particleframework.aop.compile

import org.particleframework.AbstractBeanDefinitionSpec
import org.particleframework.context.DefaultBeanContext
import org.particleframework.core.type.ReturnType
import org.particleframework.inject.BeanDefinition
import org.particleframework.inject.BeanFactory
import org.particleframework.inject.ExecutableMethod
import org.particleframework.inject.writer.BeanDefinitionVisitor

/**
 * @author graemerocher
 * @since 1.0
 */
class IntroductionGenericTypesSpec extends AbstractBeanDefinitionSpec {

    void "test that generic return types are correct when implementing an interface with type arguments"() {
        when:
        BeanDefinition beanDefinition = buildBeanDefinition('test.MyBean' + BeanDefinitionVisitor.PROXY_SUFFIX, '''
package test;

import org.particleframework.aop.introduction.*;
import org.particleframework.context.annotation.*;
import java.net.*;

interface MyInterface<T extends URL> {

    T getURL();
    
    java.util.List<T> getURLs();
}


@Stub
@javax.inject.Singleton
interface MyBean extends MyInterface<URL> {
}

''')
        then:
        !beanDefinition.isAbstract()
        beanDefinition != null
        beanDefinition.injectedFields.size() == 0
        beanDefinition.executableMethods.size() == 2
        beanDefinition.executableMethods[0].methodName == 'getURL'
        beanDefinition.executableMethods[0].returnType.type == URL
        beanDefinition.executableMethods[1].returnType.type == List
        beanDefinition.executableMethods[1].returnType.asArgument().hasTypeVariables()
        beanDefinition.executableMethods[1].returnType.asArgument().typeVariables['E'].type == URL
    }


    void "test that generic return types are correct when implementing an interface with type arguments 2"() {
        when:
        BeanDefinition beanDefinition = buildBeanDefinition('test.MyBean' + BeanDefinitionVisitor.PROXY_SUFFIX, '''
package test;

import org.particleframework.aop.introduction.*;
import org.particleframework.context.annotation.*;
import java.net.*;

interface MyInterface<T extends Person> {
    T[] getPeopleArray();

    def <V extends java.net.URL> java.util.Map<T,V> getPeopleMap();

    io.reactivex.Single<java.util.List<T>> getPeopleSingle();
    
    T getPerson();
    
    java.util.List<T> getPeople();
    
    void save(T person);
    
    void saveAll(java.util.List<T> person);
    
    java.util.List<T[]> getPeopleListArray();
    
    
    
}


@Stub
@javax.inject.Singleton
interface MyBean extends MyInterface<SubPerson> {

}

class Person {}
class SubPerson extends Person {}

''')
        then:
        !beanDefinition.isAbstract()
        beanDefinition != null
        returnType(beanDefinition, "getPerson").type.name == 'test.SubPerson'
        returnType(beanDefinition, "getPeopleArray").type.name.contains('test.SubPerson')
        returnType(beanDefinition, "getPeopleMap").typeVariables['K'].type.name == 'test.SubPerson'
        returnType(beanDefinition, "getPeopleMap").typeVariables['V'].type == URL
        returnType(beanDefinition, "getPeopleSingle").typeVariables['T'].type== List
        returnType(beanDefinition, "getPeopleSingle").typeVariables['T'].typeVariables['E'].type.name == 'test.SubPerson'
        returnType(beanDefinition, "getPeople").type == List
        returnType(beanDefinition, "getPeople").asArgument().hasTypeVariables()
        returnType(beanDefinition, "getPeople").asArgument().typeVariables['E'].type.name == 'test.SubPerson'
        returnType(beanDefinition, "getPeopleArray").type.isArray()
        returnType(beanDefinition, "getPeopleListArray").type == List
        returnType(beanDefinition, "getPeopleListArray").typeVariables['E'].type.isArray()




        when:
        def context = new DefaultBeanContext()
        context.start()
        def instance = ((BeanFactory)beanDefinition).build(context, beanDefinition)


        then:"the methods are invocable"
        instance.getPerson() == null
        instance.getPeople() == null
        instance.getPeopleArray() == null
        instance.getPeopleSingle() == null
        instance.save(null) == null
        instance.saveAll([]) == null

    }

    ReturnType returnType(BeanDefinition bd, String name) {
        bd.findPossibleMethods(name).findFirst().get().returnType
    }
}