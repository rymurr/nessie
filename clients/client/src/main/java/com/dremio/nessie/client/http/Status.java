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

import java.io.IOException;

import com.dremio.nessie.client.rest.NessieBadRequestException;
import com.dremio.nessie.client.rest.NessieForbiddenException;
import com.dremio.nessie.client.rest.NessieInternalServerException;
import com.dremio.nessie.client.rest.NessieNotAuthorizedException;
import com.dremio.nessie.client.rest.NessieServiceException;
import com.dremio.nessie.error.NessieConflictException;
import com.dremio.nessie.error.NessieError;
import com.dremio.nessie.error.NessieNotFoundException;

/**
 * HTTP request status enum. Map return code to concrete status type with message.
 */
public final class Status {

  public static final Status OK = new Status(200, "OK");
  public static final Status BAD_REQUEST = new Status(400, "Bad Request");
  public static final Status UNAUTHORIZED = new Status(401, "Unauthorized");
  public static final Status FORBIDDEN = new Status(403, "Forbidden");
  public static final Status NOT_FOUND = new Status(404, "Not Found");
  public static final Status CONFLICT = new Status(409, "Conflict");
  public static final Status UNSUPPORTED_MEDIA_TYPE = new Status(415, "Unsupported Media Type");
  public static final Status INTERNAL_SERVER_ERROR = new Status(500, "Internal Server Error");

  private final int code;
  private final String reason;

  private Status(final int statusCode, final String reason) {
    this.code = statusCode;
    this.reason = reason;
  }

  /**
   * get Status enum from http return code.
   * @param code return code
   * @return Status for return code
   * @throws UnsupportedOperationException if unknown status code
   */
  public static Status fromCode(int code, String message) {
    return new Status(code, message);
  }

  public int getCode() {
    return code;
  }

  public String getReason() {
    return reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Status status = (Status) o;

    return code == status.code;
  }

  @Override
  public int hashCode() {
    return code;
  }

  /**
   * Throws this Status as a Nessie exception, either an instance of {@link NessieServiceException}
   * (unchecked {@link RuntimeException} or an instanceof {@link com.dremio.nessie.error.BaseNessieClientServerException}
   * (checked {@link IOException}).
   *
   * @param error error object to pass into the Nessie-exception instance
   */
  public void throwAsNessieException(NessieError error) throws IOException {
    if (code == BAD_REQUEST.code) {
      throw new NessieBadRequestException(error);
    }
    if (code == UNAUTHORIZED.code) {
      throw new NessieNotAuthorizedException(error);
    }
    if (code == FORBIDDEN.code) {
      throw new NessieForbiddenException(error);
    }
    if (code == NOT_FOUND.code) {
      throw new NessieNotFoundException(error);
    }
    if (code == CONFLICT.code) {
      throw new NessieConflictException(error);
    }
    if (code == INTERNAL_SERVER_ERROR.code) {
      throw new NessieInternalServerException(error);
    }
    throw new NessieServiceException(error);
  }
}
