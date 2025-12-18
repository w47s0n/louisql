# ScalaSQL
```scala
                                     ░██                                  ░██ 
                                     ░██                                  ░██ 
     ░███████   ░███████   ░██████   ░██  ░██████    ░███████   ░████████ ░██ 
    ░██        ░██    ░██       ░██  ░██       ░██  ░██        ░██    ░██ ░██ 
     ░███████  ░██         ░███████  ░██  ░███████   ░███████  ░██    ░██ ░██ 
           ░██ ░██    ░██ ░██   ░██  ░██ ░██   ░██         ░██ ░██   ░███ ░██ 
     ░███████   ░███████   ░█████░██ ░██  ░█████░██  ░███████   ░█████░██ ░██ 
                                                                      ░██     
                                                                      ░██     
```


Interactive database client built with [layoutz](https://github.com/mattlianje/layoutz) - a library for creating beautiful terminal UIs in Scala.

## Project Structure

```
scalasql/
├── core/           # Pure ELM business logic (no UI dependencies)
│   └── src/
│       ├── Model.scala         # DbState, DbMsg
│       ├── Update.scala        # Pure update functions
│       └── DataGenerator.scala # Business logic
│
└── app/            # Presentation layer (layoutz TUI)
    └── src/
        ├── DatabaseClientApp.scala  # Layoutz views
        └── Main.scala               # Entry point
```

The **core** module contains framework-agnostic ELM logic that can be shared across different presentation layers (layoutz TUI, Tyrian+Electron, etc.).

## Running the App

### Quick Start

```bash
./run-tui.sh
```

### Using Nix (Recommended)

This project uses Nix flakes for reproducible development environments with Mill 1.0.6 and JDK 17.

```bash
# Enter development shell
nix develop

# Run the app
mill -i --disable-ticker app.runLocal

# Run tests
mill __.test

# Compile all modules
mill __.compile
```

### Manual Run

For interactive terminal applications with keyboard input, use `runLocal` instead of `run`:

```bash
mill -i --disable-ticker app.runLocal
```

**Flags explained:**
- `-i` - Interactive mode (enables stdin forwarding)
- `--disable-ticker` - Disables Mill's progress output for clean terminal UI
- `runLocal` - Runs in-process (required for proper terminal raw mode)

**Note:** Using `.run` instead of `.runLocal` will fork a subprocess that may not properly inherit terminal settings, causing keyboard input to require pressing Enter.

## Features

- **Interactive Query Input** - Type SQL queries with live cursor feedback
- **Random Data Generation** - Returns mock table data for any query
- **Beautiful Table Rendering** - Built-in layoutz table formatting
- **Keyboard Controls** - Enter to execute, Backspace to delete

## Build Commands

```bash
# Compile
mill app.compile

# Run (interactive with keyboard input)
mill -i --disable-ticker app.runLocal
```

## IDE Setup (Metals)

Mill 1.0.x removed Bloop support and uses native BSP instead. To set up IDE integration:

```bash
./mill --bsp-install
```

**Known Issue:** Mill 1.0.x BSP has a bug where `bspClean` fails with "Writing to out/mill-bsp-out not allowed". As a workaround, compile from terminal and use Metals only for navigation/diagnostics:

```bash
# Compile from terminal (avoids BSP bug)
mill __.compile

# Use Metals for: go-to-definition, find references, diagnostics
# Avoid: triggering compile from Metals directly
```
