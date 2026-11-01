# louisql
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

A terminal database client for MySQL: browse schemas, run queries, scroll results.

## Install

macOS (Apple Silicon) and Linux:

```bash
curl -fsSL https://louisql.w47s0n.com/install.sh | bash
```

```bash
louisql            # start
louisql update     # update to the latest version
louisql --version
```

## Keys

| Where | Keys |
|---|---|
| Connections | `↑↓` choose · `⏎` connect · `n` new · `e` edit · `d` delete · `q` quit |
| Form | `Tab` next field · `^T` test · `^S` save · `^O` save and connect · `Esc` cancel |
| Workspace | `Tab` next pane · `^R` run statement under cursor · `Esc` back |
| Explorer | `↑↓` move · `→←` expand/collapse · `⏎` on a table = first 100 rows · `r` refresh |
| Result | `↑↓` rows · PgUp/PgDn page · `←→` columns |

Connections are saved to `~/.config/scalasql/connections.tsv` without passwords; you're asked once per session.

## Development

```bash
nix develop
mill -i --disable-ticker app.runLocal   # run
mill __.test                            # test
```
