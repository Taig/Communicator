#!/bin/bash

# Docker entrypoint with open sbt shell and phoenix_echo running in background
#
# docker run --entrypoint="./develop.sh" -v "$PWD:/communicator/" --rm -it taig/communicator:latest

sbt