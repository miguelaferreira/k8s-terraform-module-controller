terraform {
  backend "s3" {
    region         = "VAR_REMOTE_STATE_REGION"
    bucket         = "VAR_REMOTE_STATE_BUCKET"
    dynamodb_table = "VAR_REMOTE_STATE_DYNAMODB_TABLE"
    encrypt        = "VAR_REMOTE_STATE_ENCRYPT"

    skip_metadata_api_check = true
  }
}
