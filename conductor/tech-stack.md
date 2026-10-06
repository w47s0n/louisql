# Technology Stack: ScalaSQL

This document outlines the core technologies and tools utilized in the ScalaSQL project.

## 1. Programming Languages

-   **Scala:** The primary programming language for all application logic, leveraging the latest version (Scala 3) for its advanced features, improved type inference, and enhanced functional programming capabilities.

## 2. Build Tools & Dependencies

-   **Mill:** The chosen build tool for ScalaSQL, providing a powerful, flexible, and fast build experience.
-   **JDK 17:** The Java Development Kit version 17 serves as the runtime environment for the Scala application.
-   **Nix:** Used for managing development environments and dependencies, ensuring reproducibility across different machines.

## 3. User Interface Frameworks

-   **layoutz:** Employed for developing the interactive Terminal User Interface (TUI), enabling a lightweight and keyboard-driven user experience.
-   **Tyrian:** A functional reactive programming library for building web and desktop applications in Scala, used in conjunction with Electron to deliver the desktop application variant.
-   **Electron:** A framework for building cross-platform desktop applications with web technologies, used to package the Tyrian-based desktop frontend.

## 4. Version Control

-   **Git:** The distributed version control system used for managing source code, tracking changes, and facilitating collaborative development.
