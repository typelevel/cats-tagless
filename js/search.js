// When the user clicks on the search box, we want to toggle the search dropdown
function displayToggleSearch(e) {
  e.preventDefault();
  e.stopPropagation();

  closeDropdownSearch(e);
  
  if (idx === null) {
    console.log("Building search index...");
    prepareIdxAndDocMap();
    console.log("Search index built.");
  }
  const dropdown = document.querySelector("#search-dropdown-content");
  if (dropdown) {
    if (!dropdown.classList.contains("show")) {
      dropdown.classList.add("show");
    }
    document.addEventListener("click", closeDropdownSearch);
    document.addEventListener("keydown", searchOnKeyDown);
    document.addEventListener("keyup", searchOnKeyUp);
  }
}

//We want to prepare the index only after clicking the search bar
var idx = null
const docMap = new Map()

function prepareIdxAndDocMap() {
  const docs = [  
    {
      "title": "FAQ",
      "url": "/cats-tagless/faq.html",
      "content": "FAQs Does cats-tagless support algebras with extra type parameters? Yes. e.g. import cats.tagless._ import cats.~&gt; import util.Try @autoFunctorK @finalAlg trait Foo[F[_], T] { def a(i: Int): F[T] } implicit val tryFoo: Foo[Try, String] = i =&gt; Try(i.toString) implicit val fk: Try ~&gt; Option = λ[Try ~&gt; Option](_.toOption) import Foo.autoDerive._ Foo[Option, String].a(3) // res0: Option[String] = Some(value = \"3\") Does Cats-tagless support algebras with abstract type member? Yes but with some caveats. The FunctorK instance it generates does not refine to the type member. E.g. @autoFunctorK @finalAlg trait Bar[F[_]] { type T def a(i: Int): F[T] } object Bar { type Aux[F[_], A] = Bar[F] { type T = A } } implicit val tryBarString: Bar.Aux[Try, String] = new Bar[Try] { type T = String def a(i: Int): Try[String] = Try(i.toString) } If you try to map this tryBarString to a Bar[Option], the type T of the Bar[Option] isn’t refined. That is, you can do import Bar.autoDerive._ Bar[Option].a(3) // res1: Option[Bar[Option]#T] = Some(value = \"3\") But you can’t create a Bar[Option] { type T = String } from the tryBarString using FunctorK. val barOption: Bar[Option] { type T = String } = tryBarString.mapK(fk) // error: value mapK is not a member of repl.MdocSession.MdocApp.Bar.Aux[scala.util.Try,String] // val barOption: Bar[Option] { type T = String } = tryBarString.mapK(fk) // ^^^^^^^^^^^^^^^^^ However, there is also mapK function added to the companion object of the algebra which gives you more precise type. val barOption: Bar[Option] { type T = String } = Bar.mapK(tryBarString)(fk) // barOption: Bar[Option]{type T = String} = repl.MdocSession$MdocApp$Bar$$anon$6$$anon$7@b4b7863 Also since the FunctorK (or InvariantK) instance uses a dependent type on the original interpreter, you may run into dependent type related issues. In those cases, this mapK (or imapK) on the companion object may give better result. Here are two examples. Cannot resolve implicit defined by the dependent type import cats.Show import cats.implicits._ @autoFunctorK trait Algebra[F[_]] { type TC[T] def a[T: TC](t: T): F[String] } object tryAlgInt extends Algebra[Try] { type TC[T] = Show[T] def a[T: TC](t: T): Try[String] = Try(t.show) } FunctorK.mapK will result in unusable interpreter due to scalac’s difficulty in resolving implicit based on dependent type. FunctorK[Algebra].mapK(tryAlgInt)(fk).a(List(1,2,3)) // error: could not find implicit value for evidence parameter of type stabilizer$1.TC[List[Int]] // FunctorK[Algebra].mapK(tryAlgInt)(fk).a(List(1,2,3)) // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ The mapK on the companion will work fine. Algebra.mapK(tryAlgInt)(fk).a(List(1,2,3)) // res4: Option[String] = Some(value = \"List(1, 2, 3)\") Cannot take in argument whose type is a dependent type @autoInvariantK trait InvAlg[F[_]] { type T def a(i: F[T]): F[T] } object tryInvAlgInt extends InvAlg[Try] { type T = String def a(i: Try[String]): Try[String] = i.map(_ + \"a\") } implicit val rk: Option ~&gt; Try = λ[Option ~&gt; Try](o =&gt; Try(o.get)) InvariantK.imapK will result in unusable interpreter because method a’s argument type is a dependent type on original interpreter. InvariantK[InvAlg].imapK(tryInvAlgInt)(fk)(rk).a(Some(\"4\")) // error: type mismatch; // found : String(\"4\") // required: stabilizer$2.T // InvariantK[InvAlg].imapK(tryInvAlgInt)(fk)(rk).a(Some(\"4\")) // ^^^ The imapK on the companion will work fine InvAlg.imapK(tryInvAlgInt)(fk)(rk).a(Some(\"4\")) // res6: Option[String] = Some(value = \"4a\") I am seeing diverging implicit expansion for type MyAlgebra[F] If you see error likes the following when you try to summon a specific instance of MyAlgebra diverging implicit expansion for type MyAlgebra[F] [error] starting with method autoDeriveFromFunctorK in object MyAlgebra It probably means that necessary implicit MyAlgebra instance and/or the corresponding FunctionK is missing in scope. I’m seeing java.lang.NoClassDefFoundError at runtime If you see an error like the following java.lang.NoClassDefFoundError: com/mypackage/MyAlgebra$ This can be due to multiple annotations on a trait without a companion object. This is a known issue. The solution is to add an empty companion object: @finalAlg @autoContravariant trait MyAlgebra[T] { def foo(t: T): String } object MyAlgebra {}"
    } ,    
    {
      "title": "Home",
      "url": "/cats-tagless/",
      "content": "Cats-tagless is a small library built to facilitate composing tagless final encoded algebras. Installation Cats-tagless is available on scala 2.12, 2.13 and Scala.js. Add the following dependency in build.sbt // latest version indicated in the badge above libraryDependencies += \"org.typelevel\" %% \"cats-tagless-macros\" % latestVersion // For Scala 2.13, enable macro annotations scalacOptions += \"-Ymacro-annotations\" // For Scala 2.12, scalamacros paradise is needed as well. addCompilerPlugin(\"org.scalamacros\" % \"paradise\" % \"2.1.0\" cross CrossVersion.full) Auto-transforming interpreters Say we have a typical tagless encoded algebra ExpressionAlg[F[_]] import cats.tagless._ @finalAlg @autoFunctorK @autoSemigroupalK @autoProductNK trait ExpressionAlg[F[_]] { def num(i: String): F[Float] def divide(dividend: Float, divisor: Float): F[Float] } With an interpreter implemented using Try import util.Try implicit object tryExpression extends ExpressionAlg[Try] { def num(i: String) = Try(i.toFloat) def divide(dividend: Float, divisor: Float) = Try(dividend / divisor) } Similar to simulacrum, @finalAlg adds an apply method in the companion object so that you can do implicit calling. ExpressionAlg[Try] // res0: ExpressionAlg[Try] = repl.MdocSession$MdocApp$tryExpression$@14cdb8a8 Cats-tagless provides a FunctorK type class to map over algebras using cats’ FunctionK. More specifically With an instance of FunctorK[ExpressionAlg], you can transform an ExpressionAlg[F] to a ExpressionAlg[G] using a FunctionK[F, G], a.k.a. F ~&gt; G. The @autoFunctorK annotation adds the following line (among some other code) in the companion object. object ExpressionAlg { implicit def functorKForExpressionAlg: FunctorK[ExpressionAlg] = Derive.functorK[ExpressionAlg] } This functorKForExpressionAlg is a FunctorK instance for ExpressionAlg generated using cats.tagless.Derive.functorK. Note that the usage of @autoFunctorK, like all other @autoXXXX annotations provided by cats-tagless, is optional, you can manually add this instance yourself. With this implicit instance in scope, you can call the syntax .mapK method to perform the transformation. import cats.tagless.implicits._ import cats.implicits._ import cats._ implicit val fk : Try ~&gt; Option = λ[Try ~&gt; Option](_.toOption) // fk: Try ~&gt; Option = repl.MdocSession$MdocApp$$anon$14@6714f8d6 tryExpression.mapK(fk) // res1: ExpressionAlg[[A]Option[A]] = repl.MdocSession$MdocApp$ExpressionAlg$$anon$1$$anon$2@71e561b9 Note that the Try ~&gt; Option is implemented using kind projector’s polymorphic lambda syntax. @autoFunctorK also add an auto derivation, so that if you have an implicit ExpressionAlg[F] and an implicit F ~&gt; G, you automatically have a ExpressionAlg[G]. Obviously FunctorK instance is only possible when the effect type F[_] appears only in the covariant position (i.e. the return types). For algebras with effect type also appearing in the contravariant position (i.e. argument types), Cats-tagless provides a InvariantK type class and an autoInvariantK annotation to automatically generate instances. import ExpressionAlg.autoDerive._ ExpressionAlg[Option] // res2: ExpressionAlg[Option] = repl.MdocSession$MdocApp$ExpressionAlg$$anon$1$$anon$2@790f1b61 This auto derivation can be turned off using an annotation argument: @autoFunctorK(autoDerivation = false). Make stack safe with Free Another quick win with a FunctorK instance is to lift your algebra interpreters to use Free to achieve stack safety. For example, say you have an interpreter using Try @finalAlg @autoFunctorK trait Increment[F[_]] { def plusOne(i: Int): F[Int] } implicit object incTry extends Increment[Try] { def plusOne(i: Int) = Try(i + 1) } def program[F[_]: Monad: Increment](i: Int): F[Int] = for { j &lt;- Increment[F].plusOne(i) z &lt;- if (j &lt; 10000) program[F](j) else Monad[F].pure(j) } yield z Obviously, this program is not stack safe. program[Try](0) Now lets use auto derivation to lift the interpreter with Try into an interpreter with Free import cats.free.Free import cats.arrow.FunctionK import Increment.autoDerive._ implicit def toFree[F[_]]: F ~&gt; Free[F, *] = λ[F ~&gt; Free[F, *]](t =&gt; Free.liftF(t)) program[Free[Try, *]](0).foldMap(FunctionK.id) // res4: Try[Int] = Success(value = 10000) Again the magic here is that Cats-tagless auto derive an Increment[Free[Try, *]] when there is an implicit Try ~&gt; Free[Try, *] and a Increment[Try] in scope. This auto derivation can be turned off using an annotation argument: @autoFunctorK(autoDerivation = false). Vertical composition Say you have another algebra that could use the ExpressionAlg. trait StringCalculatorAlg[F[_]] { def calc(i: String): F[Float] } When writing interpreter for this one, we can call for an interpreter for ExpressionAlg. class StringCalculatorOption(implicit exp: ExpressionAlg[Option]) extends StringCalculatorAlg[Option] { def calc(i: String): Option[Float] = { val numbers = i.split(\"/\") for { s1 &lt;- numbers.headOption f1 &lt;- exp.num(s1) s2 &lt;- numbers.lift(1) f2 &lt;- exp.num(s2) r &lt;- exp.divide(f1, f2) } yield r } } Note that the ExpressionAlg interpreter needed here is a ExpressionAlg[Option], while we only defined a ExpressionAlg[Try]. However since we have a fk: Try ~&gt; Option in scope, we can automatically have ExpressionAlg[Option] in scope through autoDerive. We can just write new StringCalculatorOption // res5: StringCalculatorOption = repl.MdocSession$MdocApp$StringCalculatorOption@fef410 Horizontal composition You can use the SemigroupalK type class to create a new interpreter that runs two interpreters simultaneously and return the result as a cats.Tuple2K. The @autoSemigroupalK attribute add an instance of SemigroupalK to the companion object. Example: val prod = ExpressionAlg[Option].productK(ExpressionAlg[Try]) // prod: ExpressionAlg[[γ$7$]data.Tuple2K[[A]Option[A], [T]Try[T], γ$7$]] = repl.MdocSession$MdocApp$ExpressionAlg$$anon$5$$anon$6@40ec6e91 prod.num(\"2\") // res6: data.Tuple2K[[A]Option[A], [T]Try[T], Float] = Tuple2K( // first = Some(value = 2.0F), // second = Success(value = 2.0F) // ) If you want to combine more than 2 interpreters, the @autoProductNK attribute add a series of product{n}K (n = 3..9) methods to the companion object. For example. val listInterpreter = ExpressionAlg[Option].mapK(λ[Option ~&gt; List](_.toList)) val vectorInterpreter = listInterpreter.mapK(λ[List ~&gt; Vector](_.toVector)) val prod4 = ExpressionAlg.product4K(ExpressionAlg[Try], ExpressionAlg[Option], listInterpreter, vectorInterpreter) // prod4: ExpressionAlg[[T](Try[T], Option[T], List[T], Vector[T])] = repl.MdocSession$MdocApp$ExpressionAlg$$anon$8@172f65f3 prod4.num(\"3\") // res7: (Try[Float], Option[Float], List[Float], Vector[Float]) = ( // Success(value = 3.0F), // Some(value = 3.0F), // List(3.0F), // Vector(3.0F) // ) prod4.num(\"invalid\") // res8: (Try[Float], Option[Float], List[Float], Vector[Float]) = ( // Failure( // exception = java.lang.NumberFormatException: For input string: \"invalid\" // ), // None, // List(), // Vector() // ) Unlike productK living in the SemigroupalK type class, currently we don’t have a type class for these product{n}K operations yet. @autoFunctor and @autoInvariant Cats-tagless also provides three derivations that can generate cats.Functor, cats.FlatMap and cats.Invariant instance for your trait. @autoFunctor @finalAlg @autoFunctor trait SimpleAlg[T] { def foo(a: String): T def bar(d: Double): Double } implicit object SimpleAlgInt extends SimpleAlg[Int] { def foo(a: String): Int = a.length def bar(d: Double): Double = 2 * d } SimpleAlg[Int].map(_ + 1).foo(\"blah\") // res9: Int = 5 Methods which return not the effect type are unaffected by the map function. SimpleAlg[Int].map(_ + 1).bar(2) // res10: Double = 4.0 @autoFlatMap @autoFlatMap trait StringAlg[T] { def foo(a: String): T } object LengthAlg extends StringAlg[Int] { def foo(a: String): Int = a.length } object HeadAlg extends StringAlg[Char] { def foo(a: String): Char = a.headOption.getOrElse(' ') } val hintAlg = for { length &lt;- LengthAlg head &lt;- HeadAlg } yield head.toString ++ \"*\" * (length - 1) hintAlg.foo(\"Password\") // res11: String = \"P*******\" @autoInvariant @finalAlg @autoInvariant trait SimpleInvAlg[T] { def foo(a: T): T } implicit object SimpleInvAlgString extends SimpleInvAlg[String] { def foo(a: String): String = a.reverse } SimpleInvAlg[String].imap(_.toInt)(_.toString).foo(12) // res12: Int = 21 @autoContravariant @finalAlg @autoContravariant trait SimpleContraAlg[T] { def foo(a: T): String } implicit object SimpleContraAlgString extends SimpleContraAlg[String] { def foo(a: String): String = a.reverse } SimpleContraAlg[String].contramap[Int](_.toString).foo(12) // res13: String = \"21\" Note that if there are multiple type parameters on the trait, @autoFunctor, @autoInvariant, @autoContravariant will treat the last one as the target T."
    } ,        
    {
      "title": "Type classes",
      "url": "/cats-tagless/typeclasses.html",
      "content": "Type classes Currently, there are five type classes defined in Cats-tagless: FunctorK, ContravariantK, InvariantK, SemigroupalK, and ApplyK. They can be deemed as somewhat higher kinded versions of the corresponding type classes in cats. FunctorK def mapK[F[_], G[_]](af: A[F])(fk: F ~&gt; G): A[G] For tagless final algebras whose effect F appears only in the covariant position, instance of FunctorK can be auto generated through the autoFunctorK annotation. ContravariantK def contramapK[F[_], G[_]](af: A[F])(fk: G ~&gt; F): A[G] For tagless final algebras whose effect F appears only in the contravariant position, instance of ContravariantK can be auto generated through the autoContravariantK annotation. InvariantK def imapK[F[_], G[_]](af: A[F])(fk: F ~&gt; G)(gK: G ~&gt; F): A[G] For tagless final algebras whose effect F appears in both the covariant positions and contravariant positions, instance of InvariantK can be auto generated through the autoInvariantK annotation. SemigroupalK def productK[F[_], G[_]](af: A[F], ag: A[G]): A[Tuple2K[F, G, *]] For tagless final algebras that has no extra type parameters or abstract type members, and whose effect F appears only in the covariant position for all members, instance of SemigroupalK can be auto generated through autoSemigroupalK annotation. ApplyK def map2K[F[_], G[_], H[_]](af: A[F], ag: A[G])(f: Tuple2K[F, G, *] ~&gt; H): A[H] ApplyK extends both SemigroupalK and FunctorK just like their lower kinded counterparts. For tagless final algebras that has no extra type parameters or abstract type members, and whose effect F appears only in the covariant position for all members, instance of ApplyK can be auto generated through autoApplyK annotation. Their laws are defined in cats.tagless.laws. To test your instance (if you decide to roll your own) against these laws please follow the examples in cats.tagless.tests, especially the ones that test against SafeAlg."
    }    
  ];

  idx = lunr(function () {
    this.ref("title");
    this.field("content");

    docs.forEach(function (doc) {
      this.add(doc);
    }, this);
  });

  docs.forEach(function (doc) {
    docMap.set(doc.title, doc.url);
  });
}

// The onkeypress handler for search functionality
function searchOnKeyDown(e) {
  const keyCode = e.keyCode;
  const parent = e.target.parentElement;
  const isSearchBar = e.target.id === "search-bar";
  const isSearchResult = parent ? parent.id.startsWith("result-") : false;
  const isSearchBarOrResult = isSearchBar || isSearchResult;

  if (keyCode === 40 && isSearchBarOrResult) {
    // On 'down', try to navigate down the search results
    e.preventDefault();
    e.stopPropagation();
    selectDown(e);
  } else if (keyCode === 38 && isSearchBarOrResult) {
    // On 'up', try to navigate up the search results
    e.preventDefault();
    e.stopPropagation();
    selectUp(e);
  } else if (keyCode === 27 && isSearchBarOrResult) {
    // On 'ESC', close the search dropdown
    e.preventDefault();
    e.stopPropagation();
    closeDropdownSearch(e);
  }
}

// Search is only done on key-up so that the search terms are properly propagated
function searchOnKeyUp(e) {
  // Filter out up, down, esc keys
  const keyCode = e.keyCode;
  const cannotBe = [40, 38, 27];
  const isSearchBar = e.target.id === "search-bar";
  const keyIsNotWrong = !cannotBe.includes(keyCode);
  if (isSearchBar && keyIsNotWrong) {
    // Try to run a search
    runSearch(e);
  }
}

// Move the cursor up the search list
function selectUp(e) {
  if (e.target.parentElement.id.startsWith("result-")) {
    const index = parseInt(e.target.parentElement.id.substring(7));
    if (!isNaN(index) && (index > 0)) {
      const nextIndexStr = "result-" + (index - 1);
      const querySel = "li[id$='" + nextIndexStr + "'";
      const nextResult = document.querySelector(querySel);
      if (nextResult) {
        nextResult.firstChild.focus();
      }
    }
  }
}

// Move the cursor down the search list
function selectDown(e) {
  if (e.target.id === "search-bar") {
    const firstResult = document.querySelector("li[id$='result-0']");
    if (firstResult) {
      firstResult.firstChild.focus();
    }
  } else if (e.target.parentElement.id.startsWith("result-")) {
    const index = parseInt(e.target.parentElement.id.substring(7));
    if (!isNaN(index)) {
      const nextIndexStr = "result-" + (index + 1);
      const querySel = "li[id$='" + nextIndexStr + "'";
      const nextResult = document.querySelector(querySel);
      if (nextResult) {
        nextResult.firstChild.focus();
      }
    }
  }
}

// Search for whatever the user has typed so far
function runSearch(e) {
  if (e.target.value === "") {
    // On empty string, remove all search results
    // Otherwise this may show all results as everything is a "match"
    applySearchResults([]);
  } else {
    const tokens = e.target.value.split(" ");
    const moddedTokens = tokens.map(function (token) {
      // "*" + token + "*"
      return token;
    })
    const searchTerm = moddedTokens.join(" ");
    const searchResults = idx.search(searchTerm);
    const mapResults = searchResults.map(function (result) {
      const resultUrl = docMap.get(result.ref);
      return { name: result.ref, url: resultUrl };
    })

    applySearchResults(mapResults);
  }

}

// After a search, modify the search dropdown to contain the search results
function applySearchResults(results) {
  const dropdown = document.querySelector("div[id$='search-dropdown'] > .dropdown-content.show");
  if (dropdown) {
    //Remove each child
    while (dropdown.firstChild) {
      dropdown.removeChild(dropdown.firstChild);
    }

    //Add each result as an element in the list
    results.forEach(function (result, i) {
      const elem = document.createElement("li");
      elem.setAttribute("class", "dropdown-item");
      elem.setAttribute("id", "result-" + i);

      const elemLink = document.createElement("a");
      elemLink.setAttribute("title", result.name);
      elemLink.setAttribute("href", result.url);
      elemLink.setAttribute("class", "dropdown-item-link");

      const elemLinkText = document.createElement("span");
      elemLinkText.setAttribute("class", "dropdown-item-link-text");
      elemLinkText.innerHTML = result.name;

      elemLink.appendChild(elemLinkText);
      elem.appendChild(elemLink);
      dropdown.appendChild(elem);
    });
  }
}

// Close the dropdown if the user clicks (only) outside of it
function closeDropdownSearch(e) {
  // Check if where we're clicking is the search dropdown
  if (e.target.id !== "search-bar") {
    const dropdown = document.querySelector("div[id$='search-dropdown'] > .dropdown-content.show");
    if (dropdown) {
      dropdown.classList.remove("show");
      document.documentElement.removeEventListener("click", closeDropdownSearch);
    }
  }
}
