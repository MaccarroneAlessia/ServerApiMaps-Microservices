variable "aws_region" {
  description = "AWS Region per il deployment"
  type        = string
  default     = "eu-west-1" # Puoi cambiarlo con la tua region preferita (es. eu-south-1 per Milano)
}

variable "db_password" {
  description = "La password per l'amministratore del database RDS (verrà salvata cifrata)"
  type        = string
  sensitive   = true
}

variable "google_api_key" {
  description = "La chiave API per Google Maps (verrà salvata cifrata)"
  type        = string
  sensitive   = true
}
