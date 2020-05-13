# Design

At the highest level the controller is a simple function that takes some input and produces some output.
Before inputs can be processed, the controller requires a baseline configuration or setup in order to offer a terraform remote execution service.
Generic things like compute, storage and network need to be provider.
But also terraform specific things like a backend where to keep track of the different states that are created by executing different modules.
An input to the controller is a description of the module that needs to be provisioned, and the variables that should be passed on to the module execution.
Credentials for the different terraform providers could be another addition to the inputs, however for simplicity the controller assumes that the user who requests the module execution has declared all required terraform providers and wired in credentials via terraform variables.
Tha means credentials for terraform providers are not injected in the environment of the terraform execution, and have to rather be specified as variables for the terraform module.
An output of the controller is the collection of outputs that the executed module produces.

Although it might seem that looking at this k8s controller simply as a function is a oversimplification of what it does, we beg to differ.
In fact, given the operational properties of terraform, terraform swallows almost all the concerns with managing state.
It manages new states of infrastructure compares to old states of infrastructure, and it also manages states of infrastructure as the codebase that generated it changes.
The controller's main concern with respect to state is mostly delegated to the terraform backend where module execution states are stored.
This means that the controller does not really need to keep any state. After a restart it can simply read what inputs were there already, re-run terraform over them and rebuilt it's own view of the overall state.

##  MVP1

In this initial implementation the controller takes its inputs from secrets on a given namespace and with a configurable label.
Terraform modules need to be packages in docker containers that are accessible by the controller (it supports private registries).
Once the controller is notified of a new input it copies the input to it's own secret (for safe keeping in case the input needs to be destroyed later), next it creates a pod where it injects the terraform module container, a execution container, a volume to bridge the two containers as well as several secrets with module input variables, terraform backend credentials and whatever else is needed for the execution.  
At the end of the execution the last command reads the terraform outputs defined in the module and pushes that to a new output secret.
Users of the controller can bind other pods to the contents of the output secret and k8s will take care of starting those pods when the secret is available.

To use the controller it needs to be installed and configured. Section [Installation](#installation) describes steps to create a terraform backend on AWS, and to install the controller on a k8s cluster.

### Building the controller

The build of the controller docker container can be done directly with gradle.
```bash
./gradlew clean build jib -x test
```
The flag `-x test` disabled the execution of tests during the build.
Tests included in the controller project require a proxy connection to a kubernetes cluster.
To execute the tests remove the `-x test` flag and open a proxy redirection to a cluster via kubectl.
```bash
kubect proxy &
./gradlew clean build jib
```

### Installation and configuration

The controller needs a TF backend to store the states of the provisioned resources.
The required configuration and credentials have to be specified in the application configuration (ie. application.yaml).
The only type of backend currently supported is S3 (S3 bucket + DynamoDB table + KMS key?).
To access the TF backend the controller needs to have permissions over the backend resources.
Permissions can be provided via a secret with AWS keys in the required format.

Here's a snippet of the controller configuration to define a terraform backend.
```yaml
terraform_backend:
  s3:
    bucket: "k8s-terraform-controller"
    dynamo_db_table: "k8s-terraform-controller"
    credentials_secret_name: "terraform-backend-credentials"
```
The credentials for the backend need to be available in a secret in the same namespace as the controller.
To create the secret do:
```bash
kubectl create secret generic terraform-backend-credentials \
    --from-literal=TF_BACKEND_AWS_ACCESS_KEY_ID='zzzzzzzzzzzzzzzzzzzz' \
    --from-literal=TF_BACKEND_AWS_SECRET_ACCESS_KEY='xxxxxxxxxxxxxxxxxxxxxxxxx'
```
Note that the content of the secret ☝️ will be injected in the environment of the container that provisions resources, so it needs to conform to the format expected by the controller.
Currently the controller configures the credentials for the backend using two environment variables:
* `TF_BACKEND_AWS_ACCESS_KEY_ID`
* `TF_BACKEND_AWS_SECRET_ACCESS_KEY`


#### Create the Terraform Backend

[Install directory](../install) contains terraform-backend, a terraform infra module that creates an S3+DynamoDB+KMS backend.
Either adopt that module in some other infrastructure and build out the backend, or use the module directly to build the backend with the following commands.
```bash
cd install/terraform-backend
terraform init
terraform apply -auto-approve
# terraform will ask for a name for the S3 bucket, choose one
```
That module also offers the possibility to configure AWS IAM users and roles with access to the backend, but that requires adapting the code and adding the respective ARNs.

#### Install the controller on a cluster

...


### Using the controller to provision a terraform module for an S3 bucket

```bash
kubectl create secret generic infra-resource-test-1 \
    --from-literal=name='test-s3-bucket' \
    --from-literal=image='docker.pkg.github.com/miguelaferreira/k8s-terraform-module-controller/terraform-modules' \
    --from-literal=tag=dev \
    --from-literal=path='/modules/aws/s3-bucket' \
    --from-literal=variables='
    {
        "bucket_name": "k8s-terraform-controller-s3-bucket", 
        "aws_access_key_id": "xxxxxxxxxxxxxxxxxxx",
        "aws_secret_access_key": "yyyyyyyyyyyyyyyyyyyy"
    }'
```

#### Modules container

In this repository we provide a modules container that is based on open source modules for well known infrastructure building blocks.
However, it is possible to use different modules containers.

#### Build the included modules container

```bash
cd docker/terraform-modules
docker build . -t docker.pkg.github.com/miguelaferreira/k8s-terraform-controller/terraform-modules:dev
# docker push docker.pkg.github.com/miguelaferreira/k8s-terraform-controller/terraform-modules:dev
```

#### Build a custom modules container

This section describes the requirements for building a custom modules container.
