# k8s-terraform-controller

K8s controller to execute terraform modules.
When installed on a cluster, this controller will watch for user input via k8s secrets on the controller namespace with a pre-configured label.
Once one such secret is detected, the controller starts a reconciliation flow that will create a few objects in the cluster that are required to either apply or destroy a terraform module.
The required resources are:
* A config map with the information of the module to execute, which is basically the source container of the module, the path in the container where the module can be found.
* A secret with the input variables for the module which include any credentials required for providers used in the module.
* A pod to execute terraform and provision or destroy the module

If the execution of the pod succeeds then a new secret is created with the outputs produced by terraform.
That secret can be bound to other kubernetes objects to make the outputs available to them.

## How to use

With an installed and configured controller, in order to provision a terraform module a user creates a secret.
```bash
kubectl -n terraform-controller create secret generic terraform-module-example-1 \
    --from-literal=name='my first terraform module via kubernetes' \
    --from-literal=image='docker.pkg.github.com/miguelaferreira/k8s-terraform-module-controller/terraform-modules' \
    --from-literal=tag=dev \
    --from-literal=path='/modules/aws/s3-bucket' \
    --from-literal=variables='
    {
        "bucket_name": "k8s-terraform-controller-s3-bucket", 
        "aws_access_key_id": "AAAAAAAAAAAAAAA",
        "aws_secret_access_key": "BBBBBBBBBB"
    }'
```
With ☝️ this secret we are asking the controller to provision an S3 bucket using the modules container image shipped together with the controller.
We are giving the bucket a name and passing the required AWS keys. The fact that we need to pass the keys derives from the way the module is specified on the container image.
The module defines the provider it uses like this:
```hcl-terraform
provider "aws" {
  region     = var.aws_region
  access_key = var.aws_access_key_id
  secret_key = var.aws_secret_access_key
}
```
Which means it expects the key pair to be passed along as terraform variables `var.aws_access_key_id` and `var.aws_secret_access_key`, therefore we need to supply those variables in the request to provision the bucket.

Through variables is currently the only way to pass along secrets/credentials to the execution.
There are plans to support placing the secrets/variables in the environment and have them picked up by the execution.
There are also plans to support controller wide credentials for providers, which would mean that the users would not need to provide credentials via variables and instead link their provisioning request to a set of credentials they or the controller provide.
Finally, there are also plans to support EKS's IRSA approach, where a IAM role (with certain level or permissions) is assigned to and assumed by the service account that is used to execute the pod that provisions or destroys modules.

After creating the provisioning request with secret `terraform-module-example-1` you will notice that the controller has done nothing to that respect.
That is because the controller expects a specific tag on the secret before it will consider it for provisioning.
Therefore, we will need to tag the secret with a label indicating that it is in-fact an input for the controller.
```kubectl
kubectl -n terraform-controller patch secret terraform-module-example-1 -p '{"metadata": {"labels": {"tf.module/input": ""} }}'
```
Once the label is added to the secret, the controller picks it up and starts provisioning it.
A new pod is created to execute terraform, and at the end (if all goes well), there will be a new secret on the namespace with the outputs.
The outputs secret has the same name as the input secret, but it is prefixed with "outputs-".
For the input secret "terraform-module-example-1" there will be a "outputs-terraform-module-example-1" secret.
```yaml
apiVersion: v1
data:
  s3_bucket_arn: YXJuOmF....idWNrZXQ=
  s3_bucket_bucket_domain_name: azhzLXRlcn....My5hbWF6b25hd3MuY29t
  s3_bucket_bucket_regional_domain_name: azhzLX....LTEuYW1hem9uYXdzLmNvbQ==
  s3_bucket_hosted_zone_id: WjFC....EVaUEU=
  s3_bucket_id: azhzLXRlc....zLWJ1Y2tldA==
  s3_bucket_region: ZX...0x
kind: Secret
metadata:
  name: outputs-terraform-module-example-1
  namespace: terraform-controller
type: Opaque
```

## How to install

### Requirements

#### Terraform backend

The controller needs a terraform backend where to store and keep track of the states for the provisioned modules.
Terraform supports several types of backends, however this controller only supports a S3 Bucket backend (S3 + DynamoDB + KMS).
Before installing the controller, create an S3 terraform backend. The controller does not require a dedicated backend, so if you already have a S3 backend in place you can re-use it.

To ease the creation of the backend, this repo includes a terraform module that does just that under [install/terraform-backend](install/terraform-backend/).
The module requires AWS credentials on the environment and a name for the S3 bucket.
```bash
# export AWS credentials on the environment

cd install/terraform-backend
terraform init
terraform apply -auto-approve

# type in the name of the bucket for var.bucket_name and hit enter
```

After provisioning the backend, terraform will print the name of the s3 bucket and dynamo db table (which, per configuration, are the same).
Make a note of those values, and also create a k8s secret with credentials for the backend in the same namespace has the controller will be installed in.
```bash
kubectl create secret generic terraform-backend-credentials \
    --from-literal=TF_BACKEND_AWS_ACCESS_KEY_ID='zzzzzzzzzzzzzzzzzzzz' \
    --from-literal=TF_BACKEND_AWS_SECRET_ACCESS_KEY='xxxxxxxxxxxxxxxxxxxxxxxxx'
```

#### Dedicated namespace

While not strictly necessary it is recommended to create a dedicated namespace for te controller.
```bash
kubectl create ns terraform-controller
```

#### Docker registry credentials

The controller will need to be able to pull the container images that contain terraform modules the users want to provision.
This repository includes such a container with modules under [docker/terraform-modules](./docker/terraform-modules).
That image is hosted on the packages for this github repository. While the image is open-source docker login is required to pull that image.
Therefore a secret needs to be created and configured in the controller installation in order to enable the controller to pull the image.
For more details on how to enable access ti private image repositories see [kubernetes documentation on the matter](https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/).
```bash
# Example: create k8s secret from username/password for accessing GitHub's docker registry
kubectl create secret docker-registry regcred-github -n terraform-controller  --docker-server=docker.pkg.github.com --docker-username=xxx --docker-password=yyy
```

### Installation

To ease the installation of the controller this repository also contains a helm chart that installs the controller and it's configuration on a given namespace.
It is recommended to create a namespace that is dedicated to the controller, it's inputs and outputs.
Before asking helm to install (or generate the installation files for) the controller, please review the [default helm values](./install/helm/k8s-terraform-module-controller/values.yaml) and create a new file with your own configuration (say `my-values.yaml`).
```yaml
# Example: my-values.yaml
controller:
  # Configure the terraform backend the controller will use to store the states of the provisioned modules
  terraformBackend:
    s3:
      region: "eu-west-1"
      bucket: "k8s-terraform-controller"
      dynamodbTable: "k8s-terraform-controller"
      # Whether to use KMS encryption
      encrypt: true
      # Credentials secret name
      sceretName: "terraform-backend-credentials"

image:
  repository: docker.pkg.github.com/miguelaferreira/k8s-terraform-module-controller/controller-java11
  pullPolicy: IfNotPresent

imagePullSecrets:
  - regcred-github
```

With the custom values file we are one command way from installing the controller.
```bash
cd install/helm
helm install release k8s-terraform-module-controller -n terraform-controller -f my-values.yaml 
```
