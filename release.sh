#!/bin/bash

version=$(grep -E 'mod_version(\s*)=(\s*)' "./gradle.properties" | cut -d'=' -f2 | tr -d ' ')

git tag -s -a v"$version" -m "$1"

git push origin main v"$version"