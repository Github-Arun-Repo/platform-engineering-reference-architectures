# Modular Terraform: AWS VPC (eu-west-1)

This Terraform setup creates only VPC and required networking:
- 1 VPC in eu-west-1
- 3 public subnets (1 per AZ)
- 6 private subnets (2 per AZ: private-app + private-data)
- Internet Gateway
- Route tables and associations

## Structure

- root module: orchestrates inputs/outputs
- modules/vpc: reusable networking module

## Remote State and Locking

This project is configured for:
- Remote state in S3
- State locking in DynamoDB

Backend block is defined in versions.tf as partial config:
- backend "s3" {}

Provide real backend settings via backend.hcl:
1. Copy backend.hcl.example to backend.hcl
2. Update bucket and dynamodb_table names

Example backend settings are in backend.hcl.example.

## Sample Input Values

Sample values are provided in terraform.tfvars.example.

1. Copy terraform.tfvars.example to terraform.tfvars
2. Adjust CIDRs, tags, and naming if needed

## Usage

From this directory:

1. Initialize:
   terraform init -backend-config=backend.hcl
2. Plan:
   terraform plan
3. Apply:
   terraform apply

## Notes

- Keep subnet CIDRs non-overlapping.
- The 3 AZs in sample values are eu-west-1a, eu-west-1b, eu-west-1c.
- No NAT gateways are created, so private subnets remain private-only.
