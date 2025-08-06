Do not name files Advanced or enhanced. Upgrade the existing codebase when you have to generate new code and only create a new Java class when it is nessasry. If a Java class already exists, upgrade it.
Confirm with the user before checking something off the todo list.
Work from the todo list creating full production code, no stub for mock functionality.
Test the compile and excute before ending a task.
Use industry-standard package naming (all-lowercase, reverse-DNS) and keep each class in a single public file.
Follow Google Java Style for braces, spacing, imports, and 100-column line length.
Add Javadoc to every public type, method, and field you touch or create.
Never delete an existing method without first deprecating it for one full milestone release.
Inject new dependencies through constructors rather than using global singletons.
Prefer immutable data objects; if mutability is required, synchronize or use java.util.concurrent primitives.
Write at least one failing unit test before fixing or adding a feature, and commit the passing test with the code change.
Maintain ≥ 90 % line-coverage in the core module; block compilation if coverage drops below the threshold.
Log meaningful messages at INFO and errors at ERROR; never print directly to System.out or System.err.
Use try-with-resources for every I/O stream and closeable object to avoid leaks.
Guard all public entry points with null-checks and validate external data before use.
Avoid hard-coded constants; place configurable values in *.properties files loaded via the resource manager.
Run static-analysis (SpotBugs, PMD, Checkstyle) and refuse to compile if any new high-severity issues appear.
Group commits by logical task; each commit message must reference the exact todo-list item ID it addresses.
Never merge generated code directly into main without first opening a pull request and waiting for human approval.
When upgrading third-party libraries, record the version bump and migration notes in CHANGELOG.md.
Profile new rendering or physics code with flight-recorder settings and ensure no single frame exceeds the 16 ms budget.
Raise a confirmation prompt before introducing a breaking API change or refactoring a widely-used package.
Document every new configuration key in docs/configuration_reference.md as soon as it is added.
Default to non-blocking, asynchronous patterns for any file, network, or chunk-generation task that might exceed 2 ms on the main thread.
The main class is actually com.odyssey.OdysseyGame.
mvn exec:java "-Dexec.mainClass=com.odyssey.OdysseyGame"  is the correct command to run the game.
The game should run on Windows, macOS, and Linux.
The game should be able to run on a potato but with AAA graphics, the graphics are the most important part of this game.


