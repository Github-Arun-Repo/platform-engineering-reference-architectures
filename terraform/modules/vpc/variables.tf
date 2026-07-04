variable "name_prefix" {
  description = "Prefix used for naming resources"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
}

variable "availability_zones" {
  description = "Three availability zones"
  type        = list(string)
}

variable "public_subnet_cidrs" {
  description = "Public subnet CIDRs mapped 1:1 with AZs"
  type        = list(string)
}

variable "private_app_subnet_cidrs" {
  description = "Private app subnet CIDRs mapped 1:1 with AZs"
  type        = list(string)
}

variable "private_data_subnet_cidrs" {
  description = "Private data subnet CIDRs mapped 1:1 with AZs"
  type        = list(string)
}

variable "tags" {
  description = "Tags applied to resources"
  type        = map(string)
  default     = {}
}
