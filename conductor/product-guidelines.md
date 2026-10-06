# Product Guidelines: ScalaSQL

These guidelines establish the standards and conventions for the ScalaSQL project, ensuring alignment with the core principles of high code quality, functional purity, and maintainability.

## 1. Code Style and Formatting

- **Automated Formatting with Scalafmt:** All Scala source code will be automatically formatted using Scalafmt. The project's `.scalafmt.conf` file is the single source of truth for code style. Developers must ensure their IDEs and build processes integrate with Scalafmt to maintain consistent formatting. Manual formatting adjustments are to be avoided; if a style change is desired, it should be implemented via the Scalafmt configuration.

## 2. Effect Management and Error Handling

- **Explicit Effect Management with Effect Monads:** All side-effecting computations, including I/O, concurrency, and asynchronicity, must be encapsulated and managed using type-safe effect monads (e.g., from Cats Effect or ZIO). This ensures referential transparency and makes the presence of effects explicit in the type signatures, adhering strictly to the principle of functional purity.
- **Immutability and Pure Functions:** All functions should strive to be pure, meaning they return the same output for the same input and produce no observable side effects. Data structures should be immutable by default. Any deviation from this principle must be thoroughly justified and documented.

## 3. Documentation and Comments

- **"Why" over "What" Comments:** Code comments should be used sparingly and strategically. Their primary purpose is to explain the rationale behind complex decisions, design choices, or non-obvious logic ("why" a piece of code exists or is structured a certain way), rather than simply re-stating what the code does ("what" it does). The code itself should be as self-explanatory and readable as possible.
- **Meaningful Naming:** Variables, functions, classes, and modules should be named clearly and descriptively, reflecting their purpose and behavior to enhance code readability and reduce the need for excessive comments.
