# ==============================================================================
# DaperkzRTP - Java Minecraft Plugin
# Copyright (c) 2026 Daperkz
#
# Makefile
# ==============================================================================

PROJECT_NAME = RTP

all: build

build:
	mvn clean package
	@echo "The file .jar was generated in the /target directory"

clean:
	mvn clean
	@echo "clean project."

re: clean build

.PHONY: build clean re
