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
package org.projectnessie.versioned.tiered;

import java.util.stream.Stream;
import org.projectnessie.versioned.Key;
import org.projectnessie.versioned.WithPayload;

/**
 * Consumer for fragments.
 *
 * <p>Implementations must return a shared state ({@code this}) from its method.
 */
public interface Fragment extends BaseValue<Fragment> {

  /**
   * The commit metadata id for this l1.
   *
   * <p>Must be called exactly once.
   *
   * @param keys The keys to add.
   * @return This consumer.
   */
  default Fragment keys(Stream<WithPayload<Key>> keys) {
    keys.forEach(ignored -> {});
    return this;
  }
}
