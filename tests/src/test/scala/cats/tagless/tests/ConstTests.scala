/*
 * Copyright 2019 cats-tagless maintainers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats.tagless.tests

import cats.tagless.Derive

@experimental
class ConstTests extends CatsTaglessTestSuite {

  test("const(42)") {
    val safe = Derive.const[SafeAlg, Int](42)
    assertEquals(safe.divide(1, 2), 42)
    assertEquals(safe.parseInt("NaN"), 42)
  }

  test("void") {
    val store = Derive.void[KVStore]
    assertEquals(store.get("tea"), ())
    assertEquals(store.put("tea", "mint"), ())
  }

  test("side effects") {
    val it = (1 to 5).iterator
    val safe = Derive.const[SafeAlg, Int](it.next())
    assertEquals(safe.divide(1, 2), 1)
    assertEquals(safe.parseInt("NaN"), 1)
  }

  test("readable macro error") {
    assert(compileErrors("Derive.const[NotSimpleAlg, Int](42)").contains("Expected method str to return"))
  }

  trait NotSimpleAlg[F[_]] extends SafeAlg[F] {
    def str(s: String): String
  }
}
