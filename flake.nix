{
  description = "Spring+React Codebase";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05";

  outputs = { nixpkgs, ... }:
    let
      systems = [ "x86_64-linux" ];
      forAllSystems = f: nixpkgs.lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});
    in {
      devShells = forAllSystems (pkgs: {
        default = pkgs.mkShellNoCC {
          buildInputs = with pkgs; [
            # git
            gradle_9
            jdk25
            nodejs-slim_24
            (pnpm_12.override {
              version = "12.8.2";
              srcHash = lib.fakeHash;
              cargoHash = lib.fakeHash;
            })
            postgresql_18
            (devcontainer.override { nodejs = nodejs-slim_24; })
          ];

          shellHook = ''
            export PS1="\n\[\033[1;32m\][devshell:\w]\$\[\033[0m\] "
          '';

          JAVA_HOME = "${pkgs.jdk25}";
        };
      });
    };
}

