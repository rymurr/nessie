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
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.dev.appstate.ApplicationStateNotification;
import io.quarkus.runtime.Quarkus;

public class TestNessieServer implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(TestNessieServer.class);
  private static final Object LOCK = new Object();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Map<String, String> properties = new HashMap<>();

  public TestNessieServer(Properties properties) {
    LocalConfigSource.setProperties(properties);
  }

  /**
   * Start a quarkus server.
   *
   * <p>
   *  We first start the server, then wait for it to start. After start we copy the final properties back into the local config to pass
   *  to the unit tests. Particularly the random http port is useful to get this way. This happens under lock so the chance of a property
   *  changing from another thread/process is minimal.
   * </p>
   */
  public void start() {
    synchronized (LOCK) {
      logger.info("Quarkus start has been called.");
      executor.submit(() -> {
        logger.info("Starting quarkus");
        Quarkus.run(null, (i, t) -> logger.info("Quarkus Stopped"));

      });
      ApplicationStateNotification.waitForApplicationStart();
      LocalConfigSource lcs = new LocalConfigSource();
      lcs.getProperties().forEach((k, v) -> LocalConfigSource.setProperty(k, System.getProperty(k, v)));
      properties.putAll(lcs.getProperties());
    }
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public void close() throws Exception {
    Quarkus.blockingExit();
  }

  /**
   * Create and start a quarkus server in a custom classloader.
   */
  public static TestNessieServer startInCustomClassLoader(ClassLoader parent, Map<String, String> props)
      throws ReflectiveOperationException {
    ClassLoader classLoader = new URLClassLoader(new URL[0], parent);
    Properties properties = (Properties) classLoader.loadClass("java.util.Properties").getConstructor().newInstance();
    props.forEach(properties::setProperty);
    TestNessieServer ts = (TestNessieServer) classLoader.loadClass("com.dremio.nessie.server.testing.TestNessieServer")
                                                        .getConstructor(Properties.class)
                                                        .newInstance(properties);
    ts.start();
    return ts;
  }
}
