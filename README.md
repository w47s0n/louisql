# ScalaSQL
```scala
     ░██                          ░██                          ░██ 
     ░██                                                       ░██ 
     ░██   ░███████   ░██    ░██  ░██   ░███████    ░████████  ░██ 
     ░██  ░██    ░██  ░██    ░██  ░██  ░██         ░██    ░██  ░██ 
     ░██  ░██    ░██  ░██    ░██  ░██   ░███████   ░██    ░██  ░██ 
     ░██  ░██    ░██  ░██   ░███  ░██         ░██  ░██   ░███  ░██ 
     ░██   ░███████    ░█████░██  ░██   ░███████    ░█████░██  ░██ 
                                                          ░██      
                                                          ░██      
```


Interactive database client built with [layoutz](https://github.com/mattlianje/layoutz) - a library for creating beautiful terminal UIs in Scala.

## Project Structure

```
scalasql/
├── core/           # Pure ELM logic: State, Msg, Update, Effect (no UI, no I/O)
│   └── src/com/w47s0n/core/
│       ├── state/      # State, connections form, Workspace tree, Editor
│       ├── message/    # Msg
│       ├── update/     # Update, WorkspaceUpdate (returns Effects, never runs them)
│       ├── effect/     # Effect, QueryResult
│       └── service/    # ConnectionStore (saved-connections file format)
│
├── app/            # layoutz TUI + the effects runner
│   └── src/com/w47s0n/app/
│       ├── Effects.scala   # runs core Effects as layoutz Cmds
│       ├── db/MySql.scala  # MySQL via ldbc (pure Scala, JVM + Native)
│       └── tui/            # views, key mapping, Main
│
└── native            # same sources, built with Scala Native
```

The **core** module never touches the network or disk: `Update` returns `Effect`s, the app runs them and answers with `Msg`s.

## Install

macOS (Apple Silicon) and Linux (x86_64, arm64):

```bash
curl -fsSL https://louisql.w47s0n.com/install.sh | bash
```

Installs `louisql` to `~/.local/bin`; set `LOUISQL_INSTALL_DIR` to change that. `louisql update` installs the latest release in place, `louisql --version` shows the installed one. Binaries come from GitHub Releases, built by `.github/workflows/release.yml` on every `v*` tag.

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

Milestone 1: a DataGrip-style explorer and query console for **MySQL**.

- **Connections** saved to `~/.config/scalasql/connections.tsv` without passwords; the password is asked once per session
- **Explorer tree** loaded lazily: connection → schemas → tables / views → columns (type, PK)
- **Query console**: multi-line editor; `^R` runs the `;`-separated statement under the cursor
- **Result grid**: up to 1000 rows, NULLs dimmed, row/column scrolling, errors and affected-row counts shown in place

### Keys

| Where | Keys |
|---|---|
| Connections | `↑↓` choose · `⏎`/`c` connect · `n` new · `e` edit · `d` delete · `q` quit |
| Form | `Tab`/`⏎` next field · `↑↓` engine · `^T` test · `^S` save · `^O` save and connect · `Esc` cancel |
| Workspace | `Tab` next pane · `^R` run · `Esc` back to connections |
| Explorer | `↑↓`/`jk` move · `→`/`l` expand · `←`/`h` collapse · `⏎` on a table = first 100 rows · `r` refresh |
| Console | type · arrows · Home/End · `⏎` new line |
| Result | `↑↓` rows · PgUp/PgDn page · `←→` columns |

### Try it against a throwaway MySQL

```bash
docker run -d --name sq-mysql -p 13306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8.4
# in the app: n → host 127.0.0.1, port 13306, user root → ^T → ^S → ⏎

# integration tests against it
SCALASQL_MYSQL=root:root@127.0.0.1:13306 mill app.test
```

### Native binary

```bash
mill native.nativeLink && ./out/native/nativeLink.dest/out
```

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
