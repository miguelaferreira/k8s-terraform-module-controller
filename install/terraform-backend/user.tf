variable "create_backend_user" {
  type    = string
  default = false
}

variable "backend_user_name" {
  type    = string
  default = "k8s-tf-module-controller-backend"
}

variable "pgp_key" {
  type    = string
  default = ""
}

module "backend_user" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-user"
  version = "2.9.0"

  create_user = var.create_backend_user
  name        = var.backend_user_name
  pgp_key     = var.pgp_key

  force_destroy                 = true
  create_iam_user_login_profile = false
}

resource "aws_iam_user_policy_attachment" "backend_user" {
  count = var.create_backend_user ? 1 : 0

  policy_arn = join("", aws_iam_policy.backend_user.*.arn)
  user       = module.backend_user.this_iam_user_name
}

resource "aws_iam_policy" "backend_user" {
  count = var.create_backend_user ? 1 : 0

  name   = var.backend_user_name
  policy = join("", data.aws_iam_policy_document.backend_user.*.json)
}

data "aws_iam_policy_document" "backend_user" {
  count = var.create_backend_user ? 1 : 0

  statement {
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = [module.state_backend.s3_bucket_arn]
  }

  statement {
    effect    = "Allow"
    actions   = ["s3:GetObject", "s3:PutObject"]
    resources = ["${module.state_backend.s3_bucket_arn}/*"]
  }
}

output "backend_user_name" {
  value = module.backend_user.this_iam_user_name
}

output "backend_user_aws_access_key_id" {
  value = module.backend_user.this_iam_access_key_id
}

output "backend_user_aws_secret_access_key" {
  value = module.backend_user.this_iam_access_key_secret
}

output "backend_user_aws_secret_access_key_encrypted" {
  value = module.backend_user.this_iam_access_key_encrypted_secret
}
