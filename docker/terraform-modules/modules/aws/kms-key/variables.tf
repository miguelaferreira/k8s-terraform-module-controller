variable "create_key" {
  description = "Whether to create the key and other resources in this module"
  type        = bool
  default     = true
}

variable "key_name" {
  description = "A name for the key, if not specified a name will be generated"
  type        = string
  default     = null
}

variable "key_description" {
  description = "A description for the key, if not specified a description will be generated"
  type        = string
  default     = null
}

variable "tags" {
  description = "Tags to be added to the resources created in the module"
  type        = map(string)
  default     = {}
}

variable "deletion_window_in_days" {
  description = "Duration in days after which the key is deleted after destruction of the resource, must be between 7 and 30 days."
  type        = number
  default     = 30
}

variable "enable_key_rotation" {
  description = "Specifies whether key rotation is enabled"
  type        = bool
  default     = true
}

variable "key_policy" {
  description = "Policy statements to be added to the default key management policy"

  type = list(object({
    principals = list(object({
      type = string, identifiers = list(string)
    }))
    effect     = string
    actions    = list(string)
    resources  = list(string)
    condition  = list(object({
      test     = string
      variable = string
      values   = list(string)
    }))
  }))

  default = [
    #    {
    #      principals = [{
    #        type = "AWS", identifiers = ["foo"]
    #      }]
    #      effect     = "Allow"
    #      actions    = ["foo", "bar"]
    #      resources  = ["*"]
    #      condition  = [{
    #        test     = "StringLike"
    #        variable = "kms:EncryptionContext:aws:cloudtrail:arn"
    #        values   = ["arn:aws:cloudtrail:*:ACCOUNT:trail/*"]
    #      }]
    #    }
  ]
}
