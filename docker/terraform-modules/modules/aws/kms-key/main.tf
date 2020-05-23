module "kms_key" {
  source = "git::https://gitlab.com/open-source-devex/terraform-modules/aws/kms-key.git?ref=v1.0.4"

  create_key = var.create_key

  key_name        = var.key_name
  key_description = var.key_description
  tags            = var.tags

  deletion_window_in_days = var.deletion_window_in_days
  enable_key_rotation     = var.enable_key_rotation

  key_policy = var.key_policy
}
