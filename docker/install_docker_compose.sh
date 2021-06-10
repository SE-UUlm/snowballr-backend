#!/bin/bash

### Uninstall current installation of docker-compose if existing
### This is used to install the newest version since most os repos use pretty old releases
### Reload shell afterwards so that the new path can be read

VERSION=$(curl --silent https://api.github.com/repos/docker/compose/releases/latest | grep -Po '"tag_name": "\K.*\d')
DESTINATION=/usr/local/bin/docker-compose
sudo curl -L https://github.com/docker/compose/releases/download/${VERSION}/docker-compose-$(uname -s)-$(uname -m) -o $DESTINATION
sudo chmod 755 $DESTINATION
