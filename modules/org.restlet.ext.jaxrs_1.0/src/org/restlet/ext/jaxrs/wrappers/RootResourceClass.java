/*
 * Copyright 2005-2008 Noelios Consulting.
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License"). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at http://www.opensource.org/licenses/cddl1.txt If
 * applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */

package org.restlet.ext.jaxrs.wrappers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.ext.jaxrs.Authenticator;
import org.restlet.ext.jaxrs.exceptions.IllegalTypeException;
import org.restlet.ext.jaxrs.impl.MatchingResult;

/**
 * Instances represents a root resource class.
 * 
 * A resource class annotated with
 * 
 * {@link Path}: Root resource classes provide the roots of the resource class
 * tree and provide access to sub-resources, see chapter 2 of JSR-311-Spec.
 * 
 * @author Stephan Koops
 * 
 */
public class RootResourceClass extends ResourceClass {

    private Constructor<?> constructor;

    /**
     * Creates a wrapper for the given JAX-RS root resource class.
     * 
     * @param jaxRsClass
     *                the root resource class to wrap
     */
    public RootResourceClass(Class<?> jaxRsClass) {
        super(jaxRsClass, true);
        constructor = findJaxRsConstructor();
    }

    /**
     * Checks if the parameters for the constructor are valid for a JAX-RS root
     * resource class.
     * 
     * @param paramAnnotationss
     * @param parameterTypes
     * @returns true, if the
     * @throws IllegalTypeException
     *                 If a parameter is annotated with {@link HttpContext},
     *                 but the type is invalid (must be UriInfo, Request or
     *                 HttpHeaders).
     */
    private static boolean checkParamAnnotations(Constructor<?> constr) {
        Annotation[][] paramAnnotationss = constr.getParameterAnnotations();
        Class<?>[] parameterTypes = constr.getParameterTypes();
        for (int i = 0; i < paramAnnotationss.length; i++) {
            Annotation[] parameterAnnotations = paramAnnotationss[i];
            Class<?> parameterType = parameterTypes[i];
            boolean ok = checkParameterAnnotation(parameterAnnotations,
                    parameterType);
            if (!ok)
                return false;
        }
        return true;
    }

    private static boolean checkParameterAnnotation(
            Annotation[] parameterAnnotations, Class<?> parameterType) {
        if (parameterAnnotations.length == 0)
            return false;
        for (Annotation annotation : parameterAnnotations) {
            Class<? extends Annotation> annotationType = annotation
                    .annotationType();
            if (annotationType.equals(HeaderParam.class)) {
                continue;
            } else if (annotationType.equals(PathParam.class)) {
                continue;
            } else if (annotationType.equals(Context.class)) {
                if (parameterType.equals(UriInfo.class))
                    continue;
                if (parameterType.equals(Request.class))
                    continue;
                if (parameterType.equals(HttpHeaders.class))
                    continue;
                if (parameterType.equals(SecurityContext.class))
                    continue;
                throw new IllegalTypeException(
                        "The Type of a parameter annotated with @HttpContext must be UriInfo, Request or HttpHeaders.");
            } else if (annotationType.equals(MatrixParam.class)) {
                continue;
            } else if (annotationType.equals(QueryParam.class)) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * Creates an instance of the root resource class.
     * 
     * @param matchingResult
     *                the MatchingResult
     * @param allTemplParamsEnc
     *                all template parameters, encoded
     * @param restletRequ
     *                Th restlet request
     * @param restletResponse
     *                The Restlet response.
     * @param authenticator
     *                Authenticator for role requests, see
     *                {@link SecurityContext#isUserInRole(String)}.
     * @return
     * @throws Exception
     */
    public ResourceObject createInstance(MatchingResult matchingResult,
            MultivaluedMap<String, String> allTemplParamsEnc,
            Request restletRequ, Response restletResponse,
            Authenticator authenticator) throws Exception {
        Constructor<?> constructor = this.constructor;
        Object newInstance = createInstance(constructor, matchingResult,
                allTemplParamsEnc, restletRequ, restletResponse, authenticator);
        return new ResourceObject(newInstance, this);
    }

    /**
     * Creates an instance of the root resource class.
     * 
     * @param constructor
     *                the constructor to create an instance with.
     * @param matchingResult
     *                the MatchingResult
     * @param allTemplParamsEnc
     *                all template parameters, encoded
     * @param restletRequ
     *                The restlet request
     * @param restletResponse
     *                The Restlet response.
     * @param authenticator
     *                Authenticator for role requests, see
     *                {@link SecurityContext#isUserInRole(String)}.
     * @return
     * @throws Exception
     */
    public static Object createInstance(Constructor<?> constructor,
            MatchingResult matchingResult,
            MultivaluedMap<String, String> allTemplParamsEnc,
            Request restletRequ, Response restletResponse,
            Authenticator authenticator) throws Exception {
        Object[] args;
        if (constructor.getParameterTypes().length == 0)
            args = new Object[0];
        else
            args = getParameterValues(constructor.getParameterAnnotations(),
                    constructor.getParameterTypes(), matchingResult,
                    restletRequ, restletResponse, allTemplParamsEnc,
                    authenticator);
        return constructor.newInstance(args);
    }

    @Override
    public boolean equals(Object anotherObject) {
        if (this == anotherObject)
            return true;
        if (!(anotherObject instanceof RootResourceClass))
            return false;
        RootResourceClass otherRootResourceClass = (RootResourceClass) anotherObject;
        return this.jaxRsClass.equals(otherRootResourceClass.jaxRsClass);
    }

    /**
     * @return Returns the constructor to use for the given root resource class
     *         (See JSR-311-Spec, section 2.3)
     */
    private Constructor<?> findJaxRsConstructor() {
        return findJaxRsConstructor(getJaxRsClass());
    }

    /**
     * @param jaxRsClass
     * @return Returns the constructor to use for the given root resource class
     *         (See JSR-311-Spec, section 2.3)
     */
    public static Constructor<?> findJaxRsConstructor(Class<?> jaxRsClass) {
        Constructor<?> constructor = null;
        int constructorParamNo = Integer.MIN_VALUE;
        for (Constructor<?> constr : jaxRsClass.getConstructors()) {
            int constrParamNo = constr.getParameterTypes().length;
            if (constrParamNo <= constructorParamNo)
                continue; // ignore this constructor
            if (!checkParamAnnotations(constr))
                continue; // ignore this constructor
            constructor = constr;
            constructorParamNo = constrParamNo;
        }
        return constructor;
    }
}