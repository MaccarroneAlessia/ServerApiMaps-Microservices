terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # -------------------------------------------------------------------------
  # BACKEND S3 REMOTO (Production-Ready)
  # Decommentare questo blocco per salvare lo State in cloud condiviso.
  # Assicurarsi di aver prima creato il bucket "maps-app-terraform-state-bucket"
  # e la tabella DynamoDB "terraform-state-locks" manualmente su AWS.
  # -------------------------------------------------------------------------
  # backend "s3" {
  #   bucket         = "maps-app-terraform-state-bucket"
  #   key            = "global/s3/terraform.tfstate"
  #   region         = "eu-west-1"
  #   dynamodb_table = "terraform-state-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region
}
