variable "bucket_name" {
  description = "Globally unique S3 bucket name"
  type        = string
}

variable "tags" {
  description = "Tags applied to the bucket"
  type        = map(string)
  default     = {}
}

variable "versioning_status" {
  description = "Versioning status for bucket"
  type        = string
  default     = "Enabled"

  validation {
    condition     = contains(["Enabled", "Suspended"], var.versioning_status)
    error_message = "versioning_status must be Enabled or Suspended."
  }
}

variable "kms_key_arn" {
  description = "Optional KMS key ARN for SSE-KMS. If null, SSE-S3 (AES256) is used."
  type        = string
  default     = null
}

variable "enable_object_lock" {
  description = "Enable object lock on bucket"
  type        = bool
  default     = false
}

variable "object_lock_mode" {
  description = "Object lock retention mode"
  type        = string
  default     = "GOVERNANCE"

  validation {
    condition     = contains(["GOVERNANCE", "COMPLIANCE"], var.object_lock_mode)
    error_message = "object_lock_mode must be GOVERNANCE or COMPLIANCE."
  }
}

variable "object_lock_days" {
  description = "Default retention days for object lock"
  type        = number
  default     = 30
}

variable "enable_lifecycle" {
  description = "Enable lifecycle policy"
  type        = bool
  default     = false
}

variable "abort_incomplete_multipart_days" {
  description = "Abort incomplete multipart uploads after these days"
  type        = number
  default     = 7
}

variable "expiration_days" {
  description = "Optional expiration for current objects"
  type        = number
  default     = null
}

variable "noncurrent_version_expiration_days" {
  description = "Optional expiration for noncurrent object versions"
  type        = number
  default     = null
}

variable "force_destroy" {
  description = "Allow bucket destroy even when non-empty"
  type        = bool
  default     = false
}
