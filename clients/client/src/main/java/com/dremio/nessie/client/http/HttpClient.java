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
package com.dremio.nessie.client.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Simple Http client to make REST calls.
 * <p>
 * Nessie uses its own, tiny HTTP client shim, because many clients like Hive2 and Hive3
 * bundle different http-client implementations in rather incompatible versions. Given
 * the requirements that Nessie has (low throughput, no "fancy" requests, etc), it's at
 * least at the moment easier to bundle a simple http-client instead of adding another
 * shaded dependency to the mix.
 * </p>
 * <p>
 * Assumptions:
 * <ul>
 *   <li>always send/receive JSON</li>
 *   <li>set headers accordingly by default</li>
 *   <li>very simple interactions w/ API</li>
 *   <li>no cookies</li>
 *   <li>no caching of connections, rely on the keep-alive-cache via
 *   {@link java.net.HttpURLConnection}</li>
 * </ul>
 * </p>
 */
public class HttpClient {
  private final ObjectMapper mapper;
  private final String base;
  private final String accept;
  private final List<RequestFilter> requestFilters = new ArrayList<>();
  private final List<ResponseFilter> responseFilters = new ArrayList<>();

  public enum Method {
    GET,
    POST,
    PUT,
    DELETE
  }

  /**
   * Construct an HTTP client with a universal Accept header.
   *
   * @param base uri base eg https://example.com
   * @param accept Accept header eg "application/json"
   */
  public HttpClient(String base, String accept) {
    base = checkNonNullTrim(base);
    accept = checkNonNullTrim(accept);
    if (!base.startsWith("http://") && !base.startsWith("https://")) {
      throw new IllegalArgumentException("base URI must start with http:// or https://");
    }

    this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                                    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    this.base = base;
    this.accept = accept;
  }

  public HttpClient(String base) {
    this(base, "application/json");
  }

  private static String checkNonNullTrim(String base) {
    Objects.requireNonNull(base);
    base = base.trim();
    return base;
  }

  public void register(RequestFilter filter) {
    filter.init(mapper);
    requestFilters.add(filter);
  }

  public void register(ResponseFilter filter) {
    filter.init(mapper);
    responseFilters.add(filter);
  }

  public HttpRequest create() {
    return new HttpRequest(base, accept, mapper, requestFilters, responseFilters);
  }
}
