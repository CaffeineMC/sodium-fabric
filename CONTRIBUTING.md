## Issues

When opening issues, please be sure to include the following information as applicable.

- The exact version of the mod you are running, such as `0.1.0-fabric`, and the version of Fabric/Forge you are using.
- If your issue is a crash, attach the latest client or server log and the complete crash report as a file. You can
attach these as a file (preferred) or host them on a service such as [GitHub Gist](https://gist.github.com/) or [Hastebin](https://hastebin.com/).
- If your issue is a bug or otherwise unexpected behavior, explain what you expected to happen.
- If your issue only occurs with other mods installed, be sure to specify the names and versions of those mods.

## Pull Requests

It's super awesome to hear you're wishing to contribute to the project! Before you open a pull request, you'll need to
give a quick read to the following guidelines.

### Contributor License Agreement (CLA)

By submitting changes to this repository, you are hereby agreeing that:

- Your contributions will be licensed irrecoverably under the [GNU LGPLv3](https://www.gnu.org/licenses/lgpl-3.0.html).
- Your contributions are of your own work and free of legal restrictions (such as patents and copyrights) or other
issues which would pose issues for inclusion or distribution under the above license.

If you do not agree to these terms, please do not submit contributions to this repository. If you have any questions
about these terms, feel free to get in contact with me through the [public Discord server](https://jellysquid.me/discord) or
through opening an issue.

### Code Style

When contributing source code changes to the project, ensure that you make consistent use of the code style guidelines
used throughout the codebase (which follow pretty closely after the standard Java code style guidelines). These guidelines
have also been packaged as EditorConfig and IDEA inspection profiles which can be found in the repository root and `idea`
directory respectively.

- Use 4 spaces for indentation, not tabs. Avoid lines which exceed 120 characters.
- Use `this` to qualify member and field access.
- Always use braces when writing if-statements and loops.
- Annotate overriding methods with `@Override` so that breaking changes when updating will create hard compile errors.
- Comment code which needs to mimic vanilla behavior with `[VanillaCopy]` so it can be inspected when updating.

### Making a Pull Request

Your pull request should include a brief description of the changes it makes and link to any open issues which it
resolves. You should also ensure that your code is well documented where non-trivial and that it follows the
outlined code style guidelines above.

If you're adding new Mixin patches to the project, please ensure that you have created appropriate entries to disable
them in the config file. Mixins should always be self-contained and grouped into "patch sets" which are easy to isolate.

Additionally, if you're making changes for the sake of improving performance in either the vanilla game or the project
itself, try to provide a detailed test-case and benchmark for them. It's understandable that micro-benchmarking is
difficult in the context of Minecraft, but even naive figures taken from a profiler, timings graph, or a simple counter
will be greatly appreciated and help track incremental improvements. 