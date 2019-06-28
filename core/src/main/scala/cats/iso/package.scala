/*
 * Copyright 2017 Kailuo Wang
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

package cats

package object iso {

  /**Const types, one (from shapeless) without structure
    * and the one in cats, with structure and rich in typeclasses;
    * there normally has to be an isomorphism between the two for all shapes of A and B
    *   but until we have king polymorphism it will be defined on some useful combinations only */
  type ShapelessConst[C] = {
    type λ[T] = C
  }
  type ShapeConst[A, B] = ShapelessConst[A]#λ[B]
  type CatsConst[A, B] = cats.data.Const[A, B]
  val CatsConst = cats.data.Const

  /**alias for binatural trans */
  type ~~>[F[_,_], G[_,_]] = BiNaturalTransformation[F, G]

  /**Natural transformations for algebras of various shapes */
  type ≈>[Alg1[_[_]], Alg2[_[_]]] = AlgebraNatTrans[Alg1, Alg2]
  type ≈~>[Alg1[_[_],_], Alg2[_[_],_]] = Algebra1NatTrans[Alg1, Alg2]
  type ≈~~>[Alg1[_[_],_,_], Alg2[_[_],_,_]] = Algebra2NatTrans[Alg1, Alg2]
  type ≈≈>[Alg1[_[_,_]], Alg2[_[_,_]]] = BiAlgebraNatTrans[Alg1, Alg2]
  type ≈≈~>[Alg1[_[_,_],_], Alg2[_[_,_],_]] = BiAlgebra1NatTrans[Alg1, Alg2]
  type ≈≈~~>[Alg1[_[_,_],_,_], Alg2[_[_,_],_,_]] = BiAlgebra2NatTrans[Alg1, Alg2]
  val ≈> = AlgebraNatTrans
  val ≈~> = Algebra1NatTrans
  val ≈~~> = Algebra2NatTrans
  val ≈≈> = BiAlgebraNatTrans
  val ≈≈~> = BiAlgebra1NatTrans
  val ≈≈~~> = BiAlgebra2NatTrans

  /**Set isomorphism */
  type IsoSet[A, B] = Iso[Function1, A, B]
  type <=>[A, B] = IsoSet[A, B]
  val <=> = IsoSet

  /**Natural isomorphism between functors */
  type IsoFunctor[F[_], G[_]] = Iso2[~>, F, G]
  type <~>[F[_], G[_]] = IsoFunctor[F, G]
  val <~> = IsoFunctor

  type IsoBifunctor[F[_,_], G[_,_]] = Iso3[~~>, F, G]
  type <~~>[F[_,_], G[_,_]] = IsoBifunctor[F, G]
  val <~~> = IsoBifunctor

  /**Isomorphism between algebras of various shapes */
  type IsoAlgFunctor[Alg1[_[_]], Alg2[_[_]]] = IsoAlg[≈>, Alg1, Alg2]
  type <≈>[Alg1[_[_]], Alg2[_[_]]] = IsoAlgFunctor[Alg1, Alg2]
  val <≈> = IsoAlgFunctor

  type IsoAlg1Functor[Alg1[_[_],_], Alg2[_[_],_]] = IsoAlg1[≈~>, Alg1, Alg2]
  type <≈~>[Alg1[_[_],_], Alg2[_[_],_]] = IsoAlg1Functor[Alg1, Alg2]
  val <≈~> = IsoAlg1Functor

  type IsoAlg2Functor[Alg1[_[_],_,_], Alg2[_[_],_,_]] = IsoAlg2[≈~~>, Alg1, Alg2]
  type <≈~~>[Alg1[_[_],_,_], Alg2[_[_],_,_]] = IsoAlg2Functor[Alg1, Alg2]
  val <≈~~> = IsoAlg2Functor

//  type IsoBiAlgFunctor[Alg1[_[_,_]], Alg2[_[_,_]]] = IsoBiAlg[≈≈>, Alg1, Alg2]
//  type <≈≈>[Alg1[_[_,_],_,_], Alg2[_[_,_],_,_]] = IsoBiAlgFunctor[Alg1, Alg2]

//  type IsoBiAlg1Functor[Alg1[_[_,_],_,_], Alg2[_[_,_],_,_]] = IsoBiAlg1[≈≈~>, Alg1, Alg2]
//  type <≈≈~>[Alg1[_[_,_],_,_], Alg2[_[_,_],_,_]] = IsoBiAlg1Functor[Alg1, Alg2]

  type IsoBiAlg2Functor[Alg1[_[_,_],_,_], Alg2[_[_,_],_,_]] = IsoBiAlg2[≈≈~~>, Alg1, Alg2]
  type <≈≈~~>[Alg1[_[_,_],_,_], Alg2[_[_,_],_,_]] = IsoBiAlg2Functor[Alg1, Alg2]

}
