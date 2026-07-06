module "vpc_network" {
  source = "./modules/vpc"

  name_prefix               = var.name_prefix
  vpc_cidr                  = var.vpc_cidr
  availability_zones        = var.availability_zones
  public_subnet_cidrs       = var.public_subnet_cidrs
  private_app_subnet_cidrs  = var.private_app_subnet_cidrs
  private_data_subnet_cidrs = var.private_data_subnet_cidrs
  tags                      = var.common_tags
}

module "secure_bucket_logs" {
  source = "./modules/s3_bucket"

  bucket_name                        = var.secure_bucket_logs_name
  tags                               = var.common_tags
  versioning_status                  = "Enabled"
  enable_object_lock                 = true
  object_lock_mode                   = "COMPLIANCE"
  object_lock_days                   = 30
  enable_lifecycle                   = true
  abort_incomplete_multipart_days    = 7
  noncurrent_version_expiration_days = 120
}

module "secure_bucket_artifacts" {
  source = "./modules/s3_bucket"

  bucket_name                     = var.secure_bucket_artifacts_name
  tags                            = var.common_tags
  versioning_status               = "Enabled"
  enable_object_lock              = false
  enable_lifecycle                = true
  abort_incomplete_multipart_days = 3
  expiration_days                 = 365
}
