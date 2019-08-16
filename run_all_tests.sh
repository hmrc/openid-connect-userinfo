#!/usr/bin/env bash

set -e

sbt clean coverage test it:test coverageOff coverageReport