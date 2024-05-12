## Contribution Guidelines

When submitting a pull request, you are granting [JellySquid](https://jellysquid.me) the right to license
your contributions under the [Polyform Shield License (Version 1.0.0)](LICENSE.md).

If you have any questions about these terms, please [get in contact with us](https://caffeinemc.net/discord).

### Code Style

When contributing source code to the project, ensure that you make consistent use of our code style guidelines. These
guidelines are based on the [Google code style guidelines for Java](https://google.github.io/styleguide/javaguide.html),
with some minor changes, as described below.

- Use 4 spaces for indentation, not tabs. Avoid lines which exceed 120 characters.
- Avoid deeply nested conditional logic, and prefer breaking out to separate functions when possible.
    - If you are using more than three levels of indentation, you should likely consider restructuring your code.
    - Branches which are only exceptionally or very rarely taken should remain concise. When this is not possible,
      prefer breaking out to a new method (where it makes sense) as this helps the compiler better optimize the code.
- Use `this` to qualify method and field access, as it avoids some ambiguity in certain contexts.

We also provide these code styles as [EditorConfig](https://editorconfig.org/) files, which most Java IDEs will
automatically detect and make use of.

#### Unsafe code

There are some additional guidelines for using unsafe code in Java, which are aimed at improving code readability and
debugging. Most developers will not need to read this section unless they are working with bindings to native libraries.

- The use of unsafe operations in general should be avoided unless strictly necessary, or where there is otherwise a
  clear performance advantage.
    - Unsafe code is often _slower_ than safe code, since the Hotspot compiler only contains basic intrinsics and is
      very pessimistic about optimizing such code.
    - Array bounds checks are almost never the bottleneck of your code. But memory layout often is, and unsafe code
      gives you explicit control over it.
- The invariants of public methods which expose unsafe operations MUST be documented.
    - An example of a safety invariant would be if passing a null pointer value or invalid array index to a method which
      uses unsafe operations would cause an invalid memory access.
    - The caller of your method should know exactly what is needed to avoid undefined or invalid behavior. 
- The invariants of unsafe methods SHOULD be checked where possible. The use of runtime checking SHOULD be configurable
  at application launch.
    - The typical way to implement these checks is to keep a `private static final boolean CHECKS = /* ... */;` field in
      the class which controls the use of runtime checking.
    - These checks can then be wrapped within an if-statement which allows them to be skipped if runtime error checking
      is disabled. This also allows the compiler to easily optimize them away.
- When plain integer types are used to store pointers to memory addresses, their variables MUST be prefixed, and a 
  comment MUST be added to indicate their underlying type.
    - For example, the declaration `int* value;` in C code would be represented as `long pValue; // int*` in Java code.
    - For chains of pointers, multiple prefixes should be used. For example, `char** strings;` in C code would become
      `long ppStrings; // char**` in Java code.

### Pull Requests

Your pull request should include a brief description of the changes it makes and link to any open issues which it
resolves. You should also ensure that your code is well documented where it is non-trivial, and that it follows our code
style guidelines.

If you're adding new Mixin patches to the project, please ensure that you have created appropriate entries to disable
them in the config file. Mixins should always be self-contained and grouped into "patch sets" which are easy to isolate,
and where that is not possible, they should be placed into the "core" package.