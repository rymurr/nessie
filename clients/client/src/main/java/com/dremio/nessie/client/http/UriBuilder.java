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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Construct a URI from base and paths. Adds query parameters, supports templates and handles url encoding of path.
 */
class UriBuilder {

  private final String baseUri;
  private final List<String> uri = new ArrayList<>();
  private final Map<String, String> query = new HashMap<>();
  private final Map<String, String> templateValues = new HashMap<>();

  UriBuilder(String baseUri) {
    baseUri = checkNonNullTrim(baseUri);
    this.baseUri = baseUri;
  }

  private static String checkNonNullTrim(String str) {
    Objects.requireNonNull(str);
    str = str.trim();
    return str;
  }

  UriBuilder path(String path) {
    path = checkNonNullTrim(path);
    uri.add(path);
    return this;
  }

  UriBuilder queryParam(String name, String value) {
    name = checkNonNullTrim(name);
    query.put(name, value);
    return this;
  }

  UriBuilder resolveTemplate(String name, String value) {
    name = checkNonNullTrim(name);
    value = checkNonNullTrim(value);
    templateValues.put(String.format("{%s}", name), value);
    return this;
  }

  String build() throws HttpClientException {
    StringBuilder uriBuilder = new StringBuilder();
    List<String> transformedUri = new ArrayList<>();
    transformedUri.add(baseUri);
    for (String s: this.uri) {
      transformedUri.add(template(s));
    }
    if (!templateValues.isEmpty()) {
      String keys = String.join(";", templateValues.keySet());
      throw new IllegalStateException(String.format("Cannot build uri. Not all template keys (%s) were used in uri %s", keys, uri));
    }
    appendTo(uriBuilder, transformedUri.iterator(), "/");

    if (!query.isEmpty()) {
      uriBuilder.append("?");
      List<String> params = new ArrayList<>();
      for (Map.Entry<String, String> kv: query.entrySet()) {
        if (kv.getValue() == null) {
          continue;
        }
        params.add(String.format("%s=%s", encode(kv.getKey()), encode(kv.getValue())));
      }
      appendTo(uriBuilder, params.iterator(), "&");
    }
    return uriBuilder.toString();
  }

  private String template(String input) throws HttpClientException {
    return Arrays.stream(input.split("/"))
                 .map(p -> encode((templateValues.containsKey(p)) ? templateValues.remove(p) : p))
                 .collect(Collectors.joining("/"));
  }

  private static String encode(String s) throws HttpClientException {
    try {
      //URLEncoder encodes space ' ' to + according to how encoding forms should work. When encoding URLs %20 should be used instead.
      return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
    } catch (UnsupportedEncodingException e) {
      throw new HttpClientException(e);
    }
  }

  private static void appendTo(StringBuilder appendable, Iterator<String> parts, String separator) {
    if (parts.hasNext()) {
      appendable.append(parts.next());
      while (parts.hasNext()) {
        appendable.append(separator);
        appendable.append(parts.next());
      }
    }
  }

}
