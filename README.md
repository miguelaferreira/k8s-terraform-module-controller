# k8s-terraform-controller
K8s controller to execute terraform modules

## Setup

JDK 11 (stopper for JDK 14 is Jib).
```bash
sdk install java 11.0.6.j9-adpt
sdk use java 11.0.6.j9-adpt
sdk install micronaut
```

Create micronaut application
```bash
mn create-app -i -p service -b gradle -l java -f kubernetes,jib
gradle wrapper --gradle-version 6.3 --distribution-type all
git add -all
git commit -m "Create micronaut app"
```

Add rbac config to `k8s.yaml`.

Configure docker container repository for jib (build and push) and for k8s in `k8s.yaml` (pull).
If needed, create a docker registry secret in the cluster and update `k8s.yaml`.
For github packages, on a new repository, push any image to the repository to have it initialized.

Build and push to docker registry.
```bash
./gradlew clean build jib
```


Deploy.
```bash
kubectl apply -g k8s.yaml
```
