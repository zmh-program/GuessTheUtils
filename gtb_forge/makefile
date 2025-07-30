.PHONY: build run server

build:
	gradlew build

run:
	gradlew runClient

server:
	gradlew :runServer

deploy:
	powershell -Command "Remove-Item 'D:\launchers\hmcl\.minecraft\mods\guesstheutils*.jar' -Force -ErrorAction SilentlyContinue"
	powershell -Command "Copy-Item 'build\libs\guesstheutils-*.jar' -Destination 'D:\launchers\hmcl\.minecraft\mods\' -Exclude '*sources*'"
