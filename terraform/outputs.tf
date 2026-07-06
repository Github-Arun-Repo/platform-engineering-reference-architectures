output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc_network.vpc_id
}

output "public_subnet_ids" {
  description = "Public subnet IDs by AZ"
  value       = module.vpc_network.public_subnet_ids
}

output "private_app_subnet_ids" {
  description = "Private app subnet IDs by AZ"
  value       = module.vpc_network.private_app_subnet_ids
}

output "private_data_subnet_ids" {
  description = "Private data subnet IDs by AZ"
  value       = module.vpc_network.private_data_subnet_ids
}

output "secure_bucket_logs_arn" {
  description = "ARN of the secure logs bucket"
  value       = module.secure_bucket_logs.bucket_arn
}

output "secure_bucket_artifacts_arn" {
  description = "ARN of the secure artifacts bucket"
  value       = module.secure_bucket_artifacts.bucket_arn
}
