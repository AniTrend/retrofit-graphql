#!/bin/bash

function create_directories {
    mkdir -p ./app/.config/
    cd ./app/.config/
}

function create_files {
    touch secrets.properties
    echo "token=\"token\"" >> secrets.properties
}


create_directories
create_files

