{ pkgs ? import <nixpkgs> {} }:
pkgs.mkShell {
  buildInputs = with pkgs; [
    babashka
    clojure
    docker
    nodejs
    openjdk
    yarn
  ];
}
