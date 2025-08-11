{
  inputs = {
    self.submodules = true;
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
    build-gradle-application.url = "github:raphiz/buildGradleApplication";
    treefmt-nix.url = "github:numtide/treefmt-nix";
    git-hooks-nix.url = "github:cachix/git-hooks.nix";
  };

  outputs =
    inputs:
    inputs.flake-parts.lib.mkFlake { inherit inputs; } {
      systems = inputs.nixpkgs.lib.systems.flakeExposed;

      imports = with inputs; [
        treefmt-nix.flakeModule
        git-hooks-nix.flakeModule
      ];

      perSystem =
        {
          config,
          system,
          pkgs,
          ...
        }:
        let
          detekt-formatting = pkgs.fetchurl {
            url = "https://github.com/detekt/detekt/releases/download/v1.23.8/detekt-formatting-1.23.8.jar";
            hash = "sha256-oYo/aA0jKtdGzOw9WiUXds45Exs5XEVbkuG7Cp6Ptro=";
          };
        in
        {
          _module.args.pkgs = import inputs.nixpkgs {
            inherit system;
            overlays = [
              inputs.build-gradle-application.overlays.default
            ];
            config = { };
          };

          packages = rec {
            default = snowballr-backend;
            snowballr-backend =
              (pkgs.buildGradleApplication {
                pname = "snowballr-backend";
                version = "0.1.0";
                src = ./.;
                meta.description = "The official backend for the SnowballR application";
                gradle = pkgs.gradleFromWrapper {
                  wrapperPropertiesPath = ./gradle/wrapper/gradle-wrapper.properties;
                };
                nativeBuildInputs = with pkgs; [
                  protoc-gen-grpc-java
                  protobuf
                ];
              }).overrideAttrs
                {
                  preBuild = ''
                    sed -i "s|artifact = \"com\.google\.protobuf.*\"|path = \"${pkgs.protobuf}/bin/protoc\"|" build.gradle.kts
                    sed -i "s|artifact = \"io\.grpc\:protoc-gen-grpc-java.*\"|path = \"${pkgs.protoc-gen-grpc-java}/bin/protoc-gen-grpc-java\"|" build.gradle.kts
                  '';
                };
          };

          checks.detekt = pkgs.runCommand "detekt" { } ''
            ${pkgs.detekt}/bin/detekt -b ${./detekt-baseline.xml} -c ${./detekt.yml} -p ${detekt-formatting} --parallel -ac -i ${./src}
            touch $out
          '';

          devShells.default = pkgs.mkShell {
            shellHook = ''
              ${config.pre-commit.installationScript}
            '';
            packages = with pkgs; [
              kotlin-language-server
              gradle
              openjdk
            ];
          };

          pre-commit.settings.hooks.treefmt.enable = true;

          treefmt = {
            projectRootFile = "flake.nix";
            programs = {
              nixfmt.enable = true;
              taplo.enable = true;
            };
            settings = {
              formatter.detekt = {
                command = pkgs.writeShellScriptBin "detektfmt" ''
                  IFS=,
                  inputs=$(printf "%s" "$*")
                  unset IFS
                  ${pkgs.detekt}/bin/detekt -b ${./detekt-baseline.xml} -c ${./detekt.yml} -p ${detekt-formatting} --parallel -ac -i "$inputs" || true
                '';
                includes = [ "*.kt" ];
              };
              global.excludes = [ "api/*" ];
            };
          };
        };
    };
}
