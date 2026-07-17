# The Database Password
resource "aws_ssm_parameter" "db_password" {
  name        = "/maps-app/prod/DB_PASSWORD"
  description = "Password for MySQL RDS"
  type        = "SecureString" # Encrypted with KMS by default
  value       = var.db_password
}

# The Google Maps API Key
resource "aws_ssm_parameter" "google_api_key" {
  name        = "/maps-app/prod/GOOGLE_MAPS_API_KEY"
  description = "Google Maps API Key"
  type        = "SecureString"
  value       = var.google_api_key
}
