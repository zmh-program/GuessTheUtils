.PHONY: build run server stop apply deploy

build:
	gradlew build

run:
	gradlew :runClient

stop:
	gradlew --stop

server:
	gradlew :runServer

apply:
	powershell -Command "Remove-Item 'D:\launchers\hmcl\.minecraft\mods\guesstheutils*.jar' -Force -ErrorAction SilentlyContinue"
	powershell -Command "Copy-Item 'build\libs\guesstheutils-*.jar' -Destination 'D:\launchers\hmcl\.minecraft\mods\' -Exclude '*sources*'"

deploy: build apply
