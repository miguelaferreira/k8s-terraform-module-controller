variable "bucket_name" {
  type = string
}

variable "aws_access_key_id" {
  type = string
}

variable "aws_secret_access_key" {
  type = string
}

variable "aws_region" {
  default = "eu-west-1"
}

variable "bucket_acl" {
  default = "private"
}

variable "policy_json" {
  default = ""
}

variable "tags" {
  type    = map(string)
  default = {}
}

variable "force_destroy" {
  default = true
}

variable "attach_policy" {
  default = false
}

variable "cors_rule" {
  default = {
    #    allowed_methods = ["PUT", "POST"]
    #    allowed_origins = ["https://modules.tf", "https://terraform-aws-modules.modules.tf"]
    #    allowed_headers = ["*"]
    #    expose_headers  = ["ETag"]
    #    max_age_seconds = 3000
  }
}

variable "lifecycle_rule" {
  default = [
    {
      id      = "log"
      enabled = true
      prefix  = "log/"

      tags = {
        rule      = "log"
        autoclean = "true"
      }

      transition = [
        {
          days          = 30
          storage_class = "ONEZONE_IA"
        }, {
          days          = 60
          storage_class = "GLACIER"
        }
      ]

      expiration = {
        days = 90
      }

      noncurrent_version_expiration = {
        days = 30
      }
    },
    {
      id                                     = "log1"
      enabled                                = true
      prefix                                 = "log1/"
      abort_incomplete_multipart_upload_days = 7

      noncurrent_version_transition = [
        {
          days          = 30
          storage_class = "STANDARD_IA"
        },
        {
          days          = 60
          storage_class = "ONEZONE_IA"
        },
        {
          days          = 90
          storage_class = "GLACIER"
        },
      ]

      noncurrent_version_expiration = {
        days = 300
      }
    },
  ]
}

variable "server_side_encryption_configuration" {
  default = {
    #    rule = {
    #      apply_server_side_encryption_by_default = {
    #        kms_master_key_id = aws_kms_key.objects.arn
    #        sse_algorithm     = "aws:kms"
    #      }
    #    }
  }
}

variable "block_public_acls" {
  default = true
}

variable "block_public_policy" {
  default = true
}

variable "ignore_public_acls" {
  default = true
}

variable "restrict_public_buckets" {
  default = true
}
