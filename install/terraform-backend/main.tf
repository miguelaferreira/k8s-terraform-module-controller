# terraform apply -auto-approve
provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  default = "eu-west-1"
}

variable "bucket_name" {}

module "state_backend" {
  source = "git::https://gitlab.com/open-source-devex/terraform-modules/aws/terraform-state-backend?ref=v1.0.9"

  aws_region = var.aws_region

  bucket_name = var.bucket_name
  bucket_acl  = "private"

  create_dynamo_lock_table = true

  common_tags = {
    project = "k8s-terraform-controller"
  }

  force_destroy = true

  block_public_acls       = true
  ignore_public_acls      = true
  block_public_policy     = true
  restrict_public_buckets = true

  prevent_unencrypted_uploads = true

  kms_deletion_window = 7

  users = [
    # aws_iam_user.some_user.arn
  ]
  roles = [
    # aws_iam_role.some_role.arn
  ]
}

output "s3_bucket" {
  value = module.state_backend.s3_bucket_id
}

output "dynamodb_table" {
  value = module.state_backend.s3_bucket_lock_table_name
}
