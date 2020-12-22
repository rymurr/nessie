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
package com.dremio.nessie.client.rest;

import java.io.IOException;
import java.io.InputStream;

import com.dremio.nessie.client.http.HttpClientException;
import com.dremio.nessie.client.http.ResponseContext;
import com.dremio.nessie.client.http.Status;
import com.dremio.nessie.error.NessieError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class ResponseCheckFilter {

  /**
   * check that response had a valid return code. Throw exception if not.
   * @param con open http connection
   * @param mapper Jackson ObjectMapper instance for this client
   * @throws IOException Throws IOException for certain error types.
   */
  public static void checkResponse(ResponseContext con, ObjectMapper mapper) throws IOException {
    final Status status;
    final NessieError error;
    try {
      status = con.getResponseCode();
      if (status.getCode() / 100 == 2) {
        return;
      }
    } catch (IOException e) {
      throw new HttpClientException(e);
    }

    try (InputStream is = con.getErrorStream()) {
      error = decodeErrorObject(status, is, mapper.readerFor(NessieError.class));
    } catch (IOException e) {
      throw new HttpClientException(e);
    }

    status.throwAsNessieException(error);
  }

  private static NessieError decodeErrorObject(Status status, InputStream inputStream, ObjectReader reader) {
    NessieError error;
    if (inputStream == null) {
      error = new NessieError(status.getCode(), status.getReason(),
                              "Could not parse error object in response.",
                              new RuntimeException("Could not parse error object in response."));
    } else {
      try {
        error = reader.readValue(inputStream);
      } catch (IOException e) {
        error = new NessieError(status.getCode(), status.getReason(), null, e);
      }
    }
    return error;
  }

}
