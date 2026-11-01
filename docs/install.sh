#!/bin/sh
# Install louisql: curl -fsSL https://louisql.w47s0n.com/install.sh | bash
set -eu

REPO="w47s0n/louisql"
BIN="louisql"

main() {
  case "$(uname -s)" in
    Darwin) os=darwin ;;
    Linux) os=linux ;;
    *) echo "Unsupported OS: $(uname -s)" >&2; exit 1 ;;
  esac
  case "$(uname -m)" in
    arm64 | aarch64) arch=arm64 ;;
    x86_64 | amd64) arch=x86_64 ;;
    *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;;
  esac
  if [ "$os-$arch" = darwin-x86_64 ]; then
    echo "Intel Macs are not supported, only Apple Silicon" >&2; exit 1
  fi

  dir="${LOUISQL_INSTALL_DIR:-$HOME/.local/bin}"
  url="https://github.com/$REPO/releases/latest/download/$BIN-$os-$arch"
  tmp="$(mktemp)"
  trap 'rm -f "$tmp"' EXIT

  echo "Downloading $url"
  curl -fSL --progress-bar -o "$tmp" "$url"
  chmod +x "$tmp"
  mkdir -p "$dir"
  mv "$tmp" "$dir/$BIN"
  echo "Installed $BIN to $dir/$BIN"

  case ":$PATH:" in
    *":$dir:"*) ;;
    *) echo "Add it to your PATH:  export PATH=\"$dir:\$PATH\"" ;;
  esac
}

main "$@"
