{
  description = "ScalaSQL - Interactive database client built with layoutz";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            mill
            jdk17
          ];

          shellHook = ''
            echo "ScalaSQL development environment"
            echo "Mill version: $(mill --version)"
            echo "Java version: $(java -version 2>&1 | head -n 1)"
          '';
        };
      }
    );
}
