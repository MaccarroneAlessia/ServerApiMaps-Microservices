variable "aws_region" {
  description = "AWS Region for deployment"
  type        = string
  default     = "eu-west-1" # You can change this to your preferred region (e.g. eu-south-1 for Milan)
}

variable "db_password" {
  description = "Password for the RDS database administrator (will be saved encrypted)"
  type        = string
  sensitive   = true
}

variable "google_api_key" {
  description = "Google Maps API key (will be saved encrypted)"
  type        = string
  sensitive   = true
}
