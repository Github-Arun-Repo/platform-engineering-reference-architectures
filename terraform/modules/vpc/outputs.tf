output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.this.id
}

output "public_subnet_ids" {
  description = "Public subnet IDs by AZ"
  value = {
    for az, subnet in aws_subnet.public : az => subnet.id
  }
}

output "private_app_subnet_ids" {
  description = "Private app subnet IDs by AZ"
  value = {
    for az, subnet in aws_subnet.private_app : az => subnet.id
  }
}

output "private_data_subnet_ids" {
  description = "Private data subnet IDs by AZ"
  value = {
    for az, subnet in aws_subnet.private_data : az => subnet.id
  }
}
