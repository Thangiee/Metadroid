0.1.1 (2016/08/07)
------------------
* Prepend namespace to key to avoid collision.
* `@Case` macro will works when a companion object is already defined.
* Support repeated class parameter i.g. `@Case Foo(bars: Int*) ...`
* Enable class parameters wrapped in `Option` to catch NPE and return `None`  