{
  description = "Spring+React Codebase";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";

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
            (nodejs_24.override { enableNpm = false; })
            (pnpm_10.override { withNode = false; })
            postgresql_18
            devcontainer
          ];

          shellHook = ''
            export PS1="\n\[\033[1;32m\][devshell:\w]\$\[\033[0m\] "
          '';

          JAVA_HOME = "${pkgs.jdk25}";
        };
      });
    };
}

