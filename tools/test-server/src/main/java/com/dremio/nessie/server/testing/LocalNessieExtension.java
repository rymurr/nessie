/*
 * Copyright (C) 2020 Dremio
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
package com.dremio.nessie.server.testing;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;

public class LocalNessieExtension extends TypeBasedParameterResolver<Map<String, String>> implements AfterAllCallback, BeforeAllCallback {

  private static final String NESSIE_LOCAL = "nessie-local";
  private static final String NESSIE_PROPS = "nessie-local-props";


  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    getNessie(context).close();
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    ClassLoader classLoader = new URLClassLoader(new URL[0], LocalNessieExtension.class.getClassLoader());

    Map<String, String> properties = new HashMap<>();
    properties.put("quarkus.http.port", "0");
    TestNessieServer ts = (TestNessieServer) classLoader.loadClass("com.dremio.nessie.server.testing.TestNessieServer")
                                                        .getMethod("startInCustomClassLoader", ClassLoader.class, Map.class)
                                                        .invoke(null, classLoader, properties);
    getStore(context).put(NESSIE_PROPS, ts.getProperties());
    getStore(context).put(NESSIE_LOCAL, (AutoCloseable) ts);
  }

  private Store getStore(ExtensionContext context) {
    return context.getStore(Namespace.create(getClass(), context.getRoot()));
  }

  private AutoCloseable getNessie(ExtensionContext context) {
    return (AutoCloseable) getStore(context).get(NESSIE_LOCAL);
  }

  @Override
  public Map<String, String> resolveParameter(ParameterContext parameterContext,
                                              ExtensionContext extensionContext) throws ParameterResolutionException {
    return (Map<String, String>) getStore(extensionContext).get(NESSIE_PROPS);
  }
}
