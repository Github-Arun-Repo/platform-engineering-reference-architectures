variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "eu-west-1"
}

variable "name_prefix" {
  description = "Prefix used for naming resources"
  type        = string
  default     = "ireland-vpc"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
}

variable "availability_zones" {
  description = "Exactly 3 availability zones in eu-west-1"
  type        = list(string)

  validation {
    condition     = length(var.availability_zones) == 3
    error_message = "Provide exactly 3 availability zones."
  }
}

variable "public_subnet_cidrs" {
  description = "One public subnet CIDR per AZ"
  type        = list(string)

  validation {
    condition     = length(var.public_subnet_cidrs) == 3
    error_message = "Provide exactly 3 public subnet CIDRs."
  }
}

variable "private_app_subnet_cidrs" {
  description = "One private app subnet CIDR per AZ"
  type        = list(string)

  validation {
    condition     = length(var.private_app_subnet_cidrs) == 3
    error_message = "Provide exactly 3 private app subnet CIDRs."
  }
}

variable "private_data_subnet_cidrs" {
  description = "One private data subnet CIDR per AZ"
  type        = list(string)

  validation {
    condition     = length(var.private_data_subnet_cidrs) == 3
    error_message = "Provide exactly 3 private data subnet CIDRs."
  }
}

variable "common_tags" {
  description = "Tags applied to all resources"
  type        = map(string)
  default     = {}
}
