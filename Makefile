.PHONY: clean
clean: 
	mvn clean

.PHONY: build
build:
	cd clients/go/cmd/zbctl && ./build.sh && mvn clean install -T1C -DskipTests -pl dist -am

TAG=SNAPSHOT-$(date +%Y-%m-%d)-$(git rev-parse --short=8 HEAD)
.PHONY: docker-build
docker-build:
	docker build --build-arg DISTBALL=dist/target/zeebe-distribution-*.tar.gz -t gcr.io/zeebe-io/zeebe:$(TAG) .

.PHONY: docker-push
docker-push:
	docker push gcr.io/zeebe-io/zeebe:$(TAG)
